package starman.common.trackable

class TrackableNumeric[T <% Number](self: T)  extends Trackable[T](self) {

  def fromInt[T](i: Int) = new TrackableNumeric(i)

  def plus(v: TrackableNumeric[T])(implicit ev: Numeric[T]) = new TrackableNumeric(
    ev.plus(value, v.value)
  )
  def +(v: TrackableNumeric[T])(implicit ev: Numeric[T]) = plus(v)
  def minus(v: TrackableNumeric[T])(implicit ev: Numeric[T]) = new TrackableNumeric(
    ev.minus(value, v.value)
  )

  def -(v: TrackableNumeric[T])(implicit ev: Numeric[T]) = minus(v)
  def unary_-(implicit ev: Numeric[T]) = new TrackableNumeric(ev.negate(value))
  def times(v: TrackableNumeric[T])(implicit ev: Numeric[T]) =
    new TrackableNumeric(ev.negate(value))

  def +=(v: TrackableNumeric[T])(implicit ev: Numeric[T]) =
    value = ev.plus(v.value, value)

  def -=(v: TrackableNumeric[T])(implicit ev: Numeric[T]) =
    value = ev.minus(value, v.value)

  def toDouble(implicit ev: Numeric[T]) = new TrackableNumeric(value.toString.toDouble)
  def toFloat(implicit ev: Numeric[T]) = new TrackableNumeric(value.toString.toFloat)
  def toInt(implicit ev: Numeric[T]) = new TrackableNumeric(value.toString.toInt)
  def toLong(implicit ev: Numeric[T]) = new TrackableNumeric(value.toString.toLong)
  def compare(v: TrackableNumeric[T])(implicit ev: Numeric[T]) = ev.compare(value, v.value)
  def equals(v: TrackableNumeric[T])(implicit ev: Numeric[T]) = value == v
  def unary_=(v: TrackableNumeric[T])(implicit ev: Numeric[T]) = value = v.value
}

object TrackableNumeric {
  def apply[T <: Number](s: T) = new TrackableNumeric(s)
}
