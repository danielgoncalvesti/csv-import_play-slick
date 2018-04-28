package models

//import org.joda.time.DateTime
//import java.util.Date
import java.sql.{Date, Timestamp}

case class Pedido(
  id: Option[Long], 
  ra: String, 
  nome: String, 
  pedido: String,
  status: String
  )

//trait PedidoDAO {
  //  def create(ra: String, nome: String, pedido: String, obs: String, status: String)
    
  //  def all() : List[Pedido]
    
  //  def findByRa(ra: String): Option [Pedido]
//}
