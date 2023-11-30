package repositories

import org.mongodb.scala.MongoDatabase
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.result.InsertOneResult

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import domain.{Author, AuthorUpdate}
import org.mongodb.scala.bson.{BsonArray, BsonDocument, BsonInt32, BsonString, ObjectId}

import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.language.postfixOps

class AuthorRepository(implicit db:MongoDatabase) {

  private val collection = db.getCollection("authors")

  // Gets
  def getAllAuthors()(implicit ec: ExecutionContext): Future[List[Author]] = {
    collection.find().toFuture().map(_.map(docToAuthor).collect { case Some(author) => author }.toList)
  }

  //  Filter
  def customFilter(field:String, parameter:Any)(implicit ec: ExecutionContext): Future[List[Author]] = {
    collection.find(equal(field,parameter)).toFuture().map(_.map(docToAuthor).collect { case Some(author) => author }.toList)
  }


  def getAuthorById(authorId: String)(implicit ec: ExecutionContext): Future[Option[Author]] = {
    val objectId = new ObjectId(authorId)
    collection.find(equal("_id", objectId)).headOption().map(_.flatMap(docToAuthor))
  }

  // Проверка
  def doesAuthorExist(authorId:Option[String])(implicit ec: ExecutionContext): Future[Boolean] = {
    if(authorId != None) {
      val objectId = new ObjectId(authorId.get)
      collection.find(equal("_id", objectId)).headOption().map(_.isDefined)
    }else Future{true}
  }

  // Create
  def addAuthor(author: Author)(implicit ec: ExecutionContext): Future[String] = {
    val document = Document(
      "name" -> author.name,
      "age" -> author.age,
      "email" -> author.email,
      "password" -> author.password,
      "books" -> author.books
    )

    collection.insertOne(document).toFuture().map { result: InsertOneResult =>
      val insertedId = result.getInsertedId
      s"Автор успешно добавлен с идентификатором: $insertedId"
    }
  }

  // Update
  def updateAuthor(authorId: String, updatedAuthor: AuthorUpdate)(implicit ec: ExecutionContext): Future[String] = {
    val objectId = new ObjectId(authorId)
    val filter = equal("_id", objectId)

    updatedAuthor.toDocument(authorId).flatMap { updatedBson =>
      println(updatedBson.toString)
      collection.updateOne(filter, updatedBson)
        .toFuture()
        .map { updateResult => {
          if (updateResult.wasAcknowledged() && updateResult.getModifiedCount > 0) {
            "Автор успешно обновлен"
          } else {
            "Обновление автора не выполнено"
          }
        }
        }
    }
  }
  // Delete
  def deleteAuthor(authorId: String)(implicit ec: ExecutionContext): Future[String] = {
    val objectId = new ObjectId(authorId)
    collection.deleteOne(equal("_id", objectId)).toFuture().map(_ => "Автор успешно удален")
  }



  // Для взятия автора из базы
  private def docToAuthor(doc: Document): Option[Author] = {
    Option(doc).map { d =>
      Author(
        Some(d.getObjectId("_id").toHexString),
        d.getString("name"),
        d.getInteger("age"),
        d.getString("email"),
        d.getString("password"),
        d.getList("books", classOf[String]).asScala.toList
      )
    }
  }
  // Нового автора исправить и вернуть на Bson формате
  implicit class RichAuthorUpdate(authorUpdate: AuthorUpdate) {
    def toDocument(authorId: String)(implicit ec: ExecutionContext): Future[BsonDocument] = {
      val oldAuthorFuture: Future[Option[Author]] = getAuthorById(authorId)
      val updatedDocumentFuture: Future[BsonDocument] = oldAuthorFuture.flatMap {
        case Some(oldAuthor) =>
          val updatedDocument = oldAuthor.toDocumentForUpdate(authorUpdate)
          println("Бисонға айналып қайтты")
          Future.successful(updatedDocument)

        case None =>
          Future.failed(new NoSuchElementException(s"Author with id $authorId not found"))
      }

      updatedDocumentFuture
    }
  }
  // Для изменения старого автора на новую
  implicit class RichAuthor(author: Author) {
    def toDocumentForUpdate(update: AuthorUpdate): BsonDocument = {
      val document = BsonDocument("$set" -> BsonDocument(
        "name" -> BsonString(update.name.getOrElse(author.name)),
        "age" -> BsonInt32(update.age.getOrElse(author.age)),
        "email" -> BsonString(update.email.getOrElse(author.email)),
        "password" -> BsonString(update.password.getOrElse(author.password)),
        "books" -> BsonArray(update.books.getOrElse(author.books).map(BsonString(_)))
      ))
      document
    }
  }
}
