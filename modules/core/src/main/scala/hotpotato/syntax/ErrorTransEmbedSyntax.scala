package hotpotato.syntax

import hotpotato.{Embedder, ErrorTrans}
import shapeless.Coproduct
import shapeless.ops.coproduct.{Basis, Inject}

private[hotpotato] trait ErrorTransEmbedSyntax {

  implicit class ErrorTransCoprodEmbedOps[F[_, _], L <: Coproduct, R](
    in: F[L, R],
  ) {

    /** Embed a coproduct into a larger (or equivalent) coproduct */
    def embedError[Super <: Coproduct](
      implicit F: ErrorTrans[F],
      embedder: Embedder[Super],
      basis: Basis[Super, L],
    ): F[Super, R] =
      F.mapError(in) { err =>
        embedder.embed[L](err)(basis)
      }

  }

  implicit class ErrorTransIdLeftOps[F[_, _], L, R](in: F[L, R]) {

    /** Embed a single non-coproduct type into the coproduct */
    def embedError[Super <: Coproduct](
      implicit F: ErrorTrans[F],
      embedder: Embedder[Super], // Used for type inference only
      inject: Inject[Super, L],
    ): F[Super, R] =
      F.mapError(in) { err =>
        inject(err)
      }
  }

}
