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

package uk.gov.hmrc.mongoFeatureToggles.controllers

import play.api.libs.json.{JsBoolean, Json, Writes}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.mongoFeatureToggles.internal.actions.InternalAuthAction
import uk.gov.hmrc.mongoFeatureToggles.internal.config.FeatureFlagsConfig
import uk.gov.hmrc.mongoFeatureToggles.internal.model.{FeatureFlagAdmin, FeatureFlagSerialised}
import uk.gov.hmrc.mongoFeatureToggles.services.FeatureFlagService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FeatureFlagsAdminController @Inject() (
  auth: InternalAuthAction,
  featureFlagService: FeatureFlagService,
  featureFlagsConfig: FeatureFlagsConfig,
  mcc: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) {

  def get: Action[AnyContent] = Action.async {
    featureFlagService.getAll
      .map { flags =>
        Ok(Json.toJson(flags)(using Writes.set(using FeatureFlagAdmin.writesToThirdParties)))
      }
  }

  def put(flagName: String): Action[AnyContent] = auth().async { request =>
    featureFlagsConfig.get(flagName) match {
      case None                             => Future.successful(BadRequest(s"the feature flag `$flagName` does not exist."))
      case Some(toggle) if !toggle.isRemote =>
        Future.successful(
          BadRequest(
            s"the feature flag `$flagName` cannot be set remotely. Edit appication.conf or override the value in app-config-*."
          )
        )
      case _                                =>
        request.body.asJson match {
          case Some(JsBoolean(enabled)) =>
            featureFlagService
              .set(flagName, enabled)
              .map(_ => NoContent)
          case _                        =>
            Future.successful(BadRequest("Request body was invalid"))
        }
    }
  }

  def putAll: Action[AnyContent] = auth().async { request =>
    request.body.asJson.fold(Future.successful(BadRequest("Request body was invalid"))) { json =>
      val featureFlagsRequest = json.as[Seq[FeatureFlagSerialised]]

      featureFlagsRequest
        .partitionMap { featureFlag =>
          featureFlagsConfig.get(featureFlag.name).toRight(featureFlag.name)
        } match {
        case (errors, _) if errors.nonEmpty                  =>
          Future.successful(BadRequest(s"The feature flag(s) ${errors.mkString(", ")} do not exist."))
        case (_, flags) if flags.exists(_.isRemote == false) =>
          val errors = flags.filter(_.isRemote == false).map(_.name)
          Future.successful(
            BadRequest(
              s"the feature flag(s) `${errors.mkString(", ")}` cannot be set remotely. Edit appication.conf or override the value in app-config-*."
            )
          )
        case _                                               =>
          featureFlagService.setAll(featureFlagsRequest.toSet).map(_ => NoContent)
      }
    }
  }
}
