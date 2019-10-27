package hotpotato.coproduct

import shapeless._
import shapeless.ops.coproduct._

/** Typeclass to combine two implicits */
trait Combine[C <: Coproduct, CC <: Coproduct] {
  type Out <: Coproduct

  def right(l: C): Out
  def left(r: CC): Out
}

object Combine {
  type Aux[C <: Coproduct, CC <: Coproduct, U <: Coproduct] = Combine[C, CC] {
    type Out = U
  }

  implicit def joinCoproduct[
    C <: Coproduct,
    CC <: Coproduct,
    CCombine <: Coproduct,
    U <: Coproduct,
  ](
    implicit extendBy: ExtendBy.Aux[C, CC, CCombine],
    unique: Unique.Aux[CCombine, U],
  ): Combine.Aux[C, CC, U] = new Combine[C, CC] {
    override type Out = U

    def right(l: C): Out = unique.apply(extendBy.right(l))
    def left(r: CC): Out = unique.apply(extendBy.left(r))
  }
}
