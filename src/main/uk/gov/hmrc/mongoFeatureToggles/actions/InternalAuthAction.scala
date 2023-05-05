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

package uk.gov.hmrc.mongoFeatureToggles.actions

import com.google.inject.Inject
import play.api.Logging
import uk.gov.hmrc.internalauth.client.Predicate.Permission
import uk.gov.hmrc.internalauth.client._
import uk.gov.hmrc.mongoFeatureToggles.config.AppConfig

import scala.concurrent.ExecutionContext

class InternalAuthAction @Inject() (
  appConfig: AppConfig,
  internalAuth: BackendAuthComponents
)(implicit
  val executionContext: ExecutionContext
) extends Logging {

  private val permission: Permission =
    Permission(
      resource = Resource(
        resourceType = ResourceType(appConfig.internalAuthResourceType),
        resourceLocation = ResourceLocation("*")
      ),
      action = IAAction("ADMIN")
    )

  def apply() =
    internalAuth.authorizedAction(permission, Retrieval.username)
}
