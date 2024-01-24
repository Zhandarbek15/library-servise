package object repositories {
  def getFields(clazz: Class[_], args:String*): Set[String] = {
    val allFields = clazz.getDeclaredFields.map(_.getName).toSet
    allFields.diff(args.toSet)
  }
}
