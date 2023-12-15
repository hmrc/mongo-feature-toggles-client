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

import uk.gov.hmrc.mongoFeatureToggles.testUtils.{BaseSpec, TestToggleA, TestToggleB}

class FeatureFlagNamesLibrarySpec extends BaseSpec {

  "getAllFlags returns all flags that have been set" in {
    object TestLibrary extends FeatureFlagNamesLibrary
    val flags = List(TestToggleA, TestToggleB)
    TestLibrary.addFlags(flags)
    TestLibrary.getAllFlags mustBe flags
  }

  "getAllFlags throw an exception" when {
    "There is no feature flags" in {
      object TestLibrary extends FeatureFlagNamesLibrary
      val result = intercept[RuntimeException] {
        TestLibrary.getAllFlags
      }
      result.getMessage mustBe "No feature flags in FeatureFlagNamesLibrary. Have you added the flags?"
    }
  }

}
