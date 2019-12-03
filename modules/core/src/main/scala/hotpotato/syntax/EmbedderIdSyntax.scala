package hotpotato.syntax

import hotpotato.Embedder
import shapeless.Coproduct
import shapeless.ops.coproduct.Inject

trait EmbedderIdSyntax {
  implicit class EmbedderIdOps[A](a: A) {
    def embed[C <: Coproduct](implicit embedder: Embedder[C], inject: Inject[C, A]): C =
      // The embedder is used for type-inference purpose
      inject.apply(a)

    /** Embed a type into a coproduct, not relying on an implicit Embedder to be in scope to guide type inference.
      *  Use this when you want to specify the coproduct type explicitly, or when the coproduct type is unambiguous
      * */
    def embedInto[C <: Coproduct](implicit inject: Inject[C, A]): C =
      inject.apply(a)
  }
}
