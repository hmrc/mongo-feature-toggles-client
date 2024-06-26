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

package uk.gov.hmrc.mongoFeatureToggles.internal.config

import play.api.Configuration

import javax.inject.{Inject, Singleton}

@Singleton
private[mongoFeatureToggles] class AppConfig @Inject() (configuration: Configuration) {
  val cacheTtlInSeconds: Int =
    configuration
      .getOptional[Int]("mongo-feature-toggles-client.cacheTtlInSeconds")
      .getOrElse(5)

  val internalAuthResourceType: String =
    configuration
      .getOptional[String]("microservice.services.internal-auth.resource-type")
      .getOrElse("ddcn-live-admin-frontend")

  val useMongoTransactions: Boolean =
    configuration
      .getOptional[Boolean]("mongo-feature-toggles-client.useMongoTransactions")
      .getOrElse(true)
}
