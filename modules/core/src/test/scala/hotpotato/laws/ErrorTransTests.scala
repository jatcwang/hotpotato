package hotpotato.laws
import cats.{Bifunctor, Eq}
import cats.laws.{BifunctorLaws, IsEq}
import cats.laws.discipline.{BifunctorTests, FunctorTests}
import hotpotato.ErrorTrans
import org.scalacheck.Arbitrary
import org.scalacheck.Prop._
import cats.laws._
import cats.laws.discipline._

trait ErrorTransLaws[F[_, _]] extends BifunctorLaws[F] {
  implicit def F: ErrorTrans[F] with Bifunctor[F]

  def transformErrorFpureErrorEquiv[L, R](in: F[L, R]): IsEq[F[L, R]] =
    F.flatMapError(in)(e => F.pureError(e)) <-> in

}

trait ErrorTransTests[F[_, _]] extends BifunctorTests[F] {
  def laws: ErrorTransLaws[F]

  def errorTrans[
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
      parent = Some(bifunctor[A, A2, A3, B, B2, B3]),
      "transformErrorF pureError" -> forAll(laws.transformErrorFpureErrorEquiv[A, B] _),
    )
}

object ErrorTransTests {
  def apply[F[_, _]](implicit FF: ErrorTrans[F]): ErrorTransTests[F] =
    new ErrorTransTests[F] {
      override def laws: ErrorTransLaws[F] = new ErrorTransLaws[F] {
        override implicit def F: ErrorTrans[F] with Bifunctor[F] =
          new ErrorTrans[F] with Bifunctor[F] {
            override def bifunctor: Bifunctor[F] = FF.bifunctor

            override def pureError[L, R](l: L): F[L, R] = FF.pureError(l)

            override def flatMapError[L, R, LL](in: F[L, R])(func: L => F[LL, R]): F[LL, R] =
              FF.flatMapError(in)(func)

            override def bimap[A, B, C, D](fab: F[A, B])(f: A => C, g: B => D): F[C, D] =
              FF.bifunctor.bimap(fab)(f, g)
          }
      }
    }
}
