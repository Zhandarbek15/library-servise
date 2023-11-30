package domain

case class Author(
                 _id:Option[String] = None,
                 name:String,
                 age:Int,
                 email:String,
                 password: String,
                 books:List[String]
                 )

case class AuthorUpdate(
                         name: Option[String],
                         age: Option[Int],
                         email: Option[String],
                         password: Option[String],
                         books: Option[List[String]]
                       )
