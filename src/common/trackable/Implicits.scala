package starman.common.trackable

object Implicits {

  implicit def rv2val[T](rv: Trackable[T]) = rv.value

  //allow any value to use Trackable methods...this is somewhat broken
  implicit class TrackableWrapper[T](self: T) extends Trackable[T](self)
  implicit class TrackableNumericValWrapper[T <% Number](self: T) extends TrackableNumeric[T](self)

}
