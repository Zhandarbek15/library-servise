package routing

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import domain._
import repositories.{AuthorRepository, BookRepository}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class BookRoute(implicit val bookRepo: BookRepository, val authorRepo:AuthorRepository, val ex:ExecutionContext)
  extends JsonSupport {

  private val fields: List[String] = List(
    "name",
    "author",
    "language",
    "subject",
    "genre",
    "typeOfWork"
  )

  val route: Route = pathPrefix("books") {
    pathEndOrSingleSlash {
      (get & parameters("field", "parameter")) {
        (field, parameter) => {
          if (!fields.contains(field)) {
            complete(StatusCodes.BadRequest,
              s"Вы ввели неправильный имя поля таблицы! Допустимые поля: ${fields.mkString(", ")}")
          }
          else {
            val convertedParameter = if (parameter.matches("-?\\d+")) parameter.toInt else parameter
            onComplete(bookRepo.customFilter(field, convertedParameter)) {
              case Success(queryResponse) => complete(StatusCodes.OK, queryResponse)
              case Failure(ex) =>
                complete(StatusCodes.InternalServerError, s"Не удалось сделать запрос! ${ex.getMessage}")
            }
          }
        }
      } ~
      get {
        onComplete(bookRepo.getAllBooks()) {
          case Success(result) =>
            val bookList = result
            complete(StatusCodes.OK, bookList)
          case Failure(ex) => complete(StatusCodes.InternalServerError, s"Ошибка в коде: ${ex.getMessage}")
        }
      } ~
        post {
          entity(as[Book]) {
            newBook => {
              val authorId = newBook match {
                case tb:TextBook => tb.author
                case fb:FictionBook => fb.author
                case sb:ScientificBook => sb.author
              }
              onComplete(authorRepo.doesAuthorExist(Some(authorId))) {
                case Success(true)=>
                  onComplete(bookRepo.addBook(newBook)) {
                    case Success(newBookId) =>
                      bookRepo.addBookToAuthor(authorId,newBookId)
                      complete(StatusCodes.Created, s"ID новой книги: $newBookId")
                    case Failure(ex) =>
                      complete(StatusCodes.InternalServerError, s"Не удалось добавить книгу: ${ex.getMessage}")
                  }
                case Success(false) =>
                  complete(StatusCodes.BadRequest, s"Не правильный айди автора")
                case Failure(ex) =>
                  complete(StatusCodes.InternalServerError, s"Ошибка при проверке книги: ${ex.getMessage}")
              }
            }
          }
        }
    } ~
      path(Segment) { bookId =>
        get {
          onComplete(bookRepo.getBookById(bookId)) {
            case Success(Some(book)) => complete(StatusCodes.OK, book)
            case Success(None) => complete(StatusCodes.NotFound, s"Книги под ID $bookId не существует!")
            case Failure(ex) => complete(StatusCodes.InternalServerError, s"Ошибка в коде: ${ex.getMessage}")
          }
        } ~
          put {
            entity(as[BookUpdate]) { updatedBook => {
              onComplete(authorRepo.doesAuthorExist(updatedBook.author)) {
                case Success(true) =>
                  onComplete(bookRepo.updateBook(bookId, updatedBook)) {
                    case Success(updatedBookId) =>
                      complete(StatusCodes.OK,s"ID обнавленной книги $updatedBookId")
                    case Failure(ex) => complete(StatusCodes.InternalServerError, s"Ошибка в коде: ${ex.getMessage}")
                  }
                case Success(false) =>
                  complete(StatusCodes.BadRequest, s"Не правильный айди автора")
                case Failure(ex) =>
                  complete(StatusCodes.InternalServerError, s"Ошибка при проверке книги: ${ex.getMessage}")
              }
            }
            }
          } ~
          delete {
            onComplete(bookRepo.deleteBook(bookId)) {
              case Success(deletedBookId) =>
                complete(StatusCodes.OK, s"ID удаленной книги: $deletedBookId")
              case Failure(ex) => complete(StatusCodes.InternalServerError, s"Ошибка в коде: ${ex.getMessage}")
            }
          }
      }
  }
}
