package hotpotato.syntax

import hotpotato.Embedder
import shapeless.Coproduct
import shapeless.ops.coproduct.Inject

object embedder extends EmbedderSyntax

trait EmbedderSyntax {
  implicit class EmbedderIdOps[A](a: A) {
    def embed[C <: Coproduct](implicit embedder: Embedder[C], inject: Inject[C, A]): C = inject.apply(a)
  }
}
