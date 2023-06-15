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

package uk.gov.hmrc.mongoFeatureToggles.internal.services

import org.mockito.ArgumentMatchers.any
import play.api.cache.AsyncCacheApi
import play.api.inject.bind
import uk.gov.hmrc.mongoFeatureToggles.internal.config.AppConfig
import uk.gov.hmrc.mongoFeatureToggles.internal.model.DeletedToggle
import uk.gov.hmrc.mongoFeatureToggles.internal.repository.FeatureFlagRepository
import uk.gov.hmrc.mongoFeatureToggles.model.{FeatureFlag, FeatureFlagName, FeatureFlagNamesLibrary}
import uk.gov.hmrc.mongoFeatureToggles.testUtils.{BaseSpec, TestToggleA, TestToggleB}

import scala.concurrent.Future

class FeatureFlagServiceWithCacheSpec extends BaseSpec {

  val mockAppConfig             = mock[AppConfig]
  val mockFeatureFlagRepository = mock[FeatureFlagRepository]

  override implicit lazy val app = localGuiceApplicationBuilder()
    .overrides(
      bind[AppConfig].toInstance(mockAppConfig),
      bind[FeatureFlagRepository].toInstance(mockFeatureFlagRepository)
    )
    .build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    FeatureFlagNamesLibrary.addFlags(List(TestToggleA, TestToggleB))
    reset(mockAppConfig, mockFeatureFlagRepository)
    cache.removeAll()
  }

  lazy val featureFlagService = inject[FeatureFlagService]
  lazy val cache              = inject[AsyncCacheApi]

  "getAll" must {
    "get all the feature flags defaulted to false" when {
      "No toggle are in Mongo" in {
        val expectedFeatureFlags = FeatureFlagNamesLibrary.getAllFlags.map(FeatureFlag(_, false))
        when(mockFeatureFlagRepository.getAllFeatureFlags).thenReturn(Future.successful(List.empty))

        val result = featureFlagService.getAll.futureValue
        result mustBe expectedFeatureFlags

        verify(mockFeatureFlagRepository, times(1)).getAllFeatureFlags
      }
    }

    "get all the feature flags" when {
      "All toggles are in Mongo" in {
        val expectedFeatureFlags = FeatureFlagNamesLibrary.getAllFlags.map(FeatureFlag(_, true))
        when(mockFeatureFlagRepository.getAllFeatureFlags).thenReturn(Future.successful(expectedFeatureFlags))

        val result = featureFlagService.getAll.futureValue
        result mustBe expectedFeatureFlags

        verify(mockFeatureFlagRepository, times(1)).getAllFeatureFlags
      }

      "some toggles are in Mongo" in {
        val mongoFlags           = List(FeatureFlag(TestToggleA, true), FeatureFlag(TestToggleB, true))
        val expectedFeatureFlags = FeatureFlagNamesLibrary.getAllFlags
          .map(FeatureFlag(_, false))
          .filterNot(flag => mongoFlags.map(_.name.toString).contains(flag.name.toString)) ::: mongoFlags
        when(mockFeatureFlagRepository.getAllFeatureFlags).thenReturn(Future.successful(mongoFlags))

        val result = featureFlagService.getAll.futureValue
        result.sortBy(_.name.toString) mustBe expectedFeatureFlags.sortBy(_.name.toString)

        verify(mockFeatureFlagRepository, times(1)).getAllFeatureFlags
      }
    }

    "get flags and delete the unused ones" in {
      val expectedFeatureFlags = FeatureFlagNamesLibrary.getAllFlags.map(FeatureFlag(_, false))
      when(mockFeatureFlagRepository.getAllFeatureFlags).thenReturn(
        Future.successful(
          List(FeatureFlag(DeletedToggle("deleted-toggle"), true), FeatureFlag(DeletedToggle("deleted-toggle2"), true))
        )
      )
      when(mockFeatureFlagRepository.deleteFeatureFlag(any())).thenReturn(Future.successful(true))

      val result = featureFlagService.getAll.futureValue
      result mustBe expectedFeatureFlags

      verify(mockFeatureFlagRepository, times(1)).getAllFeatureFlags
      verify(mockFeatureFlagRepository, times(2)).deleteFeatureFlag(any())
    }

  }

  "get" must {
    "get a feature flag set to false if not present in Mongo" in {
      val expectedFeatureFlag = FeatureFlag(TestToggleA, false)
      when(mockFeatureFlagRepository.getFeatureFlag(any())).thenReturn(Future.successful(None))

      val result = featureFlagService.get(TestToggleA).futureValue
      result mustBe expectedFeatureFlag

      verify(mockFeatureFlagRepository, times(1)).getFeatureFlag(any())
    }

    "get a feature flag and the response is cached" in {
      val featureFlag = FeatureFlag(TestToggleA, true)
      when(mockFeatureFlagRepository.getFeatureFlag(any())).thenReturn(Future.successful(Some(featureFlag)))

      val result = (for {
        _      <- featureFlagService.get(TestToggleA)
        _      <- featureFlagService.get(TestToggleA)
        result <- featureFlagService.get(TestToggleA)
      } yield result).futureValue

      result mustBe featureFlag

      verify(mockFeatureFlagRepository, times(1)).getFeatureFlag(any())
    }

    "get all feature flags and the response is cached" in {
      val featureFlags = FeatureFlagNamesLibrary.getAllFlags.map(name => FeatureFlag(name, false))
      when(mockFeatureFlagRepository.getAllFeatureFlags).thenReturn(Future.successful(List.empty))

      val result = (for {
        _      <- featureFlagService.getAll
        _      <- featureFlagService.getAll
        result <- featureFlagService.getAll
      } yield result).futureValue

      result mustBe featureFlags

      verify(mockFeatureFlagRepository, times(1)).getAllFeatureFlags
    }

    "get all feature flags but Mongo returns None" in {
      implicit def ordering: Ordering[FeatureFlagName] = new Ordering[FeatureFlagName] {
        override def compare(x: FeatureFlagName, y: FeatureFlagName): Int =
          x.toString.compareTo(y.toString)
      }
      val expectedFeatureFlags                         = FeatureFlagNamesLibrary.getAllFlags.map(name => FeatureFlag(name, false))
      when(mockFeatureFlagRepository.getAllFeatureFlags).thenReturn(Future.successful(List.empty))

      val result = featureFlagService.getAll.futureValue
      result.sortBy(_.name) mustBe expectedFeatureFlags.sortBy(_.name)

      verify(mockFeatureFlagRepository, times(1)).getAllFeatureFlags
    }
  }
}
