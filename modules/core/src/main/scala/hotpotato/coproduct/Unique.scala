package hotpotato.coproduct

import shapeless._
import shapeless.ops.coproduct.Inject

/** Given a coproduct, yield a coproduct with the duplicate types removed */
trait Unique[C <: Coproduct] extends DepFn1[C] {
  type Out <: Coproduct
}

object Unique extends LowPriorityUnique {
  type Aux[C <: Coproduct, Out0 <: Coproduct] = Unique[C] { type Out = Out0 }

  def apply[C <: Coproduct](implicit unC: Unique[C]): Aux[C, unC.Out] = unC

  implicit val uniqueCNil: Aux[CNil, CNil] = new Unique[CNil] {
    type Out = CNil
    def apply(c: CNil): CNil = c
  }

  implicit def uniqueCCons1[L, R <: Coproduct](
    implicit
    inj: Inject[R, L],
    unR: Unique[R],
  ): Aux[L :+: R, unR.Out] = new Unique[L :+: R] {
    type Out = unR.Out

    def apply(c: L :+: R): unR.Out =
      unR(c match {
        case Inl(l) => inj(l)
        case Inr(r) => r
      })
  }
}

class LowPriorityUnique {
  implicit def uniqueCCons0[L, R <: Coproduct](
    implicit
    unR: Unique[R],
  ): Unique[L :+: R] { type Out = L :+: unR.Out } = new Unique[L :+: R] {
    type Out = L :+: unR.Out

    def apply(c: L :+: R): L :+: unR.Out = c match {
      case Inl(l) => Inl(l)
      case Inr(r) => Inr(unR(r))
    }
  }
}
