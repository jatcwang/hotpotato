package hotpotato

import shapeless._
import shapeless.ops.coproduct.Basis

trait Subset[Sub <: Coproduct, Super <: Coproduct] extends Serializable {
  def embedIn(sub: Sub): Super
}

object Subset {

  type Aux[Super <: Coproduct, Sub <: Coproduct, Rest0 <: Coproduct] =
    Basis[Super, Sub] { type Rest = Rest0 }

  implicit def subsetFromBasis[Sub <: Coproduct, Super <: Coproduct](implicit basis: Basis[Super, Sub]): Subset[Sub, Super] = new Subset[Sub, Super]{
    override def embedIn(sub: Sub): Super = basis.inverse(Right(sub))
  }
}
