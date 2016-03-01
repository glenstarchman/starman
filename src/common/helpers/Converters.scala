/*
 * Copyright (c) 2015. Starman, Inc All Rights Reserved
 */

package starman.common.converters

import scala.reflect._
import scala.reflect.runtime.universe._
import scala.util.parsing.json.{JSON, JSONObject, JSONArray}
import starman.common.helpers.Text._
import starman.common.Types._


/* all kinds of conversions between types, some used in implicits */
object Mapper {

  val excludedFields = List("_isPersisted")

  /* Map  -> case class */
  def ccFromMap[T: TypeTag : ClassTag](m: Map[String, Any]) = {
    val rm = runtimeMirror(classTag[T].runtimeClass.getClassLoader)
    val classTest = typeOf[T].typeSymbol.asClass
    val classMirror = rm.reflectClass(classTest)
    val constructor = typeOf[T].decl(termNames.CONSTRUCTOR).asMethod
    val constructorMirror = classMirror.reflectConstructor(constructor)

    val constructorArgs = constructor.paramLists.flatten.map((param: Symbol) => {
      val paramName = param.name.toString
      if(param.typeSignature <:< typeOf[Option[Any]]) {
        m.get(paramName)
      } else {
        try {
          m.get(paramName).getOrElse(throw new IllegalArgumentException("Map is missing required parameter named " + paramName))
        } catch {
          case e: Exception => None
        }
      }
    }).map(x => null)
    constructorMirror(constructorArgs: _*)
  }

  /* handles recursion */
  def ccToMap[T <: Product](cc: T, snakify: Boolean=false): MapAny = {
    val values = cc.productIterator
    val fields = cc.getClass.getDeclaredFields.filter(f => !excludedFields.contains(f.getName))
    fields.map { c =>
      val name = if (snakify) {
        underscore(c.getName)
      } else {
        c.getName
      }
      name -> (values.next() match {
        case p: Product if p.productArity > 0 => ccToMap(p)
        case x => x
      })
    }.toMap 

  }

  def ccToJson(cc: Product): JSONObject = new JSONObject(ccToMap(cc, false)) 

  def jsonToCC(j: JSONObject) = {
    val data = j.obj.map(x => 
      (camelize(x._1) -> x._2)
    )
    ccFromMap(data)
  }

  def jsonStringToMap(s: String): MapAny = {
    JSON.parseFull(s) match {
      case Some(x) => x.asInstanceOf[MapAny]
      case _ => Map[String, Any]()
    }
 }

  def jsonStringToCC[T: TypeTag: ClassTag](s: String) = {
    val obj = jsonStringToMap(s).map(x =>
      x._1.asInstanceOf[String] -> x._2
    )
    ccFromMap[T](obj)
  }

  def jsonToMap(json: JSONObject): MapAny = json.obj

  /* Map conversions */
  //simple by kv grouping
  def toMultiMap(p: Map[_,_]) = p.groupBy(_._1).mapValues(_.map(_._2)) 

  /* all implicits */
  implicit def cc2json(cc: Product): JSONObject = ccToJson(cc)
  //implicit def json2cc(json: JSONObject): Product = ccFromMap(jsonToMap(json))
  implicit def json2map(json: JSONObject): MapAny = jsonToMap(json)
}

/* a trait to add some conversion functions
   you should probably not user Mapper by itself */

trait Convertable extends Product {
  def extraMap(): MapAny = Map.empty 
  def asMap(underscore: Boolean = false) = Mapper.ccToMap(this, underscore) ++ extraMap
  def asMap(): MapAny = asMap(false) ++ extraMap
  def asJson = Mapper.ccToJson(this)
  def fromMap(m: MapAny) = Mapper.ccFromMap(m)
  def fromJson(j: JSONObject) = Mapper.jsonToCC(j)
  /* a JSON string */
  def fromJson(j: String) = Mapper.jsonStringToCC(j)
}


//as a list of JSONObject
object ListConverter {
  def asJson(cc: List[Product]): JSONArray  = JSONArray(cc.map(Mapper.ccToJson))
  def asMap[T <: Product](cc: List[Product], underscore: Boolean = false): ListMap  = {
    try {
      cc.map(c => Mapper.ccToMap(c, underscore)).toList
    } catch {
      case e: Exception => cc.asInstanceOf[ListMap]
    }
  }
}

//keyed version of the above
object ObjectConverter {
  def asJson(cc: List[Product]): JSONObject  = JSONObject(cc.map(m => {
    val map = Mapper.ccToMap(m)
    map("id").toString -> JSONObject(map)
  }).toMap)

  def asMap[T <: Product](cc: List[Product], underscore: Boolean = false): Map[String, MapAny]  = cc.map(m => {
    val map = Mapper.ccToMap(m, underscore)
    map("id").toString -> map
  }).toMap
}
