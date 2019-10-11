package hotpotato

import cats.{Bifunctor, FlatMap}
import shapeless.Coproduct
import shapeless.ops.coproduct.Basis
import cats.implicits._
import scala.language.implicitConversions

class Wrapper[F[_, _], L <: Coproduct, R](val unwrap: F[L, R]) extends AnyVal {
  def map[RR](func: R => RR)(implicit f: Bifunctor[F]): Wrapper[F, L, RR] =
    new Wrapper(f.rightFunctor.map(unwrap)(func))

  def flatMap[G[_], LL <: Coproduct, RR](f: SuperMagnet[F, L, LL, R, RR])(
    implicit
    funcToBi: FunctorToBifunctor[F, LL, G],
    gFlatMap: FlatMap[G],
    fBifunctor: Bifunctor[F],
  ): Wrapper[F, LL, RR] = {
    val flatMapFunc: R => G[RR] = f.func.andThen(wr => funcToBi.fromBi(wr.unwrap))
    val leftSideEmbedded: F[LL, R] = unwrap.leftMap(l => f.basis.inverse(Right(l)))
    new Wrapper(funcToBi.toBi(funcToBi.fromBi(leftSideEmbedded).flatMap(flatMapFunc)))
  }

  // Case when LL is a subset of L
    def flatMap[G[_], LL <: Coproduct, RR](f: SubMagnet[F, L, LL, R, RR])(
      implicit
      funcToBi: FunctorToBifunctor[F, L, G],
      gFlatMap: FlatMap[G],
      fBifunctor: Bifunctor[F],
    ): Wrapper[F, L, RR] = {
      val gr: G[R] = funcToBi.fromBi(unwrap)
      val ff: R => G[RR] =
        f.func.andThen(wrapper => funcToBi.fromBi(wrapper.unwrap.leftMap(ll => f.subset.embedIn(ll))))
      new Wrapper(funcToBi.toBi(gr.flatMap(ff)))
    }
}

class SuperMagnet[F[_, _], L <: Coproduct, LL <: Coproduct, -R, RR](
  val func: R => Wrapper[F, LL, RR],
  val basis: Basis[LL, L],
)

object SuperMagnet {
  implicit def fromBasis[F[_, _], L <: Coproduct, LL <: Coproduct, R, RR](func: R => Wrapper[F, LL, RR])(
    implicit basis: Basis[LL, L],
  ): SuperMagnet[F, L, LL, R, RR] = new SuperMagnet[F,L, LL, R, RR](func, basis)

}

class SubMagnet[F[_, _], L <: Coproduct, LL <: Coproduct, -R, RR](
  val func: R => Wrapper[F, LL, RR],
  val subset: Subset[LL, L]
)

object SubMagnet {
  implicit def fromSubset[F[_, _], L <: Coproduct, LL <: Coproduct, R, RR](func: R => Wrapper[F, LL, RR])(
    implicit subset: Subset[LL, L],
  ): SubMagnet[F, L, LL, R, RR] = new SubMagnet[F,L, LL, R, RR](func, subset)

}

