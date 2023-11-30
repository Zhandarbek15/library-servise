import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import domain._
import org.bson.types.ObjectId
import org.mongodb.scala.MongoClient
import repositories.AuthorRepository

import java.sql.Date
import java.time.LocalDate
import scala.concurrent.ExecutionContextExecutor


implicit val system: ActorSystem = ActorSystem("MyAkkaHttpServer")
implicit val materializer: ActorMaterializer = ActorMaterializer()
implicit val executionContext: ExecutionContextExecutor = system.dispatcher

val client = MongoClient()
implicit val db = client.getDatabase("library")
val authorsRepo = new AuthorRepository()
val author = AuthorUpdate(name = Some("Новый автор"),age = Some(20), email = None, password = None, books = None)
val messege = authorsRepo.updateAuthor("656022a2d5c6ed0457fa2423",author)
