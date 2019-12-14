package hotpotato.laws

import cats.Eq
import cats.laws.IsEq
import cats.laws._
import cats.laws.discipline._
import hotpotato.ErrorTrans
import org.scalacheck.Arbitrary
import org.scalacheck.Prop._
import org.typelevel.discipline.Laws

trait ErrorTransLaws[F[_, _]] {
  implicit def F: ErrorTrans[F]

  def errorTransMapErrorIdentity[L, R](fa: F[L, R]): IsEq[F[L, R]] =
    F.mapError(fa)(identity) <-> fa

  def errorTransMapErrorComposition[L, R, L2, L3](
    fa: F[L, R],
    f: L  => L2,
    g: L2 => L3,
  ): IsEq[F[L3, R]] =
    F.mapError(F.mapError(fa)(f))(g) <-> F.mapError(fa)(f.andThen(g))

  def mapErrorFlatMapCoherence[L, R, LL](fa: F[L, R], f: L => LL): IsEq[F[LL, R]] =
    F.flatMapError(fa)(a => F.pureError(f(a))) <-> F.mapError(fa)(f)

}

trait ErrorTransTests[F[_, _]] extends Laws {
  def laws: ErrorTransLaws[F]

  def errorTrans[
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
      parent = None,
      "mapError Identity" -> forAll(laws.errorTransMapErrorIdentity[L, R] _),
      "mapError associativity" -> forAll(laws.errorTransMapErrorComposition[L, R, L2, L3] _),
      "mapError flatMapError coherence" -> forAll(laws.mapErrorFlatMapCoherence[L, R, L2] _),
    )
}

object ErrorTransTests {
  def apply[F[_, _]](implicit FF: ErrorTrans[F]): ErrorTransTests[F] =
    new ErrorTransTests[F] {
      override def laws: ErrorTransLaws[F] = new ErrorTransLaws[F] {
        override implicit def F: ErrorTrans[F] = FF
      }
    }
}
