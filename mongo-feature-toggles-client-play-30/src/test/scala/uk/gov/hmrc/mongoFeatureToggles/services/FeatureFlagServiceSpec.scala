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

import org.apache.pekko.Done
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import play.api.Application
import play.api.cache.AsyncCacheApi
import play.api.inject.bind
import uk.gov.hmrc.mongoFeatureToggles.internal.config.AppConfig
import uk.gov.hmrc.mongoFeatureToggles.internal.model.FeatureFlagSerialised
import uk.gov.hmrc.mongoFeatureToggles.internal.repository.FeatureFlagRepository
import uk.gov.hmrc.mongoFeatureToggles.testUtils.BaseSpec

import scala.concurrent.Future
import scala.jdk.CollectionConverters.*

class FeatureFlagServiceSpec extends BaseSpec {

  val mockAppConfig: AppConfig                         = mock[AppConfig]
  val mockFeatureFlagRepository: FeatureFlagRepository = mock[FeatureFlagRepository]
  val mockCache: AsyncCacheApi                         = mock[AsyncCacheApi]

  override implicit lazy val app: Application = localGuiceApplicationBuilder()
    .overrides(
      bind[AppConfig].toInstance(mockAppConfig),
      bind[FeatureFlagRepository].toInstance(mockFeatureFlagRepository),
      bind[AsyncCacheApi].toInstance(mockCache)
    )
    .build()

  override def beforeEach(): Unit =
    reset[Object](mockAppConfig, mockFeatureFlagRepository, mockCache)

  val featureFlagService: FeatureFlagService = inject[FeatureFlagService]

  "set" must {
    "set a feature flag" in {
      when(mockCache.remove(any())).thenReturn(Future.successful(Done))
      when(mockFeatureFlagRepository.setFeatureFlag(any())).thenReturn(Future.successful(true))

      val result = featureFlagService.set("toggle-a", true).futureValue

      result mustBe true
      val eventCaptor             = ArgumentCaptor.forClass(classOf[String])
      verify(mockCache, times(1)).remove(eventCaptor.capture())
      verify(mockFeatureFlagRepository, times(1)).setFeatureFlag(any())
      val arguments: List[String] = eventCaptor.getAllValues.asScala.toList
      arguments.sorted mustBe List(
        "toggle-a"
      ).sorted
    }
  }

  "setAll" must {
    "set all the feature flags provided" in {
      when(mockCache.remove(any())).thenReturn(Future.successful(Done))
      when(mockFeatureFlagRepository.setFeatureFlags(any()))
        .thenReturn(Future.successful(()))

      val result = featureFlagService
        .setAll(
          Set(FeatureFlagSerialised("toggle-a", false), FeatureFlagSerialised("toggle-b", true))
        )
        .futureValue

      result mustBe ((): Unit)

      val eventCaptor = ArgumentCaptor.forClass(classOf[String])
      verify(mockCache, times(2)).remove(eventCaptor.capture())
      verify(mockFeatureFlagRepository, times(1)).setFeatureFlags(any())

      val arguments: List[String] = eventCaptor.getAllValues.asScala.toList
      arguments.sorted mustBe List(
        "toggle-a",
        "toggle-b"
      ).sorted
    }
  }
}
