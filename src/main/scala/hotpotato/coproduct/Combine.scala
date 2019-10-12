package hotpotato.coproduct

import shapeless._

/** Typeclass to combine two implicits */
trait Combine[C <: Coproduct, CC <: Coproduct] extends DepFn2[C, CC] {
  override type Out <: Coproduct
}

object Combine {
//  implicit def joinCoproduct[]
}


