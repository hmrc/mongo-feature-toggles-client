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

import org.mockito.ArgumentMatchers.any
import play.api.Application
import play.api.Play.materializer
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, NO_CONTENT, OK}
import play.api.inject.bind
import play.api.libs.json.{JsBoolean, Json}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, defaultAwaitTimeout, status}
import uk.gov.hmrc.internalauth.client.Predicate.Permission
import uk.gov.hmrc.internalauth.client.{IAAction, Resource, ResourceLocation, ResourceType, Retrieval}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport
import uk.gov.hmrc.mongoFeatureToggles.internal.model.FeatureFlagSerialised
import uk.gov.hmrc.mongoFeatureToggles.internal.repository.FeatureFlagRepository
import uk.gov.hmrc.mongoFeatureToggles.model.{FeatureFlag, FeatureFlagNamesLibrary}
import uk.gov.hmrc.mongoFeatureToggles.testUtils.{BaseSpec, TestToggleA, TestToggleB}

import scala.concurrent.Future

class FeatureFlagsAdminControllerSpec extends BaseSpec with DefaultPlayMongoRepositorySupport[FeatureFlagSerialised] {

  private val extraConfigValues: Map[String, Any] = Map(
    "mongodb.uri"                                    -> mongoUri,
    "mongo-feature-toggles-client.cacheTtlInSeconds" -> 0
  )

  override implicit lazy val app: Application = localGuiceApplicationBuilder()
    .configure(
      extraConfigValues ++ configValues
    )
    .overrides(
      bind[MongoComponent].toInstance(mongoComponent)
    )
    .build()

  val featureFlags: List[FeatureFlag]         = List(FeatureFlag(TestToggleA, false), FeatureFlag(TestToggleB, false))
  val controller: FeatureFlagsAdminController = app.injector.instanceOf[FeatureFlagsAdminController]

  override protected val repository: PlayMongoRepository[FeatureFlagSerialised] =
    app.injector.instanceOf[FeatureFlagRepository]
  override val checkTtlIndex                                                    = false

  override def beforeEach(): Unit = {
    super.beforeEach()
    FeatureFlagNamesLibrary.addFlags(featureFlags.map(_.name))
    reset(mockStubBehaviour)
  }

  "getAll" must {
    "get all the flags" in {
      val request = FakeRequest("GET", "/")

      val result: Future[Result] = controller.get(request)
      status(result) mustBe OK
      contentAsJson(result).as[List[FeatureFlag]] mustBe featureFlags
    }
  }

  "put" must {
    "set the flag status" in {
      when(mockStubBehaviour.stubAuth[Unit](any(), any())).thenReturn(Future.unit)

      val request = FakeRequest("GET", "/")
        .withHeaders(HeaderNames.AUTHORIZATION -> "1")
        .withJsonBody(JsBoolean(true))

      val result: Future[Result] = controller.put(TestToggleA)(request)
      status(result) mustBe NO_CONTENT
      verify(mockStubBehaviour).stubAuth(
        Some(
          Permission(
            resource = Resource(
              resourceType = ResourceType("ddcn-live-admin-frontend"),
              resourceLocation = ResourceLocation("*")
            ),
            action = IAAction("ADMIN")
          )
        ),
        Retrieval.EmptyRetrieval
      )
    }

    "returns bad request" in {
      when(mockStubBehaviour.stubAuth[Unit](any(), any())).thenReturn(Future.unit)

      val request = FakeRequest("GET", "/")
        .withHeaders(HeaderNames.AUTHORIZATION -> "1")
        .withBody("Invalid")

      val result: Future[Result] = controller.put(TestToggleA)(request).run()
      status(result) mustBe BAD_REQUEST
      verify(mockStubBehaviour).stubAuth(
        Some(
          Permission(
            resource = Resource(
              resourceType = ResourceType("ddcn-live-admin-frontend"),
              resourceLocation = ResourceLocation("*")
            ),
            action = IAAction("ADMIN")
          )
        ),
        Retrieval.EmptyRetrieval
      )
    }
  }

  "putAll" must {
    "set multiple flags statuses" in {
      when(mockStubBehaviour.stubAuth[Unit](any(), any())).thenReturn(Future.unit)

      val request = FakeRequest("GET", "/")
        .withHeaders(HeaderNames.AUTHORIZATION -> "1")
        .withJsonBody(Json.toJson(featureFlags))

      val result: Future[Result] = controller.putAll(request)
      status(result) mustBe NO_CONTENT
      verify(mockStubBehaviour).stubAuth(
        Some(
          Permission(
            resource = Resource(
              resourceType = ResourceType("ddcn-live-admin-frontend"),
              resourceLocation = ResourceLocation("*")
            ),
            action = IAAction("ADMIN")
          )
        ),
        Retrieval.EmptyRetrieval
      )
    }

    "returns bad request when body is invalid" in {
      when(mockStubBehaviour.stubAuth[Unit](any(), any())).thenReturn(Future.unit)

      val request = FakeRequest("GET", "/")
        .withHeaders(HeaderNames.AUTHORIZATION -> "1")
        .withBody("invalid")

      val result: Future[Result] = controller.putAll(request).run()
      status(result) mustBe BAD_REQUEST
      verify(mockStubBehaviour).stubAuth(
        Some(
          Permission(
            resource = Resource(
              resourceType = ResourceType("ddcn-live-admin-frontend"),
              resourceLocation = ResourceLocation("*")
            ),
            action = IAAction("ADMIN")
          )
        ),
        Retrieval.EmptyRetrieval
      )
    }
  }

}
