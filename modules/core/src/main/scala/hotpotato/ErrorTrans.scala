package hotpotato

import cats.{Monad, MonadError}
import cats.data.EitherT
import cats.syntax.either._
import zio.ZIO

/** Typeclass for any F type constructor with two types where the error (left side) can be transformed */
trait ErrorTrans[F[_, _]] {
  def pureError[L, R](l: L): F[L, R]
  def mapError[L, R, LL](in: F[L, R])(f: L     => LL): F[LL, R] = flatMapError(in)(l => pureError(f(l)))
  def flatMapError[L, R, LL](in: F[L, R])(f: L => F[LL, R]): F[LL, R]
}

trait ErrorTransThrow[F[_, _]] extends ErrorTrans[F] {
  def extractAndThrow[L, E <: Throwable, R, LL](in: F[L, R])(
    extractUnhandled: L => Either[E, LL],
  ): F[LL, R]
}

object ErrorTrans extends ErrorTransInstances {
  def apply[F[_, _]: ErrorTrans]: ErrorTrans[F] = implicitly
}

private[hotpotato] trait ErrorTransInstances {

  implicit val eitherErrorTrans: ErrorTrans[Either] =
    new ErrorTrans[Either] {
      override def pureError[L, R](l: L): Either[L, R] = Left(l)
      override def mapError[L, R, LL](in: Either[L, R])(f: L     => LL): Either[LL, R] = in.leftMap(f)
      override def flatMapError[L, R, LL](in: Either[L, R])(f: L => Either[LL, R]): Either[LL, R] =
        in.leftFlatMap(f)
    }

  implicit def eitherTErrorTrans[M[_]: Monad]: ErrorTrans[EitherT[M, *, *]] =
    new EitherTErrorTransInstance[M]

  implicit def zioErrorTrans[Env]: ErrorTrans[ZIO[Env, *, *]] =
    new ZioErrorTransThrow[Env]
}

object ErrorTransThrow extends ErrorTransThrowInstances {
  def apply[F[_, _]: ErrorTransThrow]: ErrorTransThrow[F] = implicitly
}

private[hotpotato] trait ErrorTransThrowInstances {

  implicit def eitherTErrorTransThrow[G[_]](
    implicit G: MonadError[G, Throwable],
  ): ErrorTransThrow[EitherT[G, *, *]] =
    new EitherTErrorTransThrow[G]

  implicit def zioErrorTransThrow[Env]: ErrorTransThrow[ZIO[Env, *, *]] =
    new ZioErrorTransThrow[Env]
}

private[hotpotato] class EitherTErrorTransInstance[M[_]](implicit M: Monad[M])
    extends ErrorTrans[EitherT[M, *, *]] {

  override def pureError[L, R](l: L): EitherT[M, L, R] = EitherT.leftT[M, R].apply(l)

  override def mapError[L, R, LL](in: EitherT[M, L, R])(f: L => LL): EitherT[M, LL, R] =
    in.leftMap(f)

  override def flatMapError[L, R, LL](
    in: EitherT[M, L, R],
  )(f: L => EitherT[M, LL, R]): EitherT[M, LL, R] =
    in.leftFlatMap(f)

}

private[hotpotato] class EitherTErrorTransThrow[M[_]](implicit M: MonadError[M, Throwable])
    extends EitherTErrorTransInstance[M]
    with ErrorTransThrow[EitherT[M, *, *]] {
  override def extractAndThrow[L, E <: Throwable, R, LL](in: EitherT[M, L, R])(
    extractUnhandled: L => Either[E, LL],
  ): EitherT[M, LL, R] = flatMapError(in) { l =>
    extractUnhandled(l) match {
      case Left(throwable) => EitherT(M.raiseError(throwable))
      case Right(errors)   => EitherT.leftT[M, R].apply(errors)
    }
  }
}

private[hotpotato] class ZioErrorTransThrow[Env] extends ErrorTransThrow[ZIO[Env, *, *]] {

  override def pureError[L, R](l: L): ZIO[Env, L, R] = ZIO.fail(l)

  override def mapError[L, R, LL](in: ZIO[Env, L, R])(f: L => LL): ZIO[Env, LL, R] = in.mapError(f)

  override def flatMapError[L, R, LL](in: ZIO[Env, L, R])(
    f: L => ZIO[Env, LL, R],
  ): ZIO[Env, LL, R] = in.catchAll(f)

  override def extractAndThrow[L, E <: Throwable, R, LL](in: ZIO[Env, L, R])(
    extractUnhandled: L => Either[E, LL],
  ): ZIO[Env, LL, R] = in.catchAll { errors =>
    extractUnhandled(errors) match {
      case Left(throwable)      => ZIO.die(throwable)
      case Right(handledErrors) => ZIO.fail(handledErrors)
    }
  }
}
