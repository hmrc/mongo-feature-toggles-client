/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.mongoFeatureToggles.config

import com.typesafe.config.{Config, ConfigFactory}
import play.api.Configuration
import uk.gov.hmrc.mongoFeatureToggles.internal.config.FeatureFlagsConfig
import uk.gov.hmrc.mongoFeatureToggles.internal.model.{FeatureFlagDefinition, RemoteConfiguration}
import uk.gov.hmrc.mongoFeatureToggles.testUtils.BaseSpec

class FeatureFlagsConfigSpec extends BaseSpec {

  "A valid configuration is loaded" in {
    val config: Config = ConfigFactory.parseString("""
    mongo-feature-toggles-client {
      toggles {
        toggle-name {
          description = "Description of the toggle"
          remote-configuration {
            defaultState = true # Default state when isEnable is auto and nothing is in mongo yet
          }
          local-configuration {
            isEnabled = false
          }
          toggleSourceValue = "remote-configuration"
        }
      }
    }
    """)

    val sut = new FeatureFlagsConfig(Configuration(config))

    val result = sut.getAllToggles

    result mustBe Set(
      FeatureFlagDefinition(
        name = "toggle-name",
        description = Some("Description of the toggle"),
        remoteConfiguration = Some(RemoteConfiguration(true)),
        localConfiguration = None
      )
    )
  }
}
