package hotpotato

import cats.{Bifunctor, FlatMap, Functor}
import shapeless.Coproduct
import shapeless.ops.coproduct.Basis
import cats.implicits._
import hotpotato.coproduct.{Combine, SameElem}

import scala.language.implicitConversions

class Wrapper[F[_, _], L <: Coproduct, R](val unwrap: F[L, R]) {
  final def map[RR](func: R => RR)(implicit functor: Functor[F[L, *]]): Wrapper[F, L, RR] = {
    val u = unwrap
    new Wrapper[F, L, RR](u.map(func))
  }
}

class SubWrapper[F[_, _], L <: Coproduct, R](override val unwrap: F[L, R])
    extends Wrapper[F, L, R](unwrap) {
  def flatMap[LL <: Coproduct, RR](func: R => Wrapper[F, LL, RR])(
    implicit
    bifunctor: Bifunctor[F],
    flatMap: FlatMap[F[L, *]],
    basis: Basis[L, LL],
  ): SubWrapper[F, L, RR] = {
    val newFunc: R => F[L, RR] = func.andThen(w => w.unwrap.leftMap(ll => basis.inverse(Right(ll))))
    new SubWrapper(unwrap.flatMap(newFunc))
  }
}

class SuperWrapper[F[_, _], L <: Coproduct, R](override val unwrap: F[L, R])
    extends Wrapper[F, L, R](unwrap) {
  def flatMap[LL <: Coproduct, RR](func: R => Wrapper[F, LL, RR])(
    implicit
    bifunctor: Bifunctor[F],
    flatMap: FlatMap[F[LL, *]],
    basis: Basis[LL, L],
  ): SuperWrapper[F, LL, RR] = {
    val leftMapped: F[LL, R] = unwrap.leftMap(l => basis.inverse(Right(l)))
    new SuperWrapper(leftMapped.flatMap(func.andThen(_.unwrap)))
  }
}

class CombineWrapper[F[_, _], L <: Coproduct, CombL <: Coproduct, R](override val unwrap: F[L, R])
    extends Wrapper[F, L, R](unwrap) {
  def flatMap[LL <: Coproduct, RR](func: R => Wrapper[F, LL, RR])(
    implicit
    bifunctor: Bifunctor[F],
    flatMap: FlatMap[F[CombL, *]],
    combine: Combine.Aux[L, LL, CombL],
  ): Wrapper[F, CombL, RR] = {
    val f: F[CombL, R] = unwrap.leftMap(l => combine.right(l))
    val newFunc: R => F[CombL, RR] = func.andThen(w => w.unwrap.leftMap(ll => combine.left(ll)))
    new Wrapper(f.flatMap(newFunc))
  }
}
