package domain

case class User(_id:Option[String] = None,
                name:String,
                age:Int,
                email:String,
                password: String,
                phoneNumber:String,
                booksBorrowed:List[String]
               )

case class UserUpdate(name:Option[String],
                      age:Option[Int],
                      email:Option[String],
                      password:Option[String],
                      phoneNumber:Option[String],
                      booksBorrowed:Option[List[String]]
               )