package starman.common.trackable

class TrackableCollection[A, T[A] <: Traversable[A]](self: T[A])
    extends Trackable[T[A]](self) {

  private[this] def _new(v: Traversable[A]) = new TrackableCollection(v.asInstanceOf[T[A]])
  private[this] def _set(v: Traversable[A]) = value = v.asInstanceOf[T[A]]

  def foreach[B](f: (A) => B) = value.foreach(x => f(x))
  def map[B](f: (A) => B) = new TrackableCollection(value.map(x => f(x)).asInstanceOf[T[B]])
  def flatMap[B](f: (A) => T[B]) = new TrackableCollection(value.flatMap(x => f(x)))
  def filter(f: (A) => Boolean) = _new(value.filter(f))
  def filterNot(f: (A) => Boolean) = _new(value.filterNot(f))
  def find(f: (A) => Boolean) = new Trackable(value.find(f).asInstanceOf[Option[A]])
  def toList = new TrackableCollection(value.toList.asInstanceOf[List[A]])
  def filterInplace(f: (A) => Boolean) = _set(value.filter(f))
  def filterNotInplace(f: (A) => Boolean) = _set(value.filterNot(f))
  def mapInplace(f: (A) => A) = _set(value.map(x => f(x)))
  def asCollection() = value.asInstanceOf[T[A]]

}

object TrackableCollection {
  def apply[A, T[A] <: Traversable[A]](self: T[A]) = new TrackableCollection(self)
  def unapply[A, T[A] <: Traversable[A]](self: T[A]) = Option(self)

}
