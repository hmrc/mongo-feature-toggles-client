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

package uk.gov.hmrc.mongoFeatureToggles.testUtils

import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.stubControllerComponents
import play.api.test.Injecting
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.internalauth.client.Predicate.Permission
import uk.gov.hmrc.internalauth.client.test.{BackendAuthComponentsStub, StubBehaviour}
import uk.gov.hmrc.internalauth.client._

import scala.concurrent.ExecutionContext.Implicits.global

trait BaseSpec
    extends AnyWordSpec
    with GuiceOneAppPerSuite
    with Matchers
    with PatienceConfiguration
    with BeforeAndAfterEach
    with MockitoSugar
    with ScalaFutures
    with Injecting {
  this: Suite =>

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val configValues: Map[String, AnyVal] =
    Map(
      "metrics.enabled"  -> false,
      "auditing.enabled" -> false
    )

  protected def localGuiceApplicationBuilder(): GuiceApplicationBuilder =
    GuiceApplicationBuilder()
      .overrides(
        bind[BackendAuthComponents].toInstance(stubBackendAuthComponents)
      )
      .configure(configValues)

  override implicit lazy val app: Application = localGuiceApplicationBuilder().build()

  val expectedPredicate: Permission =
    Permission(Resource(ResourceType("ddcn-live-admin-frontend"), ResourceLocation("*")), IAAction("ADMIN"))

  lazy val mockStubBehaviour: StubBehaviour                 = mock[StubBehaviour]
  lazy val stubBackendAuthComponents: BackendAuthComponents =
    BackendAuthComponentsStub(mockStubBehaviour)(stubControllerComponents(), implicitly)

}
