package hotpotato

import cats.{Bifunctor, FlatMap, Functor}
import cats.data._
import cats.implicits._
import hotpotato.coproduct.Unique
import shapeless._
import shapeless.ops.coproduct.{Basis, Inject}

/** Typeclass for effect type that has an error type and can transform the error to another */
trait ErrorTrans[F[_, _], L, R] {
  def transformError[LL](fea: F[L, R])(f: L => LL): F[LL, R]
}

object ErrorTrans extends LowerPriorityErrorTrans {
  implicit def eitherErrorTrans[E, A]: ErrorTrans[Either, E, A] = new ErrorTrans[Either, E, A] {
    override def transformError[EE](fea: Either[E, A])(f: E => EE): Either[EE, A] = fea.leftMap(f)
  }

  implicit def eitherTErrorTrans[G[_]: Functor, E, A]: ErrorTrans[EitherT[G, *, *], E, A] =
    new ErrorTrans[EitherT[G, *, *], E, A] {
      override def transformError[EE](fea: EitherT[G, E, A])(f: E => EE): EitherT[G, EE, A] =
        fea.leftMap(f)
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

  implicit class ErrorTransOps[F[_, _], L <: Coproduct, R](val in: F[L, R])
      extends AnyVal {

    def wrapC[CombL <: Coproduct]: CombineWrapper[F, L, CombL, R] = new CombineWrapper(in)
    def wrapSub: SubWrapper[F, L, R] = new SubWrapper(in)
    def wrapSup: SuperWrapper[F, L, R] = new SuperWrapper(in)
    def wrap: Wrapper[F, L, R] = new Wrapper(in)

    def handleSome[A0, A0Out, A1, A1Out, BasisRest <: Coproduct, UniqueOut <: Coproduct](
      a0func: A0 => A0Out,
      a1func: A1 => A1Out,
    )(
      implicit basis: Basis.Aux[L, A0 :+: A1 :+: CNil, BasisRest],
      unique: Unique.Aux[A0Out :+: A1Out :+: BasisRest, UniqueOut],
      errorTrans: ErrorTrans[F, L, R],
    ): F[UniqueOut, R] = {
      errorTrans.transformError(in) { err =>
        unique.apply {
          basis(err) match {
            case Left(rest) => rest.extendLeftBy[A0Out :+: A1Out :+: CNil]
            case Right(extracted) =>
              type Inter = A0Out :+: A1Out :+: BasisRest
              extracted match {
                case Inl(a0)        => Coproduct[Inter](a0func(a0))
                case Inr(Inl(b))    => Coproduct[Inter](a1func(b))
                case Inr(Inr(cnil)) => cnil.impossible
              }
          }
        }
      }
    }

    def handle[A, B, Out, BasisRest <: Coproduct, UniqueOut <: Coproduct](
      ha: A => Out,
      hb: B => Out,
    )(
      implicit basis: Basis[L, A :+: B :+: CNil],
      toKnownCop: L =:= (A :+: B :+: CNil),
      errorTrans: ErrorTrans[F, L, R],
    ): F[Out, R] = {
      errorTrans.transformError(in) { err =>
        toKnownCop(err) match {
          case Inl(a)         => ha(a)
          case Inr(Inl(b))    => hb(b)
          case Inr(Inr(cnil)) => cnil.impossible
        }
      }
    }

    def handle[A, B, C, Out, BasisRest <: Coproduct, UniqueOut <: Coproduct](
      ha: A => Out,
      hb: B => Out,
      hc: C => Out,
    )(
      implicit basis: Basis[L, A :+: B :+: C :+: CNil],
      toKnownCop: L =:= (A :+: B :+: C :+: CNil),
      errorTrans: ErrorTrans[F, L, R],
    ): F[Out, R] = {
      errorTrans.transformError(in) { err =>
        toKnownCop(err) match {
          case Inl(a)              => ha(a)
          case Inr(Inl(b))         => hb(b)
          case Inr(Inr(Inl(c)))    => hc(c)
          case Inr(Inr(Inr(cnil))) => cnil.impossible
        }
      }
    }

  }

}

trait LowerPriorityErrorTrans {
  implicit class ErrorTransEmbedOps[F[_, _], ThisError, T](val in: F[ThisError, T]) {
    def handleSomeAdt[
      A0,
      A0Out,
      A1,
      A1Out,
      Co <: Coproduct,
      PartialCo <: Coproduct,
      Super <: Coproduct,
    ](
      f0: A0 => A0Out,
      f1: A1 => A1Out,
    )(
      implicit
      F: ErrorTrans[F, ThisError, T],
      gen: Generic.Aux[ThisError, Co],
      basisRemoveFromAdt: Basis.Aux[Co, A0 :+: A1 :+: CNil, PartialCo],
    ): F[A0Out :+: A1Out :+: PartialCo, T] = {
      type AddThem = A0Out :+: A1Out :+: PartialCo
      F.transformError(in) { err =>
        basisRemoveFromAdt(gen.to(err)) match {
          case Left(partialCo) => partialCo.extendLeftBy[A0Out :+: A1Out :+: CNil]
          case Right(handledCases) =>
            handledCases match {
              case Inl(a0)        => Coproduct[AddThem](f0(a0))
              case Inr(Inl(a1))   => Coproduct[AddThem](f1(a1))
              case Inr(Inr(cnil)) => cnil.impossible
            }
        }
      }
    }

  }

}
