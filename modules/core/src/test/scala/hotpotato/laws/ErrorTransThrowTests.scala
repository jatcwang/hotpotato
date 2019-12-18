package hotpotato.laws
import cats.laws.{IsEq, _}
import cats.laws.discipline._
import cats.{Bifunctor, Eq}
import hotpotato.ErrorTransThrow
import org.scalacheck.Arbitrary
import org.scalacheck.Prop._

trait ErrorTransThrowLaws[F[_, _]] extends ErrorTransLaws[F] {
  implicit def F: ErrorTransThrow[F] with Bifunctor[F]

  def extractAndThrowNothingIdentity[L, R](in: F[L, R]): IsEq[F[L, R]] =
    F.extractAndThrow(in)(e => Right(e)) <-> in
}

trait ErrorTransThrowTests[F[_, _]] extends ErrorTransTests[F] {
  def laws: ErrorTransThrowLaws[F]

  def errorTransThrow[
    A: Arbitrary: Eq,
    A2: Arbitrary: Eq,
    A3: Arbitrary: Eq,
    B: Arbitrary: Eq,
    B2: Arbitrary: Eq,
    B3: Arbitrary: Eq,
  ](
    implicit
    ArbFAB: Arbitrary[F[A, B]],
    ArbA2: Arbitrary[A  => A2],
    ArbA3: Arbitrary[A2 => A3],
    ArbB2: Arbitrary[B  => B2],
    ArbB3: Arbitrary[B2 => B3],
    EqFAB: Eq[F[A, B]],
    EqFCZ: Eq[F[A3, B3]],
    EqFA3B: Eq[F[A3, B]],
    EqFAB3: Eq[F[A, B3]],
  ) =
    new DefaultRuleSet(
      name   = "ErrorTrans",
      parent = Some(errorTrans[A, A2, A3, B, B2, B3]),
      "extractAndThrow nothing identity" -> forAll(laws.extractAndThrowNothingIdentity[A, B] _),
    )
}

object ErrorTransThrowTests {
  def apply[F[_, _]](implicit FF: ErrorTransThrow[F]): ErrorTransThrowTests[F] =
    new ErrorTransThrowTests[F] {
      override def laws: ErrorTransThrowLaws[F] = new ErrorTransThrowLaws[F] {
        override implicit def F: ErrorTransThrow[F] with Bifunctor[F] =
          new ErrorTransThrow[F] with Bifunctor[F] {
            override def bifunctor: Bifunctor[F] = FF.bifunctor

            override def pureError[L, R](l: L): F[L, R] = FF.pureError(l)

            override def flatMapError[L, R, LL](in: F[L, R])(func: L => F[LL, R]): F[LL, R] =
              FF.flatMapError(in)(func)

            override def bimap[A, B, C, D](fab: F[A, B])(f: A => C, g: B => D): F[C, D] =
              FF.bifunctor.bimap(fab)(f, g)

            override def extractAndThrow[L, E <: Throwable, R, LL](in: F[L, R])(
              extractUnhandled: L => Either[E, LL],
            ): F[LL, R] = FF.extractAndThrow(in)(extractUnhandled)
          }
      }
    }
}
