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

import org.mongodb.scala.model.Filters.*
import org.mongodb.scala.model.*
import play.api.Logging
import play.api.libs.json.Format
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.transaction.{TransactionConfiguration, Transactions}
import uk.gov.hmrc.mongoFeatureToggles.internal.config.AppConfig
import uk.gov.hmrc.mongoFeatureToggles.internal.model.{FeatureFlagAdmin, FeatureFlagSerialised}
import uk.gov.hmrc.mongoFeatureToggles.internal.model.FeatureFlagDefinition
import uk.gov.hmrc.mdc.Mdc
import org.mongodb.scala.ToSingleObservablePublisher
import org.mongodb.scala.bson.BsonDocument

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
private[mongoFeatureToggles] class FeatureFlagRepository @Inject() (
  val mongoComponent: MongoComponent,
  appConfig: AppConfig
)(implicit
  ec: ExecutionContext
) extends PlayMongoRepository[FeatureFlagSerialised](
      collectionName = "admin-feature-flags",
      mongoComponent = mongoComponent,
      domainFormat = implicitly[Format[FeatureFlagSerialised]],
      indexes = Seq(
        IndexModel(
          keys = Indexes.ascending("name"),
          indexOptions = IndexOptions()
            .name("name")
            .unique(true)
        )
      ),
      optSchema = Some(BsonDocument("""
          { bsonType: "object"
          , required: [ "_id", "name", "isEnabled" ]
          , properties:
            { _id       : { bsonType: "objectId" }
            , name      : { bsonType: "string" }
            , isEnabled : { bsonType: "bool" }
            }
          }
        """))
    )
    with Transactions
    with Logging {

  private implicit val tc: TransactionConfiguration = TransactionConfiguration.strict

  def deleteFeatureFlag(featureFlagSerialised: FeatureFlagSerialised): Future[Boolean] =
    collection
      .deleteOne(Filters.equal("name", featureFlagSerialised.name))
      .map(_.wasAcknowledged())
      .toSingle()
      .toFuture()

  def getFeatureFlag(featureFlagDefinition: FeatureFlagDefinition): Future[Option[FeatureFlagAdmin]] =
    Mdc.preservingMdc(
      collection
        .find(Filters.equal("name", featureFlagDefinition.name))
        .headOption()
        .map(
          _.map(flag => FeatureFlagAdmin(featureFlagDefinition, flag.isEnabled))
        )
    )

  def getAllFeatureFlags: Future[Set[FeatureFlagSerialised]] =
    Mdc.preservingMdc(
      collection
        .find()
        .toFuture()
        .map(_.toSet)
    )

  def setFeatureFlag(featureFlag: FeatureFlagSerialised): Future[Boolean] =
    Mdc.preservingMdc(
      collection
        .replaceOne(
          filter = equal("name", featureFlag.name),
          replacement = featureFlag,
          options = ReplaceOptions().upsert(true)
        )
        .map(_.wasAcknowledged())
        .toSingle()
        .toFuture()
    )

  def setFeatureFlags(flags: Set[FeatureFlagSerialised]): Future[Unit] =
    Mdc.preservingMdc(
      if (appConfig.useMongoTransactions) {
        withSessionAndTransaction(session =>
          for {
            _ <- collection.deleteMany(session, filter = in("name", flags.map(_.name).toSeq: _*)).toFuture()
            _ <- collection.insertMany(session, flags.toSeq).toFuture()
          } yield ()
        )
      } else {
        logger.warn("Mongo transactions are disabled")
        Future
          .sequence(flags.map(setFeatureFlag))
          .map(_ => ())
      }
    )
}
