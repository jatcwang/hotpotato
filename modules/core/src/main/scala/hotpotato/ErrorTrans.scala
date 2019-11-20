package hotpotato

import cats.{Bifunctor, FlatMap, Functor, Monad}
import cats.data._
import cats.implicits._
import shapeless._
import shapeless.ops.coproduct.{Basis, Inject}
import zio._

/** Typeclass for any F type constructor with two types where the error (left side) can be transformed */
trait ErrorTrans[F[_, _]] {
  def transformError[L, R, LL](in: F[L, R])(func: L  => LL): F[LL, R]
  def transformErrorF[L, R, LL](in: F[L, R])(func: L => F[LL, R]): F[LL, R]
}

object ErrorTrans extends ErrorTransSyntax {
  implicit def eitherErrorTrans: ErrorTrans[Either] =
    new ErrorTrans[Either] {
      override def transformError[L, R, LL](func: Either[L, R])(
        f: L => LL,
      ): Either[LL, R] = func.leftMap(f)

      override def transformErrorF[L, R, LL](in: Either[L, R])(
        func: L => Either[LL, R],
      ): Either[LL, R] =
        in match {
          case Left(l)      => func(l)
          case r @ Right(_) => r.leftCast[LL]
        }
    }

  implicit def eitherTErrorTrans[G[_]](implicit gMonad: Monad[G]): ErrorTrans[EitherT[G, *, *]] =
    new ErrorTrans[EitherT[G, *, *]] {
      override def transformError[L, R, LL](in: EitherT[G, L, R])(
        func: L => LL,
      ): EitherT[G, LL, R] = in.leftMap(func)

      override def transformErrorF[L, R, LL](
        in: EitherT[G, L, R],
      )(func: L => EitherT[G, LL, R]): EitherT[G, LL, R] =
        EitherT(gMonad.flatMap(in.value) {
          case Left(l)      => func(l).value
          case r @ Right(_) => gMonad.pure(r.leftCast[LL])
        })
    }

  //FIXME: zio optional dep
  implicit def zioErrorTrans[Env]: ErrorTrans[ZIO[Env, *, *]] =
    new ErrorTrans[ZIO[Env, *, *]] {
      override def transformError[L, R, LL](in: ZIO[Env, L, R])(
        func: L => LL,
      ): ZIO[Env, LL, R] = in.mapError(func)

      override def transformErrorF[L, R, LL](in: ZIO[Env, L, R])(
        func: L => ZIO[Env, LL, R],
      ): ZIO[Env, LL, R] = in.catchAll(func)
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
      inject: Inject[Super, L],
    ): F[Super, R] =
      F.transformError(in) { err =>
        inject(err)
      }
  }

}
