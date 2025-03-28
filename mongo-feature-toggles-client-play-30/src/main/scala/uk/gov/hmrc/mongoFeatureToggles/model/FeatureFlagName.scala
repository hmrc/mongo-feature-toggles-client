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

package uk.gov.hmrc.mongoFeatureToggles.model

import play.api.libs.json.{Format, JsError, JsResult, JsString, JsSuccess, JsValue, Json, Reads, Writes}
import play.api.mvc.PathBindable
import uk.gov.hmrc.mongoFeatureToggles.model.Environment.Environment

trait FeatureFlagName {
  val description: Option[String]          = None
  val name: String
  val defaultState: Boolean                = false
  val lockedEnvironments: Seq[Environment] = Seq.empty
  override def toString: String            = name
}

object FeatureFlagName {
  implicit final val writes: Writes[FeatureFlagName] = (o: FeatureFlagName) =>
    Json.obj(
      "name"               -> o.name,
      "description"        -> o.description,
      "defaultState"       -> o.defaultState,
      "lockedEnvironments" -> o.lockedEnvironments
    )

  implicit final val reads: Reads[FeatureFlagName]    = new Reads[FeatureFlagName] {
    override def reads(json: JsValue): JsResult[FeatureFlagName] = {
      val name = (json \ "name").asOpt[String].getOrElse(json.as[String])
      FeatureFlagNamesLibrary.getAllFlags
        .find(flag => flag.name == name)
        .map(JsSuccess(_))
        .getOrElse(JsError(s"Unknown FeatureFlagName `${json.toString}`"))
    }
  }
  implicit final val formats: Format[FeatureFlagName] =
    Format(reads, writes)

  implicit final def pathBindable: PathBindable[FeatureFlagName] = new PathBindable[FeatureFlagName] {

    override def bind(key: String, value: String): Either[String, FeatureFlagName] =
      JsString(value).validate[FeatureFlagName] match {
        case JsSuccess(name, _) =>
          Right(name)
        case _                  =>
          Left(s"The feature flag `$value` does not exist")
      }

    override def unbind(key: String, value: FeatureFlagName): String =
      value.name
  }
}
