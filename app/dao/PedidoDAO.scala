package dao

import scala.concurrent.{ ExecutionContext, Future, Await }
import javax.inject.Inject

import models.Pedido
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile
//import slick.driver.H2Driver.api._
import slick.driver.MySQLDriver.api._
import slick.driver.MySQLDriver.api._
import slick.dbio.DBIOAction
import scala.concurrent.duration._
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import play.api.Logger


class PedidoDAO @Inject() (protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext) extends HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._

  private val pedidos = TableQuery[PedidosTable]
  private val autoInc = pedidos returning pedidos.map(_.id)

  // SELECT * FROM pedido;
  def all(): Future[Seq[Pedido]] = db.run(pedidos.result)

  def allCount() = {
    val f = db.run(
      sql"select P.ID, P.RA, P.NOME, P.PEDIDO, P.STATUS, C.cnt from PEDIDO P INNER JOIN ( SELECT ra, count(ra) as cnt from PEDIDO group by ra) C ON P.ra = C.ra"
      .as[(Long, String, String, String, String, Int)]
    )
    f
  }

  def findByPedido(numPedido: String): Future[Seq[Pedido]] = {
    db.run(pedidos.filter(x => x.pedido === numPedido).result)
  }

  def findByPedidoCount(numPedido: String, page: String) = {
    val f = db.run(
      sql" select P.ID, P.RA, P.NOME, P.PEDIDO, P.STATUS, C.cnt from PEDIDO P INNER JOIN ( SELECT ra, count(ra) as cnt from PEDIDO group by ra) C ON P.ra = C.ra where P.pedido = $numPedido"
      .as[(Long, String, String, String, String, Int)]
      )
    f
  }

  def insert(pedido: Pedido): Future[Unit] = db.run(pedidos += pedido).map { _ => () }

  def insertAll(pedidos1: Seq[Pedido]) = {
    val insert = DBIO.seq(
      pedidos ++= pedidos1
    ).transactionally
    Await.result(db.run(insert), 20.seconds)
  }

  def deleteAll(idsPedidos: Seq[Long]) = {
     val delete = DBIO.seq(
       pedidos.filter(_.id inSetBind idsPedidos).delete
     ).transactionally
     Await.result(db.run(delete), 20.seconds)
  }

  def distinctNumPedido(): Future[Vector[(String, Int)]] = {
    //db.run(pedidos.distinctOn(_.status).result)
    val f = db.run(sql"select pedido, count(pedido) from PEDIDO group by pedido"
      .as[(String, Int)])
    //f.onComplete { case s => println(s"Result: $s")}
    f
  }



  def updateStatus(numLote: String, newStatus: String) = {
   Try (
        db.run(pedidos.filter(_.pedido === numLote).map(p => (p.status)).update((newStatus)))
     ) match {
          case Success(_) => {
              Logger.info("Database pedido was successfully update.")
          }
          case Failure(t) =>  Logger.error("Database pedido has an error.", t)
   }
   //val f = db.run(seqLote)
   //println(seqLotell)
    //println("seqLote": seqLote.toString)
   // seqLote.onComplete {case s => println(s"Result: $s")}

  }

  def delete(id: Long): Future[Unit] =
    db.run(pedidos.filter(_.id === id).delete).map(_ => ())

  def findByRa(ra: Option[String]): Future[Seq[Pedido]] =
    db.run(pedidos.filter(x => x.ra === ra).result)

  def findById(id: Long): Future[Option[Pedido]] =
    db.run(pedidos.filter(_.id === id).result.headOption)

  def findByRaNome(search: String) = {
    val f = db.run(
      sql"select P.ID, P.RA, P.NOME, P.PEDIDO, P.STATUS, C.cnt from PEDIDO P INNER JOIN ( SELECT ra, count(ra) as cnt from PEDIDO group by ra) C ON P.ra = C.ra WHERE P.NOME like $search"
      .as[(Long, String, String, String, String, Int)]
    )
    f
  }

  def update(id: Long, pedido: Pedido): Future[Unit] = {
    val pedidoToUpdate: Pedido = pedido.copy(Some(id))
    db.run(pedidos.filter(_.id === id).update(pedidoToUpdate)).map(_ =>())
  }


  class PedidosTable(tag: Tag) extends Table[Pedido](tag, "PEDIDO") {

    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def ra = column[String]("RA")
    def nome = column[String]("NOME")
    def pedido = column[String]("PEDIDO")
    def status = column[String]("STATUS")

    def * = (id.?, ra, nome, pedido, status) <> (Pedido.tupled, Pedido.unapply)
  }
}
