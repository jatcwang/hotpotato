package hotpotato

import cats.{Monad, MonadError}
import cats.data._
import cats.implicits._
import shapeless._
import shapeless.ops.coproduct.{Basis, Inject}
import zio._

/** Typeclass for any F type constructor with two types where the error (left side) can be transformed */
trait ErrorTrans[F[_, _]] {
  def pureError[L, R](l: L): F[L, R]
  def bimap[L, R, LL, RR](fab: F[L, R])(f: L         => LL, g: R => RR): F[LL, RR]
  def transformErrorF[L, R, LL](in: F[L, R])(func: L => F[LL, R]): F[LL, R]
  def transformError[L, R, LL](in: F[L, R])(func: L  => LL): F[LL, R] = bimap(in)(func, identity)
}

trait ErrorTransThrow[F[_, _]] extends ErrorTrans[F] {
  def extractAndThrow[L, E <: Throwable, R, LL](in: F[L, R])(
    extractUnhandled: L => Either[E, LL],
  ): F[LL, R]
}

object ErrorTrans extends ErrorTransSyntax with ErrorTransLowerInstances {

  implicit val eitherErrorTrans: ErrorTrans[Either] =
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

  implicit def eitherTErrorTransThrow[G[_]](
    implicit G: MonadError[G, Throwable],
  ): ErrorTransThrow[EitherT[G, *, *]] =
    new EitherTErrorTransThrow[G]

  //FIXME: zio optional dep
  implicit def zioErrorTrans[Env]: ErrorTransThrow[ZIO[Env, *, *]] =
    new ZioErrorTransThrow[Env]

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

private[hotpotato] trait ErrorTransLowerInstances {

  implicit def eitherTErrorTrans[M[_]](implicit M: Monad[M]): ErrorTrans[EitherT[M, *, *]] =
    new EitherTErrorTransInstance[M]

}

private[hotpotato] class EitherTErrorTransInstance[M[_]](implicit M: Monad[M])
    extends ErrorTrans[EitherT[M, *, *]] {

  override def pureError[L, R](l: L): EitherT[M, L, R] = EitherT.leftT[M, R].apply(l)

  override def transformError[L, R, LL](in: EitherT[M, L, R])(
    func: L => LL,
  ): EitherT[M, LL, R] = in.leftMap(func)

  override def transformErrorF[L, R, LL](
    in: EitherT[M, L, R],
  )(func: L => EitherT[M, LL, R]): EitherT[M, LL, R] =
    EitherT(M.flatMap(in.value) {
      case Left(l)      => func(l).value
      case r @ Right(_) => M.pure(r.leftCast[LL])
    })

  override def bimap[A, B, C, D](in: EitherT[M, A, B])(f: A => C, g: B => D): EitherT[M, C, D] =
    in.bimap(f, g)

}

private[hotpotato] class EitherTErrorTransThrow[M[_]](implicit M: MonadError[M, Throwable])
    extends EitherTErrorTransInstance[M]
    with ErrorTransThrow[EitherT[M, *, *]] {
  override def extractAndThrow[L, E <: Throwable, R, LL](in: EitherT[M, L, R])(
    extractUnhandled: L => Either[E, LL],
  ): EitherT[M, LL, R] = transformErrorF(in) { l =>
    extractUnhandled(l) match {
      case Left(throwable) => EitherT(M.raiseError(throwable))
      case Right(errors)   => EitherT.leftT[M, R].apply(errors)
    }
  }
}

private[hotpotato] class ZioErrorTransThrow[Env] extends ErrorTransThrow[ZIO[Env, *, *]] {
  override def transformError[L, R, LL](in: ZIO[Env, L, R])(
    func: L => LL,
  ): ZIO[Env, LL, R] = in.mapError(func)

  override def transformErrorF[L, R, LL](in: ZIO[Env, L, R])(
    func: L => ZIO[Env, LL, R],
  ): ZIO[Env, LL, R] = in.catchAll(func)

  override def bimap[A, B, C, D](in: ZIO[Env, A, B])(f: A => C, g: B => D): ZIO[Env, C, D] =
    in.bimap(f, g)

  override def pureError[L, R](l: L): ZIO[Env, L, R] = ZIO.fail(l)

  override def extractAndThrow[L, E <: Throwable, R, LL](in: ZIO[Env, L, R])(
    extractUnhandled: L => Either[E, LL],
  ): ZIO[Env, LL, R] = in.catchAll { errors =>
    extractUnhandled(errors) match {
      case Left(throwable)      => ZIO.die(throwable)
      case Right(handledErrors) => ZIO.fail(handledErrors)
    }
  }
}
