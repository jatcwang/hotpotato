package hotpotato

import shapeless._
import shapeless.ops.coproduct.Basis

/** Helper class to avoid needing to specify the wider coproduct we're embedding our error into */
final class Embedder[Super <: Coproduct] {
  def embed[C <: Coproduct](c: C)(implicit basis: Basis[Super, C]): Super = basis.inverse(Right(c))
}

object Embedder {
  def make[Super <: Coproduct] = new Embedder[Super]
}
