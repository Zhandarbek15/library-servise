import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import org.mongodb.scala.{MongoClient, MongoDatabase}

import scala.concurrent.ExecutionContextExecutor
import repositories._
import routing._


object Main extends App with JsonSupport {

  // Создание акторной системы
  implicit val system: ActorSystem = ActorSystem("MyAkkaHttpServer")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher


  private val mongoClient = MongoClient()
  implicit val db: MongoDatabase = mongoClient.getDatabase("library")

  implicit val userRepository: UserRepository = new UserRepository()
  implicit val authorRepository: AuthorRepository = new AuthorRepository()
  implicit val bookRepository: BookRepository = new BookRepository()

  private val userRoute = new UserRoute()
  private val authorRoute = new AuthorRoute()
  private val bookRoute = new BookRoute()

  // Добавление путей
  private val allRoutes = userRoute.route ~
    authorRoute.route ~
    bookRoute.route

  // Старт сервера
  private val bindingFuture = Http().bindAndHandle(allRoutes, "localhost", 8080)
  println(s"Server online at http://localhost:8080/")

  // Остановка сервера при завершении приложения
  sys.addShutdownHook {
    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())
  }
}
