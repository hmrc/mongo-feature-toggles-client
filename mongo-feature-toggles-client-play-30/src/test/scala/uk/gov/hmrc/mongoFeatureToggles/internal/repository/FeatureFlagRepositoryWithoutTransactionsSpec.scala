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

package uk.gov.hmrc.mongoFeatureToggles.internal.repository

import org.mongodb.scala.bson.BsonDocument
import play.api.Application
import play.api.inject.bind
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport
import uk.gov.hmrc.mongoFeatureToggles.internal.model.FeatureFlagSerialised
import uk.gov.hmrc.mongoFeatureToggles.model.{FeatureFlagName, FeatureFlagNamesLibrary}
import uk.gov.hmrc.mongoFeatureToggles.testUtils.{BaseSpec, TestToggleA, TestToggleB}

import scala.concurrent.ExecutionContext.Implicits.global

class FeatureFlagRepositoryWithoutTransactionsSpec
    extends BaseSpec
    with DefaultPlayMongoRepositorySupport[FeatureFlagSerialised] {

  override def beforeEach(): Unit = {
    super.beforeEach()
    FeatureFlagNamesLibrary.addFlags(List(TestToggleA, TestToggleB))
  }

  override protected lazy val optSchema: Option[BsonDocument] = Some(BsonDocument("""
      { bsonType: "object"
      , required: [ "_id", "name", "isEnabled" ]
      , properties:
        { _id       : { bsonType: "objectId" }
        , name      : { bsonType: "string" }
        , isEnabled : { bsonType: "bool" }
        , description : { bsonType: "string" }
        }
      }
    """))

  override implicit lazy val app: Application = localGuiceApplicationBuilder()
    .configure(
      Map("mongodb.uri" -> mongoUri, "mongo-feature-toggles-client.useMongoTransactions" -> "false")
        ++ configValues
    )
    .overrides(
      bind[MongoComponent].toInstance(mongoComponent)
    )
    .build()

  override val checkTtlIndex = false

  lazy val repository: FeatureFlagRepository = app.injector.instanceOf[FeatureFlagRepository]

  "setFeatureFlags" must {
    "set multiple records" in {
      val expectedFlags: Map[FeatureFlagName, Boolean] =
        Map(TestToggleA -> false, TestToggleB -> true)
      val result                                       = (for {
        _      <- repository.setFeatureFlags(expectedFlags)
        result <- findAll()
      } yield result).futureValue

      result.sortBy(_.name) mustBe expectedFlags
        .map { case (key, value) =>
          FeatureFlagSerialised(key.name, value, key.description)
        }
        .toList
        .sortBy(_.name)
    }
  }
}
