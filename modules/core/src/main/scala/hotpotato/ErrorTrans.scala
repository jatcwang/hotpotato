package hotpotato

import cats.{Bifunctor, Functor}
import cats.data._
import cats.implicits._
import shapeless._
import shapeless.ops.coproduct.{Basis, Inject}
import zio._

/** Typeclass for any F type constructor with two types where the error (left side) can be transformed */
trait ErrorTrans[F[_, _], L, R] {
  def transformError[LL](fea: F[L, R])(f: L => LL): F[LL, R]
}

object ErrorTrans extends ErrorTransSyntax {
  implicit def eitherErrorTrans[E, A]: ErrorTrans[Either, E, A] =
    new ErrorTrans[Either, E, A] {
      override def transformError[EE](fea: Either[E, A])(
        f: E => EE,
      ): Either[EE, A] = fea.leftMap(f)
    }

  implicit def eitherTErrorTrans[G[_]: Functor, E, A]: ErrorTrans[EitherT[G, *, *], E, A] =
    new ErrorTrans[EitherT[G, *, *], E, A] {
      override def transformError[EE](
        fea: EitherT[G, E, A],
      )(f: E => EE): EitherT[G, EE, A] =
        fea.leftMap(f)
    }

  //FIXME: zio optional dep
  implicit def zioErrorTrans[Env, L, R]: ErrorTrans[ZIO[Env, *, *], L, R] =
    new ErrorTrans[ZIO[Env, *, *], L, R] {
      override def transformError[LL](fea: ZIO[Env, L, R])(
        f: L => LL,
      ): ZIO[Env, LL, R] = fea.mapError(f)
    }

  implicit class ErrorTransCoprodEmbedOps[F[_, _], L <: Coproduct, R](
    val in: F[L, R],
  ) extends AnyVal {

    /** Embed a coproduct into a larger (or equivalent) coproduct */
    def embedError[Super <: Coproduct](
      implicit F: ErrorTrans[F, L, R],
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
      implicit F: ErrorTrans[F, L, R],
      embedder: Embedder[Super], // Used for type inference only
      inject: Inject[Super, L]
    ): F[Super, R] =
      F.transformError(in) { err =>
        inject(err)
      }
  }

}

trait ErrorTransLowerPrioInstances {
  implicit def bifunctorErrorTrans[F[_, _], L, R](implicit bi: Bifunctor[F]): ErrorTrans[F, L, R] = new ErrorTrans[F, L, R] {
    override def transformError[LL](flr: F[L, R])(f: L => LL): F[LL, R] = bi.leftMap(flr)(f)
  }
}
