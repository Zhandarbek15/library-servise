package domain

import domain.Genre.Genre
import domain.Language.Language
import domain.Subject.Subject
import domain.TypeOfWork.TypeOfWork

import java.util.Date

object Language extends Enumeration {
  type Language = Value
  val En, Ru, Kz, Fr = Value
}

object Subject extends Enumeration {
  type Subject = Value
  val Math, Biology, English, Physics, Chemistry, Geography = Value
}

object Genre extends Enumeration {
  type Genre = Value
  val Ballad, Myth, Novella, Tale, Story, Novel, FairyTale, Epic = Value
}

object TypeOfWork extends Enumeration {
  type TypeOfWork = Value
  val ResearchArticle, ReviewPaper, CaseStudy, Thesis, Dissertation,
  ConferencePaper, ScientificReport, LiteratureReview, ExperimentalStudy, Meta_analysis = Value
}

abstract class Book (
                      _id:Option[String] = None,
                      name: String,
                      author: String,
                      dateRelease: Date,
                      dateOfAcceptance: Date,
                      numberOfPages: Int,
                      price: Int,
                      keywords: List[String],
                      language: Language,
                      bookType: String // Добавленное поле
                    )

case class TextBook(
                     _id:Option[String] = None,
                     name: String,
                     author: String,
                     dateRelease: Date,
                     dateOfAcceptance: Date,
                     numberOfPages: Int,
                     price: Int,
                     keywords: List[String],
                     language: Language,
                     subject: Subject,
                     bookType: String = "TextBook" // Значение по умолчанию
                   ) extends Book(_id, name, author, dateRelease, dateOfAcceptance, numberOfPages, price, keywords, language, bookType)

case class FictionBook(
                        _id:Option[String] = None,
                        name: String,
                        author: String,
                        dateRelease: Date,
                        dateOfAcceptance: Date,
                        numberOfPages: Int,
                        price: Int,
                        keywords: List[String],
                        language: Language,
                        averageRating: Int,
                        genre: Genre,
                        bookType: String = "FictionBook" // Значение по умолчанию
                      ) extends Book(_id ,name, author, dateRelease, dateOfAcceptance, numberOfPages, price, keywords, language, bookType)

case class ScientificBook(
                           _id:Option[String] = None,
                           name: String,
                           author: String,
                           dateRelease: Date,
                           dateOfAcceptance: Date,
                           numberOfPages: Int,
                           price: Int,
                           keywords: List[String],
                           language: Language,
                           subject: Subject,
                           typeOfWork: TypeOfWork,
                           bookType: String = "ScientificBook" // Значение по умолчанию
                         ) extends Book(_id ,name, author, dateRelease, dateOfAcceptance, numberOfPages, price, keywords, language, bookType)

case class BookUpdate(
                       name: Option[String] = None,
                       author: Option[String] = None,
                       dateRelease: Option[java.util.Date] = None,
                       dateOfAcceptance: Option[java.util.Date] = None,
                       numberOfPages: Option[Int] = None,
                       price: Option[Int] = None,
                       keywords: Option[List[String]] = None,
                       language: Option[Language] = None,
                       subject: Option[Subject] = None,
                       averageRating: Option[Int] = None,
                       genre: Option[Genre] = None,
                       typeOfWork: Option[TypeOfWork] = None,
                       bookType: Option[String] = None
                     )