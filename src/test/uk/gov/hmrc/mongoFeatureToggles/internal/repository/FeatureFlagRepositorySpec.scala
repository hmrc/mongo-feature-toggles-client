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
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport
import uk.gov.hmrc.mongoFeatureToggles.internal.model.{DeletedToggle, FeatureFlagSerialised}
import uk.gov.hmrc.mongoFeatureToggles.model.{FeatureFlag, FeatureFlagName, FeatureFlagNamesLibrary}
import play.api.inject.bind
import uk.gov.hmrc.mongoFeatureToggles.testUtils.{BaseSpec, TestToggleA, TestToggleB}
import scala.concurrent.ExecutionContext.Implicits.global
class FeatureFlagRepositorySpec extends BaseSpec with DefaultPlayMongoRepositorySupport[FeatureFlagSerialised] {

  override def beforeEach(): Unit = {
    super.beforeEach()
    FeatureFlagNamesLibrary.addFlags(List(TestToggleA, TestToggleB))
  }

  override protected lazy val optSchema = Some(BsonDocument("""
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

  override implicit lazy val app = localGuiceApplicationBuilder()
    .configure(Map("mongodb.uri" -> mongoUri) ++ configValues)
    .overrides(
      bind[MongoComponent].toInstance(mongoComponent)
    )
    .build()

  override val checkTtlIndex = false

  lazy val repository = app.injector.instanceOf[FeatureFlagRepository]

  "getFlag" must {
    "return None if there is no record" in {
      val result = repository.getFeatureFlag(TestToggleA).futureValue

      result mustBe None
    }

    "return a flag if there is a record" in {
      val result = (for {
        _      <- insert(FeatureFlagSerialised(TestToggleA.name, true, TestToggleA.description))
        result <- repository.getFeatureFlag(TestToggleA)
      } yield result).futureValue

      result mustBe Some(FeatureFlag(TestToggleA, true, TestToggleA.description))
    }
  }

  "setFeatureFlag" must {
    "insert a record in mongo" in {
      val result = (for {
        _      <- repository.setFeatureFlag(TestToggleA, true)
        result <- findAll()
      } yield result).futureValue

      result mustBe List(
        FeatureFlagSerialised(TestToggleA.name, true, TestToggleA.description)
      )
    }

    "replace a record not create a new one" in {
      val result = (for {
        _      <- repository.setFeatureFlag(TestToggleA, true)
        _      <- repository.setFeatureFlag(TestToggleA, false)
        result <- findAll()
      } yield result).futureValue

      result mustBe List(
        FeatureFlagSerialised(TestToggleA.name, false, TestToggleA.description)
      )
    }
  }

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

  "getAllFeatureFlags" must {
    "get a list of all the feature toggles" in {
      val allFlags: Seq[FeatureFlag] = (for {
        _      <- insert(FeatureFlagSerialised(TestToggleA.name, true, TestToggleA.description))
        result <- repository.getAllFeatureFlags
      } yield result).futureValue

      allFlags mustBe List(
        FeatureFlag(TestToggleA, true, TestToggleA.description)
      )
    }

    "get a deleted toggle" in {
      val allFlags: Seq[FeatureFlag] = (for {
        _      <- insert(FeatureFlagSerialised("invalid", true, Some("invalid")))
        result <- repository.getAllFeatureFlags
      } yield result).futureValue

      allFlags mustBe List(
        FeatureFlag(DeletedToggle("invalid"), false)
      )
    }
  }

  "deleteFeatureFlag" must {
    "delete a mongo record" in {
      val allFlags: Boolean = (for {
        _      <- insert(FeatureFlagSerialised(TestToggleA.name, true, TestToggleA.description))
        result <- repository.deleteFeatureFlag(TestToggleA)
      } yield result).futureValue

      allFlags mustBe true
      findAll().futureValue.length mustBe 0
    }
  }

  "Collection" must {
    "not allow duplicates" in {
      val result = intercept[MongoWriteException] {
        await(for {
          _ <- insert(FeatureFlagSerialised(TestToggleA.name, true, TestToggleA.description))
          _ <- insert(FeatureFlagSerialised(TestToggleA.name, false, TestToggleB.description))
        } yield true)
      }
      result.getCode mustBe 11000
      result.getError.getMessage mustBe s"""E11000 duplicate key error collection: $databaseName.admin-feature-flags index: name dup key: { name: "$TestToggleA" }"""
    }
  }
}
