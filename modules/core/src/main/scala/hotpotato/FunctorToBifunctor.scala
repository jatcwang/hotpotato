package hotpotato

import cats.data.EitherT

trait FunctorToBifunctor[F[_, _], L, G[_]] {
  def fromBi[R](f: F[L, R]): G[R]
  def toBi[R](f: G[R]): F[L, R]
}

object FunctorToBifunctor {
  implicit def eitherTFunctorToBifunctor[FF[_], L]: FunctorToBifunctor[EitherT[FF, *, *], L, EitherT[FF, L, *]] =
    new FunctorToBifunctor[EitherT[FF, *, *], L, EitherT[FF, L, *]] {
      override def fromBi[R](f: EitherT[FF, L, R]): EitherT[FF, L, R]   = f
      override def toBi[R](f: EitherT[FF, L, R]): EitherT[FF, L, R] = f
    }

  implicit def eitherFunctorToBifunctor[L]: FunctorToBifunctor[Either[*, *], L, Either[L, *]] = new FunctorToBifunctor[Either[*, *], L, Either[L, *]]{
    override def fromBi[R](f: Either[L, R]): Either[L, R] = f
    override def toBi[R](f: Either[L, R]): Either[L, R] = f
  }
}
