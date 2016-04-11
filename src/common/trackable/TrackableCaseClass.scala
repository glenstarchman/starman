package starman.common.trackable


class TrackableCaseClass[A <: Product](self: A) extends Trackable[A](self) {


}


object TrackableCaseClass {
  def apply[T <: Product](self: T) = new TrackableCaseClass(self)
}
