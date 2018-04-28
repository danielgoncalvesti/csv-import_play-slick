package controllers

import dao.PedidoDAO
import javax.inject._
import play.api.mvc.{ AbstractController, ControllerComponents, Flash, RequestHeader }
import play.api.i18n._
import play.api.data._
import models.Pedido
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.Forms.text
import play.api.data.Forms._
import views.html._
import play.api.mvc.Security.Authenticated
import java.sql.{Date, Timestamp}
import scala.collection.mutable

import play.api.mvc._
import play.api.routing._
import javax.inject.Inject
import scala.concurrent.Await
import scala.concurrent.duration._



import scala.concurrent.{ExecutionContext, Future}

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class PedidosCRUD @Inject() (
    pedidoDao: PedidoDAO,
    cc: ControllerComponents,
    messagesApi: MessagesApi,
    langs: Langs
    ) (implicit executionContext: ExecutionContext) extends AbstractController(cc) with I18nSupport {

    val lang: Lang = langs.availables.head
    implicit val messages: Messages = MessagesImpl(lang, messagesApi)

    val pedidoForm = Form(
      mapping(
        "id" -> optional(longNumber),
        "ra" -> nonEmptyText(minLength = 0, maxLength = 12),
        "nome" -> nonEmptyText,
        "pedido" -> nonEmptyText,
        "status" -> nonEmptyText
      )(Pedido.apply)(Pedido.unapply)
    )

    val loteForm = Form(
      single(
        "textLote" -> nonEmptyText.verifying("Formato de Lote Inválido!", validateFormLote _)
      ),
    )

    val updateForm = Form(
      tuple(
         "numLote" -> nonEmptyText,
         "newStatus" -> nonEmptyText
      )
    )

    val searchPedidoForm = Form(
      single(
        "search" -> nonEmptyText
      ),
    )

    def validateFormLote(lote: String): Boolean = {
      val arrayLinhas = lote.split("\\r?\\n")
      var errors = 0
      arrayLinhas.foreach(x=> x.split(";").toList match {
        case ra :: nome :: pedido :: status :: Nil => errors
        case _ => errors = errors + 1
      })
      if(errors > 0) false
      else true
    }

    def lote = Action { implicit request =>
      Ok(views.html.pedidoslote("Enviar lote", loteForm))
    }

    def doLote = Action.async { implicit request =>
      loteForm.bindFromRequest.fold(
        formWithErrors => { Future { BadRequest(views.html.pedidoslote("Enviar lote", formWithErrors))
                             .flashing("error" -> "Error ao carregar o lote")}},
        lote => {
          import scala.collection.mutable
          val lista = mutable.ArrayBuffer[Pedido]()
          lote.split("\\r?\\n").foreach(
            x => x.split(";").toList match{
              case ra :: nome :: pedido :: status :: Nil => {
                  lista.append(Pedido(Some(-1), ra, nome, pedido, status))

              }
              case _ => Redirect(routes.PedidosCRUD.lote).flashing("danger" -> "Error ao carregar o lote")
            }
          )//end-foreach
          pedidoDao.insertAll(lista)
          Logger.info("Lista adicionada lote: "+lista)
          Redirect(routes.PedidosCRUD.list("")).flashing("success" -> "Lote carregado com sucesso!")
        Future {Redirect(routes.PedidosCRUD.list("")).flashing("success" -> "Lote carregado com sucesso!") }
        }
      )
    }

    // Action with Async
    def list(filter: String, page: String, search: String) = Action.async { implicit request =>
        //  val distreq = pedidoDao.distinctNumPedido()
        search match {
             case "" =>
               filter match {
                    case "" => pedidoDao.allCount().flatMap{
                                case (pedidos) => pedidoDao.distinctNumPedido().map(dist => {
                                          Ok(views.html.pedidoslista2("Lista de Pedidos", pedidos.grouped(50).toVector.lift((page.toInt - 1)), filter, dist, page, searchPedidoForm, search))
                                        })
                    }

                    case _ => pedidoDao.findByPedidoCount(filter, page).flatMap{
                                case (pedidos) => pedidoDao.distinctNumPedido().map(dist => Ok(views.html.pedidoslista2("Lista de Pedidos", pedidos.grouped(50).toVector.lift((page.toInt - 1)), filter, dist, page, searchPedidoForm, search)))
                                case _ => Future {Redirect(routes.PedidosCRUD.list("")) }
                    }
                }
              case _ => pedidoDao.findByRaNome(search).flatMap{
                                case (pedidos) => pedidoDao.distinctNumPedido().map(dist => {
                                          Ok(views.html.pedidoslista2("Lista de Pedidos", pedidos.grouped(9999).toVector.lift(0), filter, dist, page, searchPedidoForm, search))
                                        })
              }

        }
    }

    def newPedido = Action { implicit request =>
        Ok(views.html.pedidosnew("Adicionar Pedido", pedidoForm))
    }

    def doNewPedido = Action.async { implicit request =>
      pedidoForm.bindFromRequest.fold(
        formWithErrors => {
           Future {
              BadRequest(views.html.pedidosnew("Adicionar Pedido",formWithErrors))
           }
        },
        pedido => {
          val msg = "Pedido adicionado com sucesso!"
          pedidoDao.insert(pedido).map(_ => Redirect(routes.PedidosCRUD.list("")).flashing("success"-> msg))
        }
      )
     }

    def editPedido(id: Long) = Action.async { implicit request =>
        val pedidoDb = for {
            pedido <- pedidoDao.findById(id)
        } yield (pedido)
        pedidoDb.map{
            case(pedido) =>
                pedido match {
                  case Some(c) => Ok(views.html.pedidosedit("Editar Pedido", pedidoForm.fill(c), id))
                  case None => NotFound
                }
        }
    }

    def doEditPedido(id: Long) = Action.async { implicit request =>
      pedidoForm.bindFromRequest.fold(
        formWithErrors => {
           Future {
              BadRequest(views.html.pedidosedit("Editar Pedido",formWithErrors, id))
           }
        },
        pedido => {
          val msg = "Pedido editado com sucesso!"
          pedidoDao.update(id, pedido).map(_ => Redirect(routes.PedidosCRUD.list("")).flashing("success"-> msg))
        }
      )
     }

   def doDelete(id: Long, pedido: String) = Action.async { implicit request =>
        for {
            _ <- pedidoDao.delete(id)
        } yield  Redirect(routes.PedidosCRUD.list(pedido))
    }

   def doDeleteAll = Action { implicit request =>
      import play.api.libs.json.Json
      val json: String = request.body.asJson.getOrElse(0).toString().replaceAll("\"", "")
      val json2: String = json.replaceAll("[\\[\\]]","")
      val arrayBom: Seq[Long] = json2.split(",").map(_.toLong)

      pedidoDao.deleteAll(arrayBom)
      Ok(Json.toJson(arrayBom))
    }

    def delete(input : String) = Action { implicit request =>
      try {
        val id = java.lang.Long.parseLong(input)

        pedidoDao.delete(id)
        Ok("Delete valor: " + id)

      } catch { case _ : Throwable => BadRequest }
    }

    def editStatusLote() = Action { implicit request =>

       Ok(views.html.statusedit("Alterar Status do Lote", updateForm))
    }

    def doEditStatusLote() = Action.async { implicit request =>
       updateForm.bindFromRequest.fold(
         formWithErrors => Future {BadRequest(views.html.statusedit("Alterar Status do Lote", formWithErrors))},
         {
             case(numLote, newStatus) => {
                val resultado = pedidoDao.updateStatus(numLote, newStatus)
                Future {Redirect(routes.PedidosCRUD.list("")).flashing("success"-> s"Status do Lote: $numLote atualizado com sucesso!")}
             }
         }
       )
    }

}
