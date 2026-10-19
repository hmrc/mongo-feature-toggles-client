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

package uk.gov.hmrc.mongoFeatureToggles.internal.model

import play.api.libs.json.*
import uk.gov.hmrc.mongoFeatureToggles.internal.model.{LocalConfiguration, RemoteConfiguration}

private[mongoFeatureToggles] case class FeatureFlagDefinition(
  name: String,
  description: Option[String] = None,
  remoteConfiguration: Option[RemoteConfiguration] = None,
  localConfiguration: Option[LocalConfiguration] = None
) {
  require(
    remoteConfiguration.isDefined ^ localConfiguration.isDefined,
    if (remoteConfiguration.isDefined && localConfiguration.isDefined) {
      "FeatureFlagDefinition cannot have both remote and local configuration defined"
    } else {
      "FeatureFlagDefinition must have either remote or local configuration defined"
    }
  )

  def isRemote: Boolean = remoteConfiguration.isDefined
}

private[mongoFeatureToggles] object FeatureFlagDefinition {
  implicit val format: Format[FeatureFlagDefinition] = Json.format[FeatureFlagDefinition]
}

private[mongoFeatureToggles] case class RemoteConfiguration(defaultStatus: Boolean = false)
private[mongoFeatureToggles] object RemoteConfiguration {
  implicit val format: Format[RemoteConfiguration] = Json.format[RemoteConfiguration]
}

private[mongoFeatureToggles] case class LocalConfiguration(isEnabled: Boolean = false)
private[mongoFeatureToggles] object LocalConfiguration {
  implicit val format: Format[LocalConfiguration] = Json.format[LocalConfiguration]
}
