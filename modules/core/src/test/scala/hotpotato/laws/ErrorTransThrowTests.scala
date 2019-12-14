package hotpotato.laws

import cats.Eq
import cats.laws.IsEq
import cats.laws._
import cats.laws.discipline._
import hotpotato.ErrorTransThrow
import org.scalacheck.Arbitrary
import org.scalacheck.Prop._

trait ErrorTransThrowLaws[F[_, _]] extends ErrorTransLaws[F] {
  implicit def F: ErrorTransThrow[F]

  def extractAndThrowNothingIdentity[L, R](in: F[L, R]): IsEq[F[L, R]] =
    F.extractAndThrow(in)(e => Right(e)) <-> in
}

trait ErrorTransThrowTests[F[_, _]] extends ErrorTransTests[F] {
  def laws: ErrorTransThrowLaws[F]

  def errorTransThrow[
    L: Arbitrary: Eq,
    L2: Arbitrary: Eq,
    L3: Arbitrary: Eq,
    R: Arbitrary: Eq,
  ](
    implicit
    ArbFLB: Arbitrary[F[L, R]],
    ArbL2: Arbitrary[L  => L2],
    ArbL3: Arbitrary[L2 => L3],
    EqFLB: Eq[F[L, R]],
    EqFL2B: Eq[F[L2, R]],
    EqFL3B: Eq[F[L3, R]],
  ) =
    new DefaultRuleSet(
      name   = "ErrorTrans",
      parent = Some(errorTrans[L, L2, L3, R]),
      "extractAndThrow nothing identity" -> forAll(laws.extractAndThrowNothingIdentity[L, R] _),
    )
}

object ErrorTransThrowTests {
  def apply[F[_, _]](implicit FF: ErrorTransThrow[F]): ErrorTransThrowTests[F] =
    new ErrorTransThrowTests[F] {
      override def laws: ErrorTransThrowLaws[F] = new ErrorTransThrowLaws[F] {
        override implicit def F: ErrorTransThrow[F] = FF
      }
    }
}
