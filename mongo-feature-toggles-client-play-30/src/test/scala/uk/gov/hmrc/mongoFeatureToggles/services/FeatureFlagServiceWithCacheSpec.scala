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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import play.api.Application
import play.api.cache.AsyncCacheApi
import play.api.inject.bind
import uk.gov.hmrc.mongoFeatureToggles.internal.config.{AppConfig, FeatureFlagsConfig}
import uk.gov.hmrc.mongoFeatureToggles.internal.model.{FeatureFlagAdmin, FeatureFlagDefinition, FeatureFlagSerialised, LocalConfiguration, RemoteConfiguration}
import uk.gov.hmrc.mongoFeatureToggles.internal.repository.FeatureFlagRepository
import uk.gov.hmrc.mongoFeatureToggles.model.{FeatureFlag, FeatureFlagName}
import uk.gov.hmrc.mongoFeatureToggles.testUtils.BaseSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class FeatureFlagServiceWithCacheSpec extends BaseSpec {

  val mockAppConfig: AppConfig                         = mock[AppConfig]
  val mockFeatureFlagsConfig: FeatureFlagsConfig       = mock[FeatureFlagsConfig]
  val mockFeatureFlagRepository: FeatureFlagRepository = mock[FeatureFlagRepository]

  override implicit lazy val app: Application = localGuiceApplicationBuilder()
    .overrides(
      bind[AppConfig].toInstance(mockAppConfig),
      bind[FeatureFlagRepository].toInstance(mockFeatureFlagRepository),
      bind[FeatureFlagsConfig].toInstance(mockFeatureFlagsConfig)
    )
    .build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset[Object](mockAppConfig, mockFeatureFlagRepository)
    cache.removeAll()
  }

  lazy val featureFlagService: FeatureFlagService = inject[FeatureFlagService]
  lazy val cache: AsyncCacheApi                   = inject[AsyncCacheApi]

  "getAll" must {
    "get all the feature flags defaulted to false" when {
      "No toggle are in Mongo" in {
        when(mockFeatureFlagRepository.getAllFeatureFlags).thenReturn(Future.successful(Set.empty))
        when(mockFeatureFlagRepository.deleteFeatureFlag(any())).thenReturn(Future.successful(true))
        when(mockFeatureFlagsConfig.getAllToggles).thenReturn(
          Set(
            FeatureFlagDefinition(
              "toggle-name",
              Some("Description"),
              Some(RemoteConfiguration(defaultStatus = true)),
              None
            ),
            FeatureFlagDefinition(
              "toggle-name-second",
              Some("Description"),
              None,
              Some(LocalConfiguration(isEnabled = true))
            )
          )
        )

        val result = featureFlagService.getAll.futureValue
        result mustBe Set(
          FeatureFlagAdmin(
            FeatureFlagDefinition(
              "toggle-name",
              Some("Description"),
              Some(RemoteConfiguration(defaultStatus = true)),
              None
            ),
            true
          ),
          FeatureFlagAdmin(
            FeatureFlagDefinition(
              "toggle-name-second",
              Some("Description"),
              None,
              Some(LocalConfiguration(isEnabled = true))
            ),
            false
          )
        )

        verify(mockFeatureFlagRepository, times(1)).getAllFeatureFlags
      }
    }

    "get all the feature flags" when {
      "All toggles are in Mongo" in {
        val flags = Set(
          FeatureFlagSerialised("toggle-name", true),
          FeatureFlagSerialised("toggle-name-second", true)
        )

        when(mockFeatureFlagRepository.getAllFeatureFlags).thenReturn(
          Future.successful(flags)
        )
        when(mockFeatureFlagRepository.deleteFeatureFlag(any())).thenReturn(Future.successful(true))

        val result = featureFlagService.getAll.futureValue
        result mustBe Set(
          FeatureFlagAdmin(
            FeatureFlagDefinition(
              "toggle-name",
              Some("Description"),
              Some(RemoteConfiguration(defaultStatus = true)),
              None
            ),
            true
          ),
          FeatureFlagAdmin(
            FeatureFlagDefinition(
              "toggle-name-second",
              Some("Description"),
              None,
              Some(LocalConfiguration(isEnabled = true))
            ),
            true
          )
        )

        verify(mockFeatureFlagRepository, times(1)).getAllFeatureFlags
      }

    }

  }

  "get" must {
    "get a feature flag set to false if not present in Mongo" in {
      case object TestToggleA extends FeatureFlagName {
        override val name = "toggle-name-second"
      }

      when(mockFeatureFlagRepository.getFeatureFlag(any())).thenReturn(Future.successful(None))

      when(mockFeatureFlagsConfig.get(any())).thenReturn(
        Some(
          FeatureFlagDefinition(
            "toggle-name-second",
            Some("Description"),
            Some(RemoteConfiguration(defaultStatus = false)),
            None
          )
        )
      )

      val result = featureFlagService.get(TestToggleA).futureValue
      result mustBe FeatureFlag(TestToggleA, false)

      verify(mockFeatureFlagRepository, times(1)).getFeatureFlag(any())
    }

    "get a feature flag and the response is cached" in {
      case object TestToggleA extends FeatureFlagName {
        override val name = "test-toggle"
      }
      val featureFlag = FeatureFlagAdmin(
        FeatureFlagDefinition("toggle-a", Some("Description"), Some(RemoteConfiguration(defaultStatus = false))),
        true
      )

      when(mockFeatureFlagRepository.getFeatureFlag(any())).thenReturn(Future.successful(Some(featureFlag)))
      when(mockFeatureFlagsConfig.get(any())).thenReturn(
        Some(
          FeatureFlagDefinition(
            "toggle-a",
            Some("Description"),
            Some(RemoteConfiguration(defaultStatus = false)),
            None
          )
        )
      )

      val result = (for {
        _      <- featureFlagService.get(TestToggleA)
        _      <- featureFlagService.get(TestToggleA)
        result <- featureFlagService.get(TestToggleA)
      } yield result).futureValue

      result mustBe FeatureFlag(TestToggleA, true)

      verify(mockFeatureFlagRepository, times(1)).getFeatureFlag(any())
    }

    "get all feature flags but Mongo returns None" in {
      when(mockFeatureFlagRepository.getAllFeatureFlags).thenReturn(Future.successful(Set.empty))
      when(mockFeatureFlagsConfig.getAllToggles).thenReturn(Set.empty)

      val result = featureFlagService.getAll.futureValue
      result mustBe Set.empty[FeatureFlagAdmin]

      verify(mockFeatureFlagRepository, times(1)).getAllFeatureFlags
    }
  }

  "getAsEitherT" must {
    "do the same as get but return an EitherT" in {
      val featureFlag = FeatureFlagAdmin(
        FeatureFlagDefinition(
          "toggle-a",
          Some("Description"),
          Some(RemoteConfiguration(defaultStatus = false))
        ),
        true
      )
      case object TestToggleA extends FeatureFlagName {
        override val name = "toggle-a"
      }
      when(mockFeatureFlagRepository.getFeatureFlag(any())).thenReturn(
        Future.successful(Some(featureFlag))
      )

      when(mockFeatureFlagsConfig.get(any())).thenReturn(
        Some(
          FeatureFlagDefinition(
            "toggle-a",
            Some("Description"),
            Some(RemoteConfiguration(defaultStatus = false))
          )
        )
      )

      val resultEitherT = featureFlagService.getAsEitherT(TestToggleA).value.futureValue
      val result        = Right(featureFlagService.get(TestToggleA).futureValue)
      result mustBe resultEitherT

      verify(mockFeatureFlagRepository, times(1)).getFeatureFlag(any())
    }
  }
}
