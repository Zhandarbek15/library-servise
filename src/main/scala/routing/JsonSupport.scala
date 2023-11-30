package routing

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json._
import domain._

import java.text.SimpleDateFormat
import java.util.Date

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  import spray.json._

  implicit object DateJsonFormat extends RootJsonFormat[Date] {
    private val pattern = "yyyy-MM-dd"
    private val formatter = new SimpleDateFormat(pattern)

    def write(obj: Date): JsValue = JsString(formatter.format(obj))

    def read(json: JsValue): Date = json match {
      case JsString(dateString) => formatter.parse(dateString)
      case _ => throw DeserializationException("Неправильные данные для типа Date. Правильный формат: ГГГГ-ММ-ДД!")
    }
  }

  implicit object LanguageFormat extends RootJsonFormat[Language.Language] {
    def write(obj: Language.Language): JsValue = JsString(obj.toString)

    def read(json: JsValue): Language.Language = json match {
      case JsString(value) => Language.withName(value)
      case _ => throw DeserializationException("Неправильные данные для типа Language. " +
        "Допустимые значения: En, Ru, Kz, Fr")
    }
  }

  implicit object SubjectFormat extends RootJsonFormat[Subject.Subject] {
    def write(obj: Subject.Subject): JsValue = JsString(obj.toString)

    def read(json: JsValue): Subject.Subject = json match {
      case JsString(value) => Subject.withName(value)
      case _ => throw DeserializationException("Неправильные данные для типа Subject. " +
        "Допустимые значения: Math, Biology, English, Physics, Chemistry, Geography")
    }
  }

  implicit object GenreFormat extends RootJsonFormat[Genre.Genre] {
    def write(obj: Genre.Genre): JsValue = JsString(obj.toString)

    def read(json: JsValue): Genre.Genre = json match {
      case JsString(value) => Genre.withName(value)
      case _ => throw DeserializationException("Неправильные данные для типа Genre. " +
        "Допустимые значения: Ballad, Myth, Novella, Tale, Story, Novel, FairyTale, Epic")
    }
  }

  implicit object TypeOfWorkFormat extends RootJsonFormat[TypeOfWork.TypeOfWork] {
    def write(obj: TypeOfWork.TypeOfWork): JsValue = JsString(obj.toString)

    def read(json: JsValue): TypeOfWork.TypeOfWork = json match {
      case JsString(value) => TypeOfWork.withName(value)
      case _ => throw DeserializationException(
        "Неправильные данные для типа TypeOfWork. " +
          "Допустимые значения: ResearchArticle, ReviewPaper, CaseStudy, Thesis, " +
          "Dissertation, ConferencePaper, ScientificReport, LiteratureReview, ExperimentalStudy, " +
          "Meta_analysis")
    }
  }

  implicit val userFormat: RootJsonFormat[User] = jsonFormat7(User)
  implicit val userListFormat: RootJsonFormat[List[User]] = listFormat(userFormat)
  implicit val userUpdateFormat: RootJsonFormat[UserUpdate] = jsonFormat6(UserUpdate)

  implicit val authorFormat: RootJsonFormat[Author] = jsonFormat6(Author)
  implicit val authorListFormat: RootJsonFormat[List[Author]] = listFormat(authorFormat)
  implicit val authorUpdateFormat: RootJsonFormat[AuthorUpdate] = jsonFormat5(AuthorUpdate)

  implicit val bookFormat: RootJsonFormat[Book] = new RootJsonFormat[Book] {
    override def write(obj: Book): JsValue = obj match {
      case textBook: TextBook => textBookFormat.write(textBook)
      case fictionBook: FictionBook => fictionBookFormat.write(fictionBook)
      case scientificBook: ScientificBook => scientificBookFormat.write(scientificBook)
    }

    override def read(json: JsValue): Book = {
      val bookType = json.asJsObject.fields("bookType").convertTo[String]
      val book = bookType match {
        case "TextBook" => textBookFormat.read(json)
        case "FictionBook" => fictionBookFormat.read(json)
        case "ScientificBook" => scientificBookFormat.read(json)
        case _ => throw DeserializationException("Не правильный тип книги! " +
          "Допустимые типы: TextBook, FictionBook, ScientificBook")
      }
      book.asInstanceOf[Book]
    }
  }

  implicit val bookListFormat: RootJsonFormat[List[Book]] = listFormat(bookFormat)
  implicit val textBookFormat: RootJsonFormat[TextBook] = jsonFormat11(TextBook)
  implicit val fictionBookFormat: RootJsonFormat[FictionBook] = jsonFormat12(FictionBook)
  implicit val scientificBookFormat: RootJsonFormat[ScientificBook] = jsonFormat12(ScientificBook)
  implicit val bookUpdateFormat: RootJsonFormat[BookUpdate] = jsonFormat13(BookUpdate)

}
