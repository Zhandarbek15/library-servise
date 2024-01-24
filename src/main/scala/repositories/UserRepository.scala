package repositories

import org.mongodb.scala.MongoDatabase
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.result.InsertOneResult

import scala.concurrent.{ExecutionContext, Future}
import domain.{User, UserUpdate}
import org.mongodb.scala.bson.{BsonArray, BsonDocument, BsonInt32, BsonString, ObjectId}
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.language.postfixOps

class UserRepository(implicit db:MongoDatabase) {

  private val collection = db.getCollection("users")

  // Gets
  def getAllUsers()(implicit ec: ExecutionContext): Future[List[User]] = {
    collection.find().toFuture().map(_.map(docToUser).collect { case Some(user) => user }.toList)
  }

  // Filter
  def customFilter(field: String, parameter: Any)(implicit ec: ExecutionContext): Future[List[User]] = {
    collection.find(equal(field, parameter)).toFuture().map(_.map(docToUser).collect { case Some(user) => user }.toList)
  }

  def getUserById(userId: String)(implicit ec: ExecutionContext): Future[Option[User]] = {
    val objectId = new ObjectId(userId)
    collection.find(equal("_id", objectId)).headOption().map(_.flatMap(docToUser))
  }

  // Create
  def addUser(user: User)(implicit ec: ExecutionContext): Future[String] = {
    val document = Document(
      "name" -> user.name,
      "age" -> user.age,
      "email" -> user.email,
      "password" -> user.password,
      "phoneNumber" -> user.phoneNumber,
      "booksBorrowed" -> user.booksBorrowed
    )

    collection.insertOne(document).toFuture().map { result: InsertOneResult =>
      val insertedId = result.getInsertedId
      s"Пользователь успешно добавлен с идентификатором: $insertedId"
    }
  }

  // Update
  def updateUser(userId: String, updatedUser: UserUpdate)(implicit ec: ExecutionContext): Future[String] = {
    val objectId = new ObjectId(userId)
    val filter = equal("_id", objectId)

    updatedUser.toDocument(userId).flatMap { updatedBson =>
      collection.updateOne(filter, updatedBson)
        .toFuture()
        .map { updateResult => {
          if (updateResult.wasAcknowledged() && updateResult.getModifiedCount > 0) {
            "Пользователь успешно обновлен"
          } else {
            "Обновление пользователя не выполнено"
          }
        }
        }
    }
  }

  // Delete
  def deleteUser(userId: String)(implicit ec: ExecutionContext): Future[String] = {
    val objectId = new ObjectId(userId)
    collection.deleteOne(equal("_id", objectId)).toFuture().map(_ => "Пользователь успешно удален")
  }

  // To convert MongoDB document to User
  private def docToUser(doc: Document): Option[User] = {
    Option(doc).map { d =>
      User(
        Some(d.getObjectId("_id").toHexString),
        d.getString("name"),
        d.getInteger("age"),
        d.getString("email"),
        d.getString("password"),
        d.getString("phoneNumber"),
        d.getList("booksBorrowed", classOf[String]).asScala.toList
      )
    }
  }

  // To convert UserUpdate to BsonDocument for update
  implicit class RichUserUpdate(userUpdate: UserUpdate) {
    def toDocument(userId: String)(implicit ec: ExecutionContext): Future[BsonDocument] = {
      val oldUserFuture: Future[Option[User]] = getUserById(userId)
      val updatedDocumentFuture: Future[BsonDocument] = oldUserFuture.flatMap {
        case Some(oldUser) =>
          val updatedDocument = oldUser.toDocumentForUpdate(userUpdate)
          Future.successful(updatedDocument)

        case None =>
          Future.failed(new NoSuchElementException(s"User with id $userId not found"))
      }

      updatedDocumentFuture
    }
  }

  // To convert User to BsonDocument for update
  implicit class RichUser(user: User) {
    def toDocumentForUpdate(update: UserUpdate): BsonDocument = {
      val document = BsonDocument("$set" -> BsonDocument(
        "name" -> BsonString(update.name.getOrElse(user.name)),
        "age" -> BsonInt32(update.age.getOrElse(user.age)),
        "email" -> BsonString(update.email.getOrElse(user.email)),
        "password" -> BsonString(update.password.getOrElse(user.password)),
        "phoneNumber" -> BsonString(update.phoneNumber.getOrElse(user.phoneNumber)),
        "booksBorrowed" -> BsonArray(update.booksBorrowed.getOrElse(user.booksBorrowed).map(BsonString(_)))
      ))
      document
    }
  }
}
