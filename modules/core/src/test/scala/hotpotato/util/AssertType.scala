package hotpotato.util

// Invariant container used to 'assert' the type of some expression to be of type A
final case class AssertType[A](unpact: A) {
  def is[B](implicit ev: A =:= B): B = ev(unpact)
}
