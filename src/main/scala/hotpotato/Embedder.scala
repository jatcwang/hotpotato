package hotpotato

import shapeless._
import shapeless.ops.coproduct.Basis

/*
TODOO: Make something like:
embedderFor[E1 :+: E2 :+: E3 :+: CNil] { implicit em =>
}
Which under the hood reuse instances of the Embedder
 */
/** Helper class to avoid needing to specify the wider coproduct we're embedding our error into */
final class Embedder[Super <: Coproduct] {
  def embed[C <: Coproduct](c: C)(implicit basis: Basis[Super, C]): Super = basis.inverse(Right(c))
}

object Embedder {
  implicit def make[Super <: Coproduct] = new Embedder[Super]
}
