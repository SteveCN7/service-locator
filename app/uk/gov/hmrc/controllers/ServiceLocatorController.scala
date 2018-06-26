/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.controllers

import play.api.Logger
import play.api.libs.json.Json.JsValueWrapper
import play.api.libs.json._
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.models.ErrorCode
import uk.gov.hmrc.models.ErrorCode._
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}

trait ServiceLocatorController extends BaseController {

  override protected def withJsonBody[T](f: (T) => Future[Result])
                                        (implicit request: Request[JsValue], m: Manifest[T], reads: Reads[T]): Future[Result] = {

    Try(request.body.validate[T]) match {
      case Success(JsSuccess(payload, _)) => f(payload)
      case Success(JsError(errs)) => Future(UnprocessableEntity(error(INVALID_REQUEST_PAYLOAD, JsError.toJson(errs))))
      case Failure(e) => Future(UnprocessableEntity(error(INVALID_REQUEST_PAYLOAD, e.getMessage)))
    }
  }

  def error(errorCode: ErrorCode.Value, message: JsValueWrapper): JsObject = {
    Json.obj(
      "code" -> errorCode.toString,
      "message" -> message
    )
  }

  def recovery: PartialFunction[Throwable, Result] = {
    case e =>
      Logger.error(s"An unexpected error occurred: ${e.getMessage}", e)
      InternalServerError(error(ErrorCode.UNKNOWN_ERROR, "An unexpected error occurred"))
  }

}
