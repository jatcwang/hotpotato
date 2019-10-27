package hotpotato.coproduct
import shapeless.ops.coproduct._
import shapeless._

trait SameElem[C <: Coproduct, CC <: Coproduct] {
  def apply(in: CC): C
}

object SameElem extends SameElemLowPriority {

  implicit def sameType[C <: Coproduct, CC <: Coproduct](
    implicit ev: CC =:= C,
  ): SameElem[C, CC] = new SameElem[C, CC] {
    override def apply(in: CC): C = ev(in)
  }

  implicit val cnil: SameElem[CNil, CNil] = new SameElem[CNil, CNil] {
    override def apply(t: CNil): CNil = t
  }
}

trait SameElemLowPriority extends SameElemLowestPriority {

  implicit def dupeHeadType[H, T <: Coproduct, RemoveOut <: Coproduct, CC <: Coproduct](
    implicit remove: Remove.Aux[CC, H, RemoveOut],
    injectHIntoCC: Inject[RemoveOut, H],
    tailSameElem: SameElem[H :+: T, RemoveOut],
  ): SameElem[H :+: T, CC] = new SameElem[H :+: T, CC] {
    override def apply(cc: CC): H :+: T =
      remove.apply(cc) match {
        case Left(h)          => tailSameElem(injectHIntoCC(h))
        case Right(removeOut) => tailSameElem(removeOut)
      }
  }

}

trait SameElemLowestPriority {
  implicit def uniquHeadType[H, T <: Coproduct, RemoveOut <: Coproduct, CC <: Coproduct](
    implicit remove: Remove.Aux[CC, H, RemoveOut],
    tailSameElem: SameElem[T, RemoveOut],
    notEq: (H :+: T) =:!= CC,
  ): SameElem[H :+: T, CC] = new SameElem[H :+: T, CC] {
    override def apply(cc: CC): H :+: T =
      remove.apply(cc) match {
        case Left(h)          => Inl(h)
        case Right(removeOut) => Inr(tailSameElem.apply(removeOut))
      }
  }

}
