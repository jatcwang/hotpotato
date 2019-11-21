package hotpotato

import cats.{Bifunctor, FlatMap, Functor, Monad}
import cats.data._
import cats.implicits._
import shapeless._
import shapeless.ops.coproduct.{Basis, Inject}
import zio._

/** Typeclass for any F type constructor with two types where the error (left side) can be transformed */
trait ErrorTrans[F[_, _]]{
  def pureError[L, R](l: L): F[L, R]
  def bimap[L, R, LL, RR](fab: F[L, R])(f: L => LL, g: R => RR): F[LL, RR]
  def transformErrorF[L, R, LL](in: F[L, R])(func: L => F[LL, R]): F[LL, R]
  def transformError[L, R, LL](in: F[L, R])(func: L  => LL): F[LL, R] = bimap(in)(func, identity)
}

object ErrorTrans extends ErrorTransSyntax {
  implicit def eitherErrorTrans: ErrorTrans[Either] =
    new ErrorTrans[Either] {
      override def transformErrorF[L, R, LL](in: Either[L, R])(
        func: L => Either[LL, R],
      ): Either[LL, R] =
        in match {
          case Left(l)      => func(l)
          case r @ Right(_) => r.leftCast[LL]
        }

      override def bimap[A, B, C, D](in: Either[A, B])(f: A => C, g: B => D): Either[C, D] =
        cats.instances.either.catsStdBitraverseForEither.bimap(in)(f, g)

      override def pureError[L, R](l: L): Either[L, R] = Left(l)
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

      override def bimap[A, B, C, D](in: EitherT[G, A, B])(f: A => C, g: B => D): EitherT[G, C, D] =
        in.bimap(f, g)

      override def pureError[L, R](l: L): EitherT[G, L, R] = EitherT.leftT[G, R].apply(l)
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

      override def bimap[A, B, C, D](in: ZIO[Env, A, B])(f: A => C, g: B => D): ZIO[Env, C, D] =
        in.bimap(f, g)

      override def pureError[L, R](l: L): ZIO[Env, L, R] = ZIO.fail(l)
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
