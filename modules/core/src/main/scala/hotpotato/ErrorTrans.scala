package hotpotato

import cats.Functor
import cats.data._
import cats.implicits._
import hotpotato.coproduct.Unique
import shapeless._
import shapeless.ops.coproduct.{Basis, Inject}
import zio._

/** Typeclass for effect type that has an error type and can transform the error to another */
trait ErrorTrans[F[_, _], L, R] {
  def transformError[LL](fea: F[L, R])(f: L => LL): F[LL, R]
}

object ErrorTrans extends LowerPriorityErrorTrans with ErrorTransSyntax {
  implicit def eitherErrorTrans[E, A]: ErrorTrans[Either, E, A] =
    new ErrorTrans[Either, E, A] {
      override def transformError[EE](fea: Either[E, A])(
        f: E => EE,
      ): Either[EE, A] = fea.leftMap(f)
    }

  implicit def eitherTErrorTrans[G[_]: Functor, E, A]: ErrorTrans[EitherT[G, *, *], E, A] =
    new ErrorTrans[EitherT[G, *, *], E, A] {
      override def transformError[EE](
        fea: EitherT[G, E, A],
      )(f: E => EE): EitherT[G, EE, A] =
        fea.leftMap(f)
    }

  //FIXME: zio optional dep
  implicit def zioErrorTrans[Env, L, R]: ErrorTrans[ZIO[Env, *, *], L, R] =
    new ErrorTrans[ZIO[Env, *, *], L, R] {
      override def transformError[LL](fea: ZIO[Env, L, R])(
        f: L => LL,
      ): ZIO[Env, LL, R] = fea.mapError(f)
    }

  implicit class ErrorTransCoprodEmbedOps[F[_, _], L <: Coproduct, R](
    val in: F[L, R],
  ) extends AnyVal {
    def embedError[Super <: Coproduct](
      implicit F: ErrorTrans[F, L, R],
      embedder: Embedder[Super],
      basis: Basis[Super, L],
    ): F[Super, R] =
      F.transformError(in) { err =>
        embedder.embed[L](err)(basis)
      }

  }

  implicit class ErrorTransIdLeftOps[F[_, _], L, R](val in: F[L, R]) extends AnyVal {

    def embedError[Super <: Coproduct](
      implicit F: ErrorTrans[F, L, R],
      inject: Inject[Super, L],
    ): F[Super, R] =
      F.transformError(in) { err =>
        inject(err)
      }
  }

  implicit class ErrorTransOpops[F[_, _], L <: Coproduct, R](val in: F[L, R]) extends AnyVal {
//FIXME:add handleSomeInto (same output type so no need for unique)
  }

}

trait LowerPriorityErrorTrans {
  implicit class ErrorTransEmbedOps[F[_, _], ThisError, T](
    val in: F[ThisError, T],
  ) {

    def handleSomeAdt[
      A0,
      A0Out,
      A1,
      A1Out,
      Co <: Coproduct,
      PartialCo <: Coproduct,
      Super <: Coproduct,
    ](f0: A0 => A0Out, f1: A1 => A1Out)(
      implicit
      F: ErrorTrans[F, ThisError, T],
      gen: Generic.Aux[ThisError, Co],
      basisRemoveFromAdt: Basis.Aux[Co, A0 :+: A1 :+: CNil, PartialCo],
    ): F[A0Out :+: A1Out :+: PartialCo, T] = {
      type AddThem = A0Out :+: A1Out :+: PartialCo
      F.transformError(in) { err =>
        basisRemoveFromAdt(gen.to(err)) match {
          case Left(partialCo) =>
            partialCo.extendLeftBy[A0Out :+: A1Out :+: CNil]
          case Right(handledCases) =>
            handledCases match {
              case Inl(a0)        => Coproduct[AddThem](f0(a0))
              case Inr(Inl(a1))   => Coproduct[AddThem](f1(a1))
              case Inr(Inr(cnil)) => cnil.impossible
            }
        }
      }
    }

    def handleSomeAdt[
      L0,
      L0Out,
      L1,
      L1Out,
      L2,
      L2Out,
      Co <: Coproduct,
      PartialCo <: Coproduct,
      Super <: Coproduct,
    ](f0: L0 => L0Out, f1: L1 => L1Out, f2: L2 => L2Out)(
      implicit
      F: ErrorTrans[F, ThisError, T],
      gen: Generic.Aux[ThisError, Co],
      basisRemoveFromAdt: Basis.Aux[Co, L0 :+: L1 :+: L2 :+: CNil, PartialCo],
    ): F[L0Out :+: L1Out :+: L2Out :+: PartialCo, T] = {
      type AddThem = L0Out :+: L1Out :+: L2Out :+: PartialCo
      F.transformError(in) { err =>
        basisRemoveFromAdt(gen.to(err)) match {
          case Left(partialCo) =>
            partialCo.extendLeftBy[L0Out :+: L1Out :+: L2Out :+: CNil]
          case Right(handledCases) =>
            handledCases match {
              case Inl(x)              => Coproduct[AddThem](f0(x))
              case Inr(Inl(x))         => Coproduct[AddThem](f1(x))
              case Inr(Inr(Inl(x)))    => Coproduct[AddThem](f2(x))
              case Inr(Inr(Inr(cnil))) => cnil.impossible
            }
        }
      }
    }

    def handleSomeAdt[
      L0,
      L0Out,
      L1,
      L1Out,
      L2,
      L2Out,
      L3,
      L3Out,
      Co <: Coproduct,
      PartialCo <: Coproduct,
      Super <: Coproduct,
    ](f0: L0 => L0Out, f1: L1 => L1Out, f2: L2 => L2Out, f3: L3 => L3Out)(
      implicit
      F: ErrorTrans[F, ThisError, T],
      gen: Generic.Aux[ThisError, Co],
      basisRemoveFromAdt: Basis.Aux[Co, L0 :+: L1 :+: L2 :+: L3 :+: CNil, PartialCo],
    ): F[L0Out :+: L1Out :+: L2Out :+: L3Out :+: PartialCo, T] = {
      type AddThem = L0Out :+: L1Out :+: L2Out :+: L3Out :+: PartialCo
      F.transformError(in) { err =>
        basisRemoveFromAdt(gen.to(err)) match {
          case Left(partialCo) =>
            partialCo.extendLeftBy[L0Out :+: L1Out :+: L2Out :+: L3Out :+: CNil]
          case Right(handledCases) =>
            handledCases match {
              case Inl(x)                   => Coproduct[AddThem](f0(x))
              case Inr(Inl(x))              => Coproduct[AddThem](f1(x))
              case Inr(Inr(Inl(x)))         => Coproduct[AddThem](f2(x))
              case Inr(Inr(Inr(Inl(x))))    => Coproduct[AddThem](f3(x))
              case Inr(Inr(Inr(Inr(cnil)))) => cnil.impossible
            }
        }
      }
    }

    def handleSomeAdt[
      L0,
      L0Out,
      L1,
      L1Out,
      L2,
      L2Out,
      L3,
      L3Out,
      L4,
      L4Out,
      Co <: Coproduct,
      PartialCo <: Coproduct,
      Super <: Coproduct,
    ](f0: L0 => L0Out, f1: L1 => L1Out, f2: L2 => L2Out, f3: L3 => L3Out, f4: L4 => L4Out)(
      implicit
      F: ErrorTrans[F, ThisError, T],
      gen: Generic.Aux[ThisError, Co],
      basisRemoveFromAdt: Basis.Aux[Co, L0 :+: L1 :+: L2 :+: L3 :+: L4 :+: CNil, PartialCo],
    ): F[L0Out :+: L1Out :+: L2Out :+: L3Out :+: L4Out :+: PartialCo, T] = {
      type AddThem = L0Out :+: L1Out :+: L2Out :+: L3Out :+: L4Out :+: PartialCo
      F.transformError(in) { err =>
        basisRemoveFromAdt(gen.to(err)) match {
          case Left(partialCo) =>
            partialCo.extendLeftBy[
              L0Out :+: L1Out :+: L2Out :+: L3Out :+: L4Out :+: CNil,
            ]
          case Right(handledCases) =>
            handledCases match {
              case Inl(x)                        => Coproduct[AddThem](f0(x))
              case Inr(Inl(x))                   => Coproduct[AddThem](f1(x))
              case Inr(Inr(Inl(x)))              => Coproduct[AddThem](f2(x))
              case Inr(Inr(Inr(Inl(x))))         => Coproduct[AddThem](f3(x))
              case Inr(Inr(Inr(Inr(Inl(x)))))    => Coproduct[AddThem](f4(x))
              case Inr(Inr(Inr(Inr(Inr(cnil))))) => cnil.impossible
            }
        }
      }
    }

  }

}
