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

import org.mongodb.scala.MongoWriteException
import org.mongodb.scala.bson.BsonDocument
import play.api.Application
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport
import uk.gov.hmrc.mongoFeatureToggles.internal.model.{FeatureFlagAdmin, FeatureFlagDefinition, FeatureFlagSerialised, RemoteConfiguration}
import play.api.inject.bind
import uk.gov.hmrc.mongoFeatureToggles.testUtils.BaseSpec

import scala.concurrent.ExecutionContext.Implicits.global

class FeatureFlagRepositorySpec extends BaseSpec with DefaultPlayMongoRepositorySupport[FeatureFlagSerialised] {

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
    .configure(Map("mongodb.uri" -> mongoUri) ++ configValues)
    .overrides(
      bind[MongoComponent].toInstance(mongoComponent)
    )
    .build()

  override val checkTtlIndex = false

  override val repository: FeatureFlagRepository = app.injector.instanceOf[FeatureFlagRepository]

  "getFlag" must {
    "return None if there is no record" in {
      val result = repository
        .getFeatureFlag(
          FeatureFlagDefinition("toggle-name", Some("Description"), Some(RemoteConfiguration(false)), None)
        )
        .futureValue

      result mustBe None
    }

    "return a flag if there is a record" in {
      val featureFlagDefinition =
        FeatureFlagDefinition("toggle-name", Some("Description"), Some(RemoteConfiguration(false)), None)
      val result                = (for {
        _      <- insert(FeatureFlagSerialised("toggle-name", true))
        result <- repository
                    .getFeatureFlag(featureFlagDefinition)
      } yield result).futureValue

      result mustBe Some(FeatureFlagAdmin(featureFlagDefinition, true))
    }
  }

  "setFeatureFlag" must {
    "insert a record in mongo" in {
      val result = (for {
        _      <- repository.setFeatureFlag(FeatureFlagSerialised("toggle-name", true))
        result <- findAll()
      } yield result).futureValue

      result mustBe List(
        FeatureFlagSerialised("toggle-name", true)
      )
    }

    "replace a record not create a new one" in {
      val result = (for {
        _      <- repository.setFeatureFlag(FeatureFlagSerialised("toggle-name", true))
        _      <- repository.setFeatureFlag(FeatureFlagSerialised("toggle-name", true))
        result <- findAll()
      } yield result).futureValue

      result mustBe List(
        FeatureFlagSerialised("toggle-name", true)
      )
    }
  }

  "setFeatureFlags" must {
    "set multiple records" in {
      val toggles = Set(
        FeatureFlagSerialised("toggle-a", true),
        FeatureFlagSerialised("toggle-b", false)
      )
      val result  = (for {
        _      <- repository.setFeatureFlags(toggles)
        result <- findAll()
      } yield result).futureValue.toSet

      result mustBe toggles
    }
  }

  "getAllFeatureFlags" must {
    "get a list of all the feature toggles" in {
      val allFlags = (for {
        _      <- insert(FeatureFlagSerialised("toggle-a", true))
        result <- repository.getAllFeatureFlags
      } yield result).futureValue

      allFlags mustBe Set(
        FeatureFlagSerialised("toggle-a", true)
      )
    }
  }

  "Collection" must {
    "not allow duplicates" in {
      val result = intercept[MongoWriteException] {
        await(for {
          _ <- insert(FeatureFlagSerialised("toggle-a", true))
          _ <- insert(FeatureFlagSerialised("toggle-a", false))
        } yield true)
      }
      result.getCode mustBe 11000
      result.getError.getMessage mustBe s"""E11000 duplicate key error collection: $databaseName.admin-feature-flags index: name dup key: { name: "toggle-a" }"""
    }
  }
}
