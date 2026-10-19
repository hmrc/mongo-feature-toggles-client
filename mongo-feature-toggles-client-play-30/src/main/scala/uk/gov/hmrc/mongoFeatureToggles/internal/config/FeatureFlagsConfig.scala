/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.mongoFeatureToggles.internal.config

import com.google.inject.{Inject, Singleton}
import play.api.{Configuration, Logging}
import uk.gov.hmrc.mongoFeatureToggles.internal.model.{FeatureFlagDefinition, LocalConfiguration, RemoteConfiguration}

import scala.jdk.CollectionConverters.*

@Singleton
class FeatureFlagsConfig @Inject() (configuration: Configuration) extends Logging {

  private val RootPath = "mongo-feature-toggles-client.toggles"

  def get(featureFlagName: String): Option[FeatureFlagDefinition] =
    getAllToggles.find(_.name == featureFlagName)

  def getAllToggles: Set[FeatureFlagDefinition] =
    if (!configuration.underlying.hasPath(RootPath)) {
      Set.empty
    } else {
      val config = configuration.underlying.getConfig(RootPath)

      config
        .root()
        .entrySet()
        .asScala
        .map { entry =>
          val toggleConfiguration = Configuration(config.getConfig(entry.getKey))

          val remoteConfiguration = if (toggleConfiguration.underlying.hasPath("remote-configuration")) {
            Some(RemoteConfiguration(toggleConfiguration.get[Boolean]("remote-configuration.defaultState")))
          } else {
            None
          }

          val localConfiguration = if (toggleConfiguration.underlying.hasPath("local-configuration")) {
            Some(LocalConfiguration(toggleConfiguration.get[Boolean]("local-configuration.isEnabled")))
          } else {
            None
          }

          val description           = toggleConfiguration.getOptional[String]("description")
          val isRemoteConfiguration = toggleConfiguration.get[String]("toggleSourceValue").toLowerCase match {
            case "remote-configuration" => true
            case "local-configuration"  => false
            case other                  =>
              throw new RuntimeException(
                s"Invalid value `$other` for config `isEnabled`. It should be true, false or auto"
              )
          }

          (isRemoteConfiguration, remoteConfiguration, localConfiguration) match {
            case (true, Some(_), _)  =>
              FeatureFlagDefinition(
                name = entry.getKey,
                description = description,
                remoteConfiguration = remoteConfiguration,
                localConfiguration = None
              )
            case (false, _, Some(_)) =>
              FeatureFlagDefinition(
                name = entry.getKey,
                description = description,
                remoteConfiguration = None,
                localConfiguration = localConfiguration
              )

            case (false, _, None) =>
              throw new RuntimeException(s"Toggle set to local configuration but local-configuration is missing")

            case (true, None, _) =>
              throw new RuntimeException(s"Toggle set to remote configuration but remote-configuration is missing")
          }
        }
        .toSet
    }
}
