package hotpotato

import cats.{Bifunctor, FlatMap, Functor}
import cats.data._
import cats.implicits._
import shapeless._
import shapeless.ops.coproduct.{Basis, Inject}

trait BiToMonad[F[_, _], A, G[_]] {
  def to[B](fa: F[A, B]): G[B]
  def from[B](ga: G[B]): F[A, B]
}

object BiToMonad {
  implicit def eitherTBiToMonad[FF[_], A]: BiToMonad[EitherT[FF, *, *], A, EitherT[FF, A, *]] =
    new BiToMonad[EitherT[FF, *, *], A, EitherT[FF, A, *]] {
      override def to[B](fa: EitherT[FF, A, B]): EitherT[FF, A, B]   = fa
      override def from[B](ga: EitherT[FF, A, B]): EitherT[FF, A, B] = ga
    }
}

/** Typeclass for effect type that has an error type and can transform the error to another */
trait ErrorTrans[F[_, _], E, A] {
  def transformError[EE](fea: F[E, A])(f: E => EE): F[EE, A]
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

  implicit class ErrorTransCoprodEmbedOps[F[_, _], ThisError <: Coproduct, A](
    val in: F[ThisError, A],
  ) extends AnyVal {
    def embedError[Super <: Coproduct](
      implicit F: ErrorTrans[F, ThisError, A],
      embedder: Embedder[Super],
      basis: Basis[Super, ThisError],
    ): F[Super, A] =
      F.transformError(in) { err =>
        embedder.embed[ThisError](err)(basis)
      }

    def flatMap[ThatError <: Coproduct, B, G[_]](f: A => F[ThatError, B])(
      implicit G: FlatMap[G],
      bimThis: BiToMonad[F, ThisError, G],
      basis: Basis[ThisError, ThatError],
      bifunc: Bifunctor[F],
    ): F[ThisError, B] = {
      val flatMapFunc =
        f.andThen(s => s.leftMap(thatError => basis.inverse(Right(thatError)))).map(bimThis.to)
      bimThis.from(bimThis.to(in).flatMap(flatMapFunc))
    }
  }

  implicit class ErrorTransOps[F[_, _], ThisError <: Coproduct, T](val in: F[ThisError, T])
      extends AnyVal {

    def handleSome[A0, A0Out, A1, A1Out, BasisRest <: Coproduct, UniqueOut <: Coproduct](
      a0func: A0 => A0Out,
      a1func: A1 => A1Out,
    )(
      implicit basis: Basis.Aux[ThisError, A0 :+: A1 :+: CNil, BasisRest],
      unique: Unique.Aux[A0Out :+: A1Out :+: BasisRest, UniqueOut],
      errorTrans: ErrorTrans[F, ThisError, T],
    ): F[UniqueOut, T] = {
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
      implicit basis: Basis[ThisError, A :+: B :+: CNil],
      toKnownCop: ThisError =:= (A :+: B :+: CNil),
      errorTrans: ErrorTrans[F, ThisError, T],
    ): F[Out, T] = {
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
      implicit basis: Basis[ThisError, A :+: B :+: C :+: CNil],
      toKnownCop: ThisError =:= (A :+: B :+: C :+: CNil),
      errorTrans: ErrorTrans[F, ThisError, T],
    ): F[Out, T] = {
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
//    def embedError[Super <: Coproduct](
//      implicit F: ErrorTrans[F, ThisError, T],
//      embedder: Embedder[Super],
//      inject: Inject[Super, ThisError],
//    ): F[Super, T] =
//      F.transformError(in) { err =>
//        inject(err)
//      }

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
