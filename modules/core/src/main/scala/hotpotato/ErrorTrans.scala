package hotpotato

import cats.{Bifunctor, Functor}
import cats.data._
import cats.implicits._
import shapeless._
import shapeless.ops.coproduct.{Basis, Inject}
import zio._

/** Typeclass for any F type constructor with two types where the error (left side) can be transformed */
trait ErrorTrans[F[_, _]] {
  def transformError[L, R, LL](fea: F[L, R])(f: L => LL): F[LL, R]
}

object ErrorTrans extends ErrorTransSyntax {
  implicit def eitherErrorTrans: ErrorTrans[Either] =
    new ErrorTrans[Either] {
      override def transformError[L, R, LL](fea: Either[L, R])(
        f: L => LL,
      ): Either[LL, R] = fea.leftMap(f)
    }

  implicit def eitherTErrorTrans[G[_]: Functor]: ErrorTrans[EitherT[G, *, *]] =
    new ErrorTrans[EitherT[G, *, *]] {
      override def transformError[L, R, LL](fea: EitherT[G, L, R])(f: L => LL): EitherT[G, LL, R] = fea.leftMap(f)
    }

  //FIXME: zio optional dep
  implicit def zioErrorTrans[Env, L, R]: ErrorTrans[ZIO[Env, *, *]] =
    new ErrorTrans[ZIO[Env, *, *]] {
      override def transformError[L, R, LL](fea: ZIO[Env, L, R])(
        f: L => LL,
      ): ZIO[Env, LL, R] = fea.mapError(f)
    }

  implicit class ErrorTransCoprodEmbedOps[F[_, _], L <: Coproduct, R](
    val in: F[L, R],
  ) extends AnyVal {

    /** Embed a coproduct into a larger (or equivalent) coproduct */
    def embedError[Super <: Coproduct](
      implicit F: ErrorTrans[F],
      embedder: Embedder[Super],
      basis: Basis[Super, L],
    ): F[Super, R] =
      F.transformError(in) { err =>
        embedder.embed[L](err)(basis)
      }

  }

  implicit class ErrorTransIdLeftOps[F[_, _], L, R](val in: F[L, R]) extends AnyVal {

    /** Embed a single non-coproduct type into the coproduct */
    def embedError[Super <: Coproduct](
      implicit F: ErrorTrans[F],
      embedder: Embedder[Super], // Used for type inference only
      inject: Inject[Super, L]
    ): F[Super, R] =
      F.transformError(in) { err =>
        inject(err)
      }
  }

}

trait ErrorTransLowerPrioInstances {
  implicit def bifunctorErrorTrans[F[_, _]](implicit bi: Bifunctor[F]): ErrorTrans[F] = new ErrorTrans[F] {
    override def transformError[L, R, LL](flr: F[L, R])(f: L => LL): F[LL, R] = bi.leftMap(flr)(f)
  }
}
