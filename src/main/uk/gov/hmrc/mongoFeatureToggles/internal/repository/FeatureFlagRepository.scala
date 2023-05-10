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

import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model._
import play.api.libs.json.{Format, JsString}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.transaction.{TransactionConfiguration, Transactions}
import uk.gov.hmrc.mongoFeatureToggles.internal.model.FeatureFlagSerialised
import uk.gov.hmrc.mongoFeatureToggles.model
import uk.gov.hmrc.mongoFeatureToggles.model.{FeatureFlag, FeatureFlagName}
import uk.gov.hmrc.play.http.logging.Mdc

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
private[internal] class FeatureFlagRepository @Inject() (
  val mongoComponent: MongoComponent
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
      )
    )
    with Transactions {

  private implicit val tc = TransactionConfiguration.strict

  def deleteFeatureFlag(name: FeatureFlagName): Future[Boolean] =
    collection
      .deleteOne(Filters.equal("name", name.toString))
      .map(_.wasAcknowledged())
      .toSingle()
      .toFuture()

  def getFeatureFlag(name: FeatureFlagName): Future[Option[FeatureFlag]] =
    Mdc.preservingMdc(
      collection
        .find(Filters.equal("name", name.toString))
        .headOption()
        .map(_.map(flag => model.FeatureFlag(JsString(flag.name).as[FeatureFlagName], flag.isEnabled, flag.description)))
    )

  def getAllFeatureFlags: Future[List[FeatureFlag]] =
    Mdc.preservingMdc(
      collection
        .find()
        .toFuture()
        .map(
          _.toList.map(flag => model.FeatureFlag(JsString(flag.name).as[FeatureFlagName], flag.isEnabled, flag.description))
        )
    )

  def setFeatureFlag(name: FeatureFlagName, enabled: Boolean): Future[Boolean] =
    Mdc.preservingMdc(
      collection
        .replaceOne(
          filter = equal("name", name),
          replacement = FeatureFlagSerialised(name.toString, enabled, name.description),
          options = ReplaceOptions().upsert(true)
        )
        .map(_.wasAcknowledged())
        .toSingle()
        .toFuture()
    )

  def setFeatureFlags(flags: Map[FeatureFlagName, Boolean]): Future[Unit] = {
    val featureFlags = flags.map { case (flag, status) =>
      FeatureFlagSerialised(flag.toString, status, flag.description)
    }.toList
    Mdc.preservingMdc(
      withSessionAndTransaction(session =>
        for {
          _ <- collection.deleteMany(session, filter = in("name", flags.keys.toSeq.map(_.toString): _*)).toFuture()
          _ <- collection.insertMany(session, featureFlags).toFuture()
        } yield ()
      )
    )
  }
}
