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

import play.api.libs.json.{JsBoolean, Json}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.mongoFeatureToggles.internal.actions.InternalAuthAction
import uk.gov.hmrc.mongoFeatureToggles.model.{FeatureFlag, FeatureFlagName}
import uk.gov.hmrc.mongoFeatureToggles.services.FeatureFlagService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FeatureFlagsAdminController @Inject() (
  auth: InternalAuthAction,
  featureFlagService: FeatureFlagService,
  mcc: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) {

  def get: Action[AnyContent] = auth().async {
    featureFlagService.getAll
      .map(flags => Ok(Json.toJson(flags)))
  }

  def put(flagName: FeatureFlagName): Action[AnyContent] = auth().async { request =>
    request.body.asJson match {
      case Some(JsBoolean(enabled)) =>
        featureFlagService
          .set(flagName, enabled)
          .map(_ => NoContent)
      case _                        =>
        Future.successful(BadRequest("Request body was invalid"))
    }
  }

  def putAll: Action[AnyContent] = auth().async { request =>
    request.body.asJson.fold(Future.successful(BadRequest("Request body was invalid"))) { json =>
      val flags = json
        .as[Seq[FeatureFlag]]
        .map(flag => (flag.name -> flag.isEnabled))
        .toMap
      featureFlagService.setAll(flags).map(_ => NoContent)
    }
  }
}
