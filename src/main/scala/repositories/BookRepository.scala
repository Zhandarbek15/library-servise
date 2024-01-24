package repositories

import org.mongodb.scala.MongoDatabase
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.result.InsertOneResult
import org.mongodb.scala.model.Updates.push

import scala.concurrent.{Await, ExecutionContext, Future}
import domain._
import org.mongodb.scala.bson.{BsonDocument, ObjectId}

import scala.concurrent.duration._
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.language.postfixOps


class BookRepository(implicit db: MongoDatabase) {

  private val collection = db.getCollection("books")

  def getAllBooks()(implicit ec: ExecutionContext): Future[List[Book]] = {
    collection.find().toFuture().map(_.map(docToBook).collect { case Some(book) => book }.toList)
  }

  def addBookToAuthor(authorId: String, newBookId: String): Unit = {
    val objectId = new ObjectId(authorId)
    val filter = equal("_id", objectId)
    val update = push("books", newBookId)

    collection.updateOne(filter, update)
  }

  def checkBooksExist(bookIds: List[String])(implicit ec: ExecutionContext): Future[Boolean] = {
    val checks = bookIds.map(bookId => collection.countDocuments(equal("_id", bookId)).toFuture.map(_ > 0))

    Future.sequence(checks).map(_.forall(identity))
  }


  //  Filter
  def customFilter(field:String, parameter:Any)(implicit ec: ExecutionContext): Future[List[Book]] = {
    collection.find(equal(field, parameter)).toFuture().map(_.map(docToBook).collect { case Some(book) => book }.toList)
  }


  def getBookById(bookId: String)(implicit ec: ExecutionContext): Future[Option[Book]] = {
    val objectId = new org.bson.types.ObjectId(bookId)
    collection.find(equal("_id", objectId)).headOption().map(_.flatMap(docToBook))
  }

  def addBook(book: Book)(implicit ec: ExecutionContext): Future[String] = {

    val document = book match {
      case tb: TextBook => tb.toDocument
      case fb: FictionBook => fb.toDocument
      case sb: ScientificBook => sb.toDocument
    }

    collection.insertOne(document).toFuture().map { result: InsertOneResult =>
      val insertedId = result.getInsertedId
      s"Книга успешно добавлена с идентификатором: $insertedId"
    }
  }

  def deleteBook(bookId: String)(implicit ec: ExecutionContext): Future[String] = {
    val objectId = new org.bson.types.ObjectId(bookId)
    collection.deleteOne(equal("_id", objectId)).toFuture().map(_ =>  "Книга успешно удалена")
  }

  def updateBook(bookId: String, updatedBook: BookUpdate)(implicit ec: ExecutionContext): Future[String] = {
    val objectId = new ObjectId(bookId)
    val filter = equal("_id", objectId)
    val updatedBson = Await.result(updatedBook.toDocument(bookId),2 second)

    val updateFuture:Future[String]=
      collection.updateOne(filter, updatedBson)
        .toFuture()
        .map { updateResult =>
          if (updateResult.wasAcknowledged() && updateResult.getModifiedCount > 0) {
            "Книга успешно обновлена"
          } else {
            "Обновление книги не выполнено"
          }
        }
    updateFuture
  }


  implicit class RichTextBook(textBook: TextBook) {
    def toDocument: BsonDocument = {
      BsonDocument(
        "name" -> textBook.name,
        "author" -> textBook.author,
        "dateRelease" -> textBook.dateRelease,
        "dateOfAcceptance" -> textBook.dateOfAcceptance,
        "numberOfPages" -> textBook.numberOfPages,
        "price" -> textBook.price,
        "keywords" -> textBook.keywords,
        "language" -> textBook.language.toString,
        "subject" -> textBook.subject.toString,
        "bookType" -> textBook.bookType
      )
    }

    def toDocumentForUpdate(bookUpdate: BookUpdate): BsonDocument = {
      BsonDocument(
        "$set" -> BsonDocument(
          "name" -> bookUpdate.name.getOrElse(textBook.name),
          "author" -> bookUpdate.author.getOrElse(textBook.author),
          "dateRelease" -> bookUpdate.dateRelease.getOrElse(textBook.dateRelease),
          "dateOfAcceptance" -> bookUpdate.dateOfAcceptance.getOrElse(textBook.dateOfAcceptance),
          "numberOfPages" -> bookUpdate.numberOfPages.getOrElse(textBook.numberOfPages),
          "price" -> bookUpdate.price.getOrElse(textBook.price),
          "keywords" -> bookUpdate.keywords.getOrElse(textBook.keywords),
          "language" -> bookUpdate.language.map(_.toString).getOrElse(textBook.language.toString),
          "subject" -> bookUpdate.subject.map(_.toString).getOrElse(textBook.subject.toString),
          "bookType" -> textBook.bookType
        )
      )
    }
  }

  implicit class RichFictionBook(fictionBook: FictionBook) {
    def toDocument: BsonDocument = {
      BsonDocument(
        "name" -> fictionBook.name,
        "author" -> fictionBook.author,
        "dateRelease" -> fictionBook.dateRelease,
        "dateOfAcceptance" -> fictionBook.dateOfAcceptance,
        "numberOfPages" -> fictionBook.numberOfPages,
        "price" -> fictionBook.price,
        "keywords" -> fictionBook.keywords,
        "language" -> fictionBook.language.toString,
        "averageRating" -> fictionBook.averageRating,
        "genre" -> fictionBook.genre.toString,
        "bookType" -> fictionBook.bookType
      )
    }

    def toDocumentForUpdate(bookUpdate: BookUpdate): BsonDocument = {
      BsonDocument(
      "$set" -> BsonDocument(
        "name" -> bookUpdate.name.getOrElse(fictionBook.name),
        "author" -> bookUpdate.author.getOrElse(fictionBook.author),
        "dateRelease" -> bookUpdate.dateRelease.getOrElse(fictionBook.dateRelease),
        "dateOfAcceptance" -> bookUpdate.dateOfAcceptance.getOrElse(fictionBook.dateOfAcceptance),
        "numberOfPages" -> bookUpdate.numberOfPages.getOrElse(fictionBook.numberOfPages),
        "price" -> bookUpdate.price.getOrElse(fictionBook.price),
        "keywords" -> bookUpdate.keywords.getOrElse(fictionBook.keywords),
        "language" -> bookUpdate.language.map(_.toString).getOrElse(fictionBook.language.toString),
        "averageRating" -> bookUpdate.averageRating.getOrElse(fictionBook.averageRating),
        "genre" -> bookUpdate.genre.map(_.toString).getOrElse(fictionBook.genre.toString),
        "bookType" -> fictionBook.bookType
        )
      )
    }
  }

  implicit class RichScientificBook(scientificBook: ScientificBook) {
    def toDocument: BsonDocument = {
      BsonDocument(
        "name" -> scientificBook.name,
        "author" -> scientificBook.author,
        "dateRelease" -> scientificBook.dateRelease,
        "dateOfAcceptance" -> scientificBook.dateOfAcceptance,
        "numberOfPages" -> scientificBook.numberOfPages,
        "price" -> scientificBook.price,
        "keywords" -> scientificBook.keywords,
        "language" -> scientificBook.language.toString,
        "subject" -> scientificBook.subject.toString,
        "typeOfWork" -> scientificBook.typeOfWork.toString,
        "bookType" -> scientificBook.bookType
      )
    }

    def toDocumentForUpdate(bookUpdate: BookUpdate): BsonDocument = {
      BsonDocument(
          "$set"-> BsonDocument(
          "name" -> bookUpdate.name.getOrElse(scientificBook.name),
          "author" -> bookUpdate.author.getOrElse(scientificBook.author),
          "dateRelease" -> bookUpdate.dateRelease.getOrElse(scientificBook.dateRelease),
          "dateOfAcceptance" -> bookUpdate.dateOfAcceptance.getOrElse(scientificBook.dateOfAcceptance),
          "numberOfPages" -> bookUpdate.numberOfPages.getOrElse(scientificBook.numberOfPages),
          "price" -> bookUpdate.price.getOrElse(scientificBook.price),
          "keywords" -> bookUpdate.keywords.getOrElse(scientificBook.keywords),
          "language" -> bookUpdate.language.map(_.toString).getOrElse(scientificBook.language.toString),
          "subject" -> bookUpdate.language.map(_.toString).getOrElse(scientificBook.subject.toString),
          "typeOfWork" -> bookUpdate.typeOfWork.map(_.toString).getOrElse(scientificBook.typeOfWork.toString),
          "bookType" -> scientificBook.bookType
        )
      )
    }
  }

  implicit class RichBookUpdate(bookUpdate: BookUpdate) {
    def toDocument(bookId: String)(implicit ec: ExecutionContext): Future[BsonDocument] = {
      // Брать обьект до изменения
      val oldBookFuture: Future[Option[Book]] = getBookById(bookId)
      // Составить фючерс
      val updatedDocumentFuture: Future[BsonDocument] = oldBookFuture.flatMap {
        case Some(oldBook) =>
          // Если обьект по айди найден обнавляем обьект по типу
          val updatedDocument = oldBook match {
            case book: TextBook => book.toDocumentForUpdate(bookUpdate)
            case book: FictionBook => book.toDocumentForUpdate(bookUpdate)
            case book: ScientificBook => book.toDocumentForUpdate(bookUpdate)
          }
          // и возврвщвем
          Future.successful(updatedDocument)

        case None =>
          // Обработка случая, когда книга не найдена
          Future.failed(new NoSuchElementException(s"Book with id $bookId not found"))
      }
      updatedDocumentFuture
    }
  }



  private def docToBook(doc: Document): Option[Book] = {
    val bookType = doc.getString("bookType")
    bookType match {
      case "TextBook" => documentToTextBook(doc)
      case "FictionBook" => documentToFictionBook(doc)
      case "ScientificBook" => documentToScientificBook(doc)
      case _ => None
    }
  }

  private def documentToTextBook(doc: Document): Option[TextBook] = {
    Some(
      TextBook(
        Some(doc.getObjectId("_id").toHexString),
        doc.getString("name"),
        doc.getString("author"),
        doc.getDate("dateRelease"),
        doc.getDate("dateOfAcceptance"),
        doc.getInteger("numberOfPages"),
        doc.getInteger("price"),
        doc.getList("keywords", classOf[String]).asScala.toList,
        Language.withName(doc.getString("language")),
        Subject.withName(doc.getString("subject")),
        doc.getString("bookType")
      )
    )
  }


  private def documentToFictionBook(doc: Document):Option[FictionBook] = {
    Some(
      FictionBook(
        Some(doc.getObjectId("_id").toHexString),
        doc.getString("name"),
        doc.getString("author"),
        doc.getDate("dateRelease"),
        doc.getDate("dateOfAcceptance"),
        doc.getInteger("numberOfPages"),
        doc.getInteger("price"),
        doc.getList("keywords", classOf[String]).asScala.toList,
        Language.withName(doc.getString("language")),
        doc.getInteger("averageRating"),
        Genre.withName(doc.getString("genre")),
        doc.getString("bookType")
      )
    )
  }

  private def documentToScientificBook(doc: Document):Option[ScientificBook] = {
    Some(
      ScientificBook(
        Some(doc.getObjectId("_id").toHexString),
        doc.getString("name"),
        doc.getString("author"),
        doc.getDate("dateRelease"),
        doc.getDate("dateOfAcceptance"),
        doc.getInteger("numberOfPages"),
        doc.getInteger("price"),
        doc.getList("keywords", classOf[String]).asScala.toList,
        Language.withName(doc.getString("language")),
        Subject.withName(doc.getString("subject")),
        TypeOfWork.withName(doc.getString("typeOfWork")),
        doc.getString("bookType")
      )
    )
  }
}