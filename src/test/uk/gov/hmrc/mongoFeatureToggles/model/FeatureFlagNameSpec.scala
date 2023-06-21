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

import play.api.libs.json.{JsResultException, JsString, Json}
import uk.gov.hmrc.mongoFeatureToggles.testUtils.{BaseSpec, TestToggleA, TestToggleB}

class FeatureFlagNameSpec extends BaseSpec {

  override def beforeEach(): Unit = {
    super.beforeEach()
    FeatureFlagNamesLibrary.addFlags(List(TestToggleA, TestToggleB))
  }

  "read json" in {
    JsString(TestToggleA.name).as[FeatureFlagName] mustBe TestToggleA
  }

  "throw an exception if name is invalid" in {
    val result = intercept[JsResultException] {
      JsString("invalid").as[FeatureFlagName]
    }

    result.getMessage must include("Unknown FeatureFlagName `\"invalid\"`")
  }

  "write json" in {
    Json.toJson(TestToggleA: FeatureFlagName).toString mustBe s""""${TestToggleA.name}""""
    Json.toJson(TestToggleA: FeatureFlagName).toString mustBe s""""${TestToggleA.toString}""""
  }

  "String binds to a Right(FeatureFlagName)" in {
    FeatureFlagName.pathBindable.bind("aa", TestToggleA.name) mustBe Right(TestToggleA)
  }

  "Invalid string binds to a Left" in {
    val name = "invalid"
    FeatureFlagName.pathBindable.bind("aa", name) mustBe Left(s"The feature flag `$name` does not exist")
  }

  "FeatureFlagName unbinds to a string" in {
    FeatureFlagName.pathBindable.unbind("aa", TestToggleA) mustBe TestToggleA.name
  }

}
