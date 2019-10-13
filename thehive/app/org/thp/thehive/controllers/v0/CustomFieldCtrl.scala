package org.thp.thehive.controllers.v0

import javax.inject.{Inject, Singleton}
import org.thp.scalligraph.auth.Permission
import org.thp.scalligraph.controllers.{EntryPoint, FieldsParser}
import org.thp.scalligraph.models.Database
import org.thp.scalligraph.query.PropertyUpdater
import org.thp.scalligraph.steps.StepsOps._
import org.thp.thehive.controllers.v0.Conversion._
import org.thp.thehive.dto.v0.InputCustomField
import org.thp.thehive.models.Permissions
import org.thp.thehive.services.CustomFieldSrv
import play.api.libs.json.{JsNumber, JsObject, Json}
import play.api.mvc.{Action, AnyContent, Results}

import scala.util.Success

@Singleton
class CustomFieldCtrl @Inject()(entryPoint: EntryPoint, db: Database, properties: Properties, customFieldSrv: CustomFieldSrv) extends AuditRenderer {

  val permissions: Set[Permission] = Set(Permissions.manageCustomField)

  def create: Action[AnyContent] =
    entryPoint("create custom field")
      .extract("customField", FieldsParser[InputCustomField])
      .authPermittedTransaction(db, permissions) { implicit request => implicit graph =>
        val customField: InputCustomField = request.body("customField")
        customFieldSrv
          .create(customField.toCustomField)
          .map(createdCustomField => Results.Created(createdCustomField.toJson))
      }

  def list: Action[AnyContent] =
    entryPoint("list custom fields")
      .authRoTransaction(db) { _ => implicit graph =>
        val customFields = customFieldSrv
          .initSteps
          .map(_.toJson)
          .toList
        Success(Results.Ok(Json.toJson(customFields)))
      }

  def delete(id: String): Action[AnyContent] =
    entryPoint("delete custom field")
      .extract("force", FieldsParser.boolean.optional.on("force"))
      .authPermittedTransaction(db, permissions) { implicit request => implicit graph =>
        val force = request.body("force").getOrElse(false)
        for {
          cf <- customFieldSrv.getOrFail(id)
          _  <- customFieldSrv.delete(cf, force)
        } yield Results.NoContent
      }

  def update(id: String): Action[AnyContent] =
    entryPoint("update custom field")
      .extract("customField", FieldsParser.update("customField", properties.customField))
      .authPermittedTransaction(db, permissions) { implicit request => implicit graph =>
        val propertyUpdaters: Seq[PropertyUpdater] = request.body("customField")

        for {
          updated <- customFieldSrv.update(customFieldSrv.get(id), propertyUpdaters)
          cf      <- updated._1.getOrFail()
        } yield Results.Ok(cf.toJson)
      }

  def useCount(id: String): Action[AnyContent] =
    entryPoint("get use count of custom field")
      .authPermittedTransaction(db, permissions) { _ => implicit graph =>
        customFieldSrv.getOrFail(id).map(customFieldSrv.useCount).map { countMap =>
          val total = countMap.valuesIterator.sum
          val countStats = JsObject(countMap.map {
            case (k, v) => fromObjectType(k) -> JsNumber(v)
          })
          Results.Ok(countStats + ("total" -> JsNumber(total)))
        }
      }
}
