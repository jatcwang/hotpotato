import cats.Functor
import cats.data._
import cats.implicits._
import shapeless._
import shapeless.ops.coproduct.{Basis, Inject}

import scala.language.higherKinds

/** Typeclass for effect type that has an error type and can transform the error to another */
trait ErrorTrans[F[_, _], E, A] {
  def transformError[EE](fea: F[E, A])(f: E => EE): F[EE, A]
}

object ErrorTrans extends LowerPriorityErrorTrans {
  implicit def eitherErrorTrans[E, A]: ErrorTrans[Either, E, A] = new ErrorTrans[Either, E, A] {
    override def transformError[EE](fea: Either[E, A])(f: E => EE): Either[EE, A] = fea.leftMap(f)
  }

  implicit def eitherTErrorTrans[G[_]: Functor, E, A]: ErrorTrans[EitherT[G, ?, ?], E, A] =
    new ErrorTrans[EitherT[G, ?, ?], E, A] {
      override def transformError[EE](fea: EitherT[G, E, A])(f: E => EE): EitherT[G, EE, A] =
        fea.leftMap(f)
    }

  implicit class ErrorTransCoprodEmbedOps[F[_, _], ThisError <: Coproduct, T](val in: F[ThisError, T]) extends AnyVal {
    def embedError[Super <: Coproduct](
      implicit F: ErrorTrans[F, ThisError, T],
      embedder: Embedder[Super],
      basis: Basis[Super, ThisError],
    ): F[Super, T] =
      F.transformError(in) { err =>
        embedder.embed[ThisError](err)(basis)
      }
  }

  implicit class ErrorTransOps[F[_, _], ThisError <: Coproduct, T](val in: F[ThisError, T]) extends AnyVal {

    def handleSome[A, AOut, B, BOut, BasisRest <: Coproduct, UniqueOut <: Coproduct](ha: A => AOut, hb: B => BOut)(
      implicit basis: Basis.Aux[ThisError, A :+: B :+: CNil, BasisRest],
      unique: Unique.Aux[AOut :+: BOut :+: BasisRest, UniqueOut],
      errorTrans: ErrorTrans[F, ThisError, T]
    ): F[UniqueOut, T] = {
      errorTrans.transformError(in) { err =>
        unique.apply {
          basis(err) match {
            case Left(rest) => rest.extendLeftBy[AOut :+: BOut :+: CNil]
            case Right(extracted) =>
              type Inter = AOut :+: BOut :+: BasisRest
              extracted match {
                case Inl(a)         => Coproduct[Inter](ha(a))
                case Inr(Inl(b))    => Coproduct[Inter](hb(b))
                case Inr(Inr(cnil)) => cnil.impossible
              }
          }
        }
      }
    }

    def handle[A, B, Out, BasisRest <: Coproduct, UniqueOut <: Coproduct](ha: A => Out, hb: B => Out)(
      implicit basis: Basis[ThisError, A :+: B :+: CNil],
      toKnownCop: ThisError =:= (A :+: B :+: CNil),
      errorTrans: ErrorTrans[F, ThisError, T]
    ): F[Out, T] = {
      errorTrans.transformError(in) { err =>
        toKnownCop(err) match {
          case Inl(a)         => ha(a)
          case Inr(Inl(b))    => hb(b)
          case Inr(Inr(cnil)) => cnil.impossible
        }
      }
    }

    def handle[A, B, C, Out, BasisRest <: Coproduct, UniqueOut <: Coproduct](ha: A => Out, hb: B => Out, hc: C => Out)(
      implicit basis: Basis[ThisError, A :+: B :+: C :+: CNil],
      toKnownCop: ThisError =:= (A :+: B :+: C :+: CNil),
      errorTrans: ErrorTrans[F, ThisError, T]
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
    def embedError[Super <: Coproduct](
      implicit F: ErrorTrans[F, ThisError, T],
      embedder: Embedder[Super],
      inject: Inject[Super, ThisError]
    ): F[Super, T] =
      F.transformError(in) { err =>
        inject(err)
      }
  }

}