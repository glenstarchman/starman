package starman.common.trackable

case class TrackableHandler[T](handlerType: String, name: String, func: (String, T*) => Any)
case class TrackableResult[T](payload: T)
