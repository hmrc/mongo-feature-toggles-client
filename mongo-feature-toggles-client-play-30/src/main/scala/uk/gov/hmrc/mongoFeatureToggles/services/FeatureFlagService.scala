/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.mongoFeatureToggles.services

import cats.data.EitherT
import play.api.Logging
import play.api.cache.AsyncCacheApi
import uk.gov.hmrc.mongoFeatureToggles.internal.config.{AppConfig, FeatureFlagsConfig}
import uk.gov.hmrc.mongoFeatureToggles.internal.model.{FeatureFlagAdmin, FeatureFlagDefinition, FeatureFlagSerialised}
import uk.gov.hmrc.mongoFeatureToggles.internal.repository.FeatureFlagRepository
import uk.gov.hmrc.mongoFeatureToggles.model.{FeatureFlag, FeatureFlagName}

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.{Duration, FiniteDuration, SECONDS as Seconds}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FeatureFlagService @Inject() (
  appConfig: AppConfig,
  toggleConfig: FeatureFlagsConfig,
  featureFlagRepository: FeatureFlagRepository,
  cache: AsyncCacheApi
)(implicit
  ec: ExecutionContext
) extends Logging {
  val cacheValidFor: FiniteDuration =
    Duration(appConfig.cacheTtlInSeconds, Seconds)

  private[mongoFeatureToggles] def set(flagName: String, enabled: Boolean): Future[Boolean] =
    for {
      _      <- cache.remove(flagName)
      result <- featureFlagRepository.setFeatureFlag(FeatureFlagSerialised(flagName, enabled))
      // blocking thread to let time to other containers to update their cache
      _      <- Future.successful(Thread.sleep(appConfig.cacheTtlInSeconds * 1000))
    } yield result

  private def innerGet(flagName: FeatureFlagName): Future[FeatureFlag] =
    toggleConfig.get(flagName.name) match {
      case None                   =>
        throw new RuntimeException(s"no definition for toggle `$flagName` in application.conf or app-config-*")
      case Some(toggleDefinition) =>
        if (toggleDefinition.isRemote) {
          logger.info(s"Found toggle $flagName in config with source set to mongo")
          cache.getOrElseUpdate(flagName.toString, cacheValidFor) {
            featureFlagRepository
              .getFeatureFlag(toggleDefinition)
              .map(
                _.fold(
                  FeatureFlag(flagName, toggleDefinition.remoteConfiguration.fold(false)(_.defaultStatus))
                )(featureFlag => FeatureFlag(flagName, featureFlag.isEnabled))
              )
          }
        } else {
          logger.info(s"Found toggle $flagName in config with source set to config")
          Future.successful(FeatureFlag(flagName, toggleDefinition.localConfiguration.fold(false)(_.isEnabled)))
        }
    }

  def get(flagName: FeatureFlagName): Future[FeatureFlag] = innerGet(flagName)

  def getAsEitherT[A](flagName: FeatureFlagName): EitherT[Future, A, FeatureFlag] =
    EitherT[Future, A, FeatureFlag](innerGet(flagName).map(Right(_)))

  private[mongoFeatureToggles] def getAll: Future[Set[FeatureFlagAdmin]] = {
    val featureFlagsFromConfig: Set[FeatureFlagDefinition] = toggleConfig.getAllToggles

    featureFlagRepository.getAllFeatureFlags.map { featureFlagsInMongo =>
      val mongoByName  = featureFlagsInMongo.map(flag => flag.name -> flag).toMap
      val configByName = featureFlagsFromConfig.map(flag => flag.name -> flag).toMap

      val intersect              =
        featureFlagsFromConfig.filter(flag => mongoByName.contains(flag.name))
      val featureFlagsNotInMongo =
        featureFlagsFromConfig.filterNot(flag => mongoByName.contains(flag.name))
      val deletedFeatureFlags    =
        featureFlagsInMongo.filterNot(flag => configByName.contains(flag.name))

      deletedFeatureFlags.foreach { flag =>
        featureFlagRepository.deleteFeatureFlag(flag).map {
          case true  => logger.warn(s"Flag `${flag.name}` has been deleted from Mongo")
          case false => logger.error(s"Flag `${flag.name}` could not be deleted from Mongo")
        }
      }

      intersect.map { flag =>
        if (flag.isRemote) {
          FeatureFlagAdmin(flag, mongoByName(flag.name).isEnabled)
        } else {
          FeatureFlagAdmin(flag, flag.localConfiguration.fold(false)(_.isEnabled))
        }
      } ++ featureFlagsNotInMongo.map { flag =>
        FeatureFlagAdmin(flag, flag.remoteConfiguration.fold(false)(_.defaultStatus))
      }
    }
  }

  private[mongoFeatureToggles] def setAll(flags: Set[FeatureFlagSerialised]): Future[Unit] =
    Future
      .sequence(flags.map(flag => cache.remove(flag.name)))
      .flatMap { _ =>
        featureFlagRepository.setFeatureFlags(flags)
      }
      .map { _ =>
        // blocking thread to let time to other containers to update their cache
        Thread.sleep(appConfig.cacheTtlInSeconds * 1000)
        ()
      }
}
