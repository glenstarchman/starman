package starman.common.trackable

import starman.common.helpers.RandomStringGenerator

class Trackable[T](private[this] var self: T) {

  private[this] var oldValue:T = self
  private[this] var handlers: List[TrackableHandler[T]] = List.empty
  def value: T = self

  //fire event on value change
  def value_=(x: T) = {
    oldValue = self
    if (x != oldValue)	{
      self = x
      fireChange(x, oldValue)
    }
  }

  def fireChange(newVal: T, oldVal: T): Unit =
    handlers
      .filter(h => h.handlerType == "change")
      .foreach(handler => handler.func("change", newVal, oldVal))


  /*def fire(handlerType: String, message: Any) =
    handlers
      .filter(h => h.handlerType == handlerType)
      .foreach(handler => handler.func(message))
   */

  //private[this] def fireAll: Unit = fireAll(self, oldValue)
  private[this] def handlerExists(handlerType: String, name: String) = {
    val exists = handlers.find(x => x.name == name && x.handlerType == handlerType)
    exists match {
      case Some(x) => true
      case _ => false
    }
  }

  def addHandler(handlerType: String, name: String, func: (String, T* ) => Any) = {
    if (handlerExists(handlerType, name)) {
      throw(new Exception("Handler exists"))
    }

    handlers ++= List(TrackableHandler(handlerType, name, func))
    name
  }


  private[this] def findHandler(name: String) = handlers.find(x => x.name == name)
  def removeHandler(handlerType: String, name: String) =
    handlers = handlers.filter(x => (x.name != name && x.handlerType != handlerType))

  //some sample handlers
  val testHandler = (newVal:T, oldVal:T) => println(newVal, oldVal)

}

object Trackable {
  def apply[T](self: T) = new Trackable(self)
  def unapply[T](self: T) = Option(self)
}
