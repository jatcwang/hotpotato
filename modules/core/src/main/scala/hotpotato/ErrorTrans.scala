package hotpotato

import cats.{Bifunctor, FlatMap, Functor}
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

object ErrorTrans extends LowerPriorityErrorTrans {
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

  implicit class ErrorTransOps[F[_, _], L <: Coproduct, R](val in: F[L, R]) extends AnyVal {

    def handleSome[L0, L0Out, L1, L1Out, BasisRest <: Coproduct, UniqueOut <: Coproduct](
      func0: L0 => L0Out,
      func1: L1 => L1Out,
    )(
      implicit basis: Basis.Aux[L, L0 :+: L1 :+: CNil, BasisRest],
      unique: Unique.Aux[L0Out :+: L1Out :+: BasisRest, UniqueOut],
      errorTrans: ErrorTrans[F, L, R],
    ): F[UniqueOut, R] = {
      errorTrans.transformError(in) { err =>
        unique.apply {
          basis(err) match {
            case Left(rest) => rest.extendLeftBy[L0Out :+: L1Out :+: CNil]
            case Right(extracted) =>
              type Inter = L0Out :+: L1Out :+: BasisRest
              extracted match {
                case Inl(x)         => Coproduct[Inter](func0(x))
                case Inr(Inl(x))    => Coproduct[Inter](func1(x))
                case Inr(Inr(cnil)) => cnil.impossible
              }
          }
        }
      }
    }

    def handleSome[L0, L0Out, L1, L1Out, L2, L2Out, BasisRest <: Coproduct, UniqueOut <: Coproduct](
      func0: L0 => L0Out,
      func1: L1 => L1Out,
      func2: L2 => L2Out,
    )(
      implicit basis: Basis.Aux[L, L0 :+: L1 :+: L2 :+: CNil, BasisRest],
      unique: Unique.Aux[L0Out :+: L1Out :+: L2Out :+: BasisRest, UniqueOut],
      errorTrans: ErrorTrans[F, L, R],
    ): F[UniqueOut, R] = {
      errorTrans.transformError(in) { err =>
        unique.apply {
          basis(err) match {
            case Left(rest) =>
              rest.extendLeftBy[L0Out :+: L1Out :+: L2Out :+: CNil]
            case Right(extracted) =>
              type Inter = L0Out :+: L1Out :+: L2Out :+: BasisRest
              extracted match {
                case Inl(x)              => Coproduct[Inter](func0(x))
                case Inr(Inl(x))         => Coproduct[Inter](func1(x))
                case Inr(Inr(Inl(x)))    => Coproduct[Inter](func2(x))
                case Inr(Inr(Inr(cnil))) => cnil.impossible
              }
          }
        }
      }
    }

    def handleSome[
      L0,
      L0Out,
      L1,
      L1Out,
      L2,
      L2Out,
      L3,
      L3Out,
      BasisRest <: Coproduct,
      UniqueOut <: Coproduct,
    ](func0: L0 => L0Out, func1: L1 => L1Out, func2: L2 => L2Out, func3: L3 => L3Out)(
      implicit basis: Basis.Aux[L, L0 :+: L1 :+: L2 :+: L3 :+: CNil, BasisRest],
      unique: Unique.Aux[L0Out :+: L1Out :+: L2Out :+: L3Out :+: BasisRest, UniqueOut],
      errorTrans: ErrorTrans[F, L, R],
    ): F[UniqueOut, R] = {
      errorTrans.transformError(in) { err =>
        unique.apply {
          basis(err) match {
            case Left(rest) =>
              rest.extendLeftBy[L0Out :+: L1Out :+: L2Out :+: L3Out :+: CNil]
            case Right(extracted) =>
              type Inter = L0Out :+: L1Out :+: L2Out :+: L3Out :+: BasisRest
              extracted match {
                case Inl(x)                   => Coproduct[Inter](func0(x))
                case Inr(Inl(x))              => Coproduct[Inter](func1(x))
                case Inr(Inr(Inl(x)))         => Coproduct[Inter](func2(x))
                case Inr(Inr(Inr(Inl(x))))    => Coproduct[Inter](func3(x))
                case Inr(Inr(Inr(Inr(cnil)))) => cnil.impossible
              }
          }
        }
      }
    }

    def handleSome[
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
      BasisRest <: Coproduct,
      UniqueOut <: Coproduct,
    ](
      func0: L0 => L0Out,
      func1: L1 => L1Out,
      func2: L2 => L2Out,
      func3: L3 => L3Out,
      func4: L4 => L4Out,
    )(
      implicit basis: Basis.Aux[L, L0 :+: L1 :+: L2 :+: L3 :+: L4 :+: CNil, BasisRest],
      unique: Unique.Aux[
        L0Out :+: L1Out :+: L2Out :+: L3Out :+: L4Out :+: BasisRest,
        UniqueOut,
      ],
      errorTrans: ErrorTrans[F, L, R],
    ): F[UniqueOut, R] = {
      errorTrans.transformError(in) { err =>
        unique.apply {
          basis(err) match {
            case Left(rest) =>
              rest.extendLeftBy[
                L0Out :+: L1Out :+: L2Out :+: L3Out :+: L4Out :+: CNil,
              ]
            case Right(extracted) =>
              type Inter =
                L0Out :+: L1Out :+: L2Out :+: L3Out :+: L4Out :+: BasisRest
              extracted match {
                case Inl(x)                        => Coproduct[Inter](func0(x))
                case Inr(Inl(x))                   => Coproduct[Inter](func1(x))
                case Inr(Inr(Inl(x)))              => Coproduct[Inter](func2(x))
                case Inr(Inr(Inr(Inl(x))))         => Coproduct[Inter](func3(x))
                case Inr(Inr(Inr(Inr(Inl(x)))))    => Coproduct[Inter](func4(x))
                case Inr(Inr(Inr(Inr(Inr(cnil))))) => cnil.impossible
              }
          }
        }
      }
    }

    def handleSome[
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
      L5,
      L5Out,
      BasisRest <: Coproduct,
      UniqueOut <: Coproduct,
    ](
      func0: L0 => L0Out,
      func1: L1 => L1Out,
      func2: L2 => L2Out,
      func3: L3 => L3Out,
      func4: L4 => L4Out,
      func5: L5 => L5Out,
    )(
      implicit basis: Basis.Aux[L, L0 :+: L1 :+: L2 :+: L3 :+: L4 :+: L5 :+: CNil, BasisRest],
      unique: Unique.Aux[
        L0Out :+: L1Out :+: L2Out :+: L3Out :+: L4Out :+: L5Out :+: BasisRest,
        UniqueOut,
      ],
      errorTrans: ErrorTrans[F, L, R],
    ): F[UniqueOut, R] = {
      errorTrans.transformError(in) { err =>
        unique.apply {
          basis(err) match {
            case Left(rest) =>
              rest.extendLeftBy[
                L0Out :+: L1Out :+: L2Out :+: L3Out :+: L4Out :+: L5Out :+: CNil,
              ]
            case Right(extracted) =>
              type Inter =
                L0Out :+: L1Out :+: L2Out :+: L3Out :+: L4Out :+: L5Out :+: BasisRest
              extracted match {
                case Inl(x)                     => Coproduct[Inter](func0(x))
                case Inr(Inl(x))                => Coproduct[Inter](func1(x))
                case Inr(Inr(Inl(x)))           => Coproduct[Inter](func2(x))
                case Inr(Inr(Inr(Inl(x))))      => Coproduct[Inter](func3(x))
                case Inr(Inr(Inr(Inr(Inl(x))))) => Coproduct[Inter](func4(x))
                case Inr(Inr(Inr(Inr(Inr(Inl(x)))))) =>
                  Coproduct[Inter](func5(x))
                case Inr(Inr(Inr(Inr(Inr(Inr(cnil)))))) => cnil.impossible
              }
          }
        }
      }
    }

    def handleSome[
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
      L5,
      L5Out,
      L6,
      L6Out,
      BasisRest <: Coproduct,
      UniqueOut <: Coproduct,
    ](
      func0: L0 => L0Out,
      func1: L1 => L1Out,
      func2: L2 => L2Out,
      func3: L3 => L3Out,
      func4: L4 => L4Out,
      func5: L5 => L5Out,
      func6: L6 => L6Out,
    )(
      implicit basis: Basis.Aux[
        L,
        L0 :+: L1 :+: L2 :+: L3 :+: L4 :+: L5 :+: L6 :+: CNil,
        BasisRest,
      ],
      unique: Unique.Aux[
        L0Out :+: L1Out :+: L2Out :+: L3Out :+: L4Out :+: L5Out :+: L6Out :+: BasisRest,
        UniqueOut,
      ],
      errorTrans: ErrorTrans[F, L, R],
    ): F[UniqueOut, R] = {
      errorTrans.transformError(in) { err =>
        unique.apply {
          basis(err) match {
            case Left(rest) =>
              rest.extendLeftBy[
                L0Out :+: L1Out :+: L2Out :+: L3Out :+: L4Out :+: L5Out :+: L6Out :+: CNil,
              ]
            case Right(extracted) =>
              type Inter =
                L0Out :+: L1Out :+: L2Out :+: L3Out :+: L4Out :+: L5Out :+: L6Out :+: BasisRest
              extracted match {
                case Inl(x)                     => Coproduct[Inter](func0(x))
                case Inr(Inl(x))                => Coproduct[Inter](func1(x))
                case Inr(Inr(Inl(x)))           => Coproduct[Inter](func2(x))
                case Inr(Inr(Inr(Inl(x))))      => Coproduct[Inter](func3(x))
                case Inr(Inr(Inr(Inr(Inl(x))))) => Coproduct[Inter](func4(x))
                case Inr(Inr(Inr(Inr(Inr(Inl(x)))))) =>
                  Coproduct[Inter](func5(x))
                case Inr(Inr(Inr(Inr(Inr(Inr(Inl(x))))))) =>
                  Coproduct[Inter](func6(x))
                case Inr(Inr(Inr(Inr(Inr(Inr(Inr(cnil))))))) => cnil.impossible
              }
          }
        }
      }
    }

    def handle[L0, L1, Out, BasisRest <: Coproduct, UniqueOut <: Coproduct](
      func0: L0 => Out,
      func1: L1 => Out,
    )(
      implicit basis: Basis[L, L0 :+: L1 :+: CNil],
      toKnownCop: L =:= (L0 :+: L1 :+: CNil),
      errorTrans: ErrorTrans[F, L, R],
    ): F[Out, R] = {
      errorTrans.transformError(in) { err =>
        toKnownCop(err) match {
          case Inl(x)         => func0(x)
          case Inr(Inl(x))    => func1(x)
          case Inr(Inr(cnil)) => cnil.impossible
        }
      }
    }

    def handle[L0, L1, L2, Out, BasisRest <: Coproduct, UniqueOut <: Coproduct](
      func0: L0 => Out,
      func1: L1 => Out,
      func2: L2 => Out,
    )(
      implicit basis: Basis[L, L0 :+: L1 :+: L2 :+: CNil],
      toKnownCop: L =:= (L0 :+: L1 :+: L2 :+: CNil),
      errorTrans: ErrorTrans[F, L, R],
    ): F[Out, R] = {
      errorTrans.transformError(in) { err =>
        toKnownCop(err) match {
          case Inl(x)              => func0(x)
          case Inr(Inl(x))         => func1(x)
          case Inr(Inr(Inl(x)))    => func2(x)
          case Inr(Inr(Inr(cnil))) => cnil.impossible
        }
      }
    }

    def handle[L0, L1, L2, L3, Out, BasisRest <: Coproduct, UniqueOut <: Coproduct](
      func0: L0 => Out,
      func1: L1 => Out,
      func2: L2 => Out,
      func3: L3 => Out,
    )(
      implicit basis: Basis[L, L0 :+: L1 :+: L2 :+: L3 :+: CNil],
      toKnownCop: L =:= (L0 :+: L1 :+: L2 :+: L3 :+: CNil),
      errorTrans: ErrorTrans[F, L, R],
    ): F[Out, R] = {
      errorTrans.transformError(in) { err =>
        toKnownCop(err) match {
          case Inl(x)                   => func0(x)
          case Inr(Inl(x))              => func1(x)
          case Inr(Inr(Inl(x)))         => func2(x)
          case Inr(Inr(Inr(Inl(x))))    => func3(x)
          case Inr(Inr(Inr(Inr(cnil)))) => cnil.impossible
        }
      }
    }

    def handle[L0, L1, L2, L3, L4, Out, BasisRest <: Coproduct, UniqueOut <: Coproduct](
      func0: L0 => Out,
      func1: L1 => Out,
      func2: L2 => Out,
      func3: L3 => Out,
      func4: L4 => Out,
    )(
      implicit basis: Basis[L, L0 :+: L1 :+: L2 :+: L3 :+: L4 :+: CNil],
      toKnownCop: L =:= (L0 :+: L1 :+: L2 :+: L3 :+: L4 :+: CNil),
      errorTrans: ErrorTrans[F, L, R],
    ): F[Out, R] = {
      errorTrans.transformError(in) { err =>
        toKnownCop(err) match {
          case Inl(x)                        => func0(x)
          case Inr(Inl(x))                   => func1(x)
          case Inr(Inr(Inl(x)))              => func2(x)
          case Inr(Inr(Inr(Inl(x))))         => func3(x)
          case Inr(Inr(Inr(Inr(Inl(x)))))    => func4(x)
          case Inr(Inr(Inr(Inr(Inr(cnil))))) => cnil.impossible
        }
      }
    }

    def handle[L0, L1, L2, L3, L4, L5, Out, BasisRest <: Coproduct, UniqueOut <: Coproduct](
      func0: L0 => Out,
      func1: L1 => Out,
      func2: L2 => Out,
      func3: L3 => Out,
      func4: L4 => Out,
      func5: L5 => Out,
    )(
      implicit basis: Basis[L, L0 :+: L1 :+: L2 :+: L3 :+: L4 :+: L5 :+: CNil],
      toKnownCop: L =:= (L0 :+: L1 :+: L2 :+: L3 :+: L4 :+: L5 :+: CNil),
      errorTrans: ErrorTrans[F, L, R],
    ): F[Out, R] = {
      errorTrans.transformError(in) { err =>
        toKnownCop(err) match {
          case Inl(x)                             => func0(x)
          case Inr(Inl(x))                        => func1(x)
          case Inr(Inr(Inl(x)))                   => func2(x)
          case Inr(Inr(Inr(Inl(x))))              => func3(x)
          case Inr(Inr(Inr(Inr(Inl(x)))))         => func4(x)
          case Inr(Inr(Inr(Inr(Inr(Inl(x))))))    => func5(x)
          case Inr(Inr(Inr(Inr(Inr(Inr(cnil)))))) => cnil.impossible
        }
      }
    }

    def handle[L0, L1, L2, L3, L4, L5, L6, Out, BasisRest <: Coproduct, UniqueOut <: Coproduct](
      func0: L0 => Out,
      func1: L1 => Out,
      func2: L2 => Out,
      func3: L3 => Out,
      func4: L4 => Out,
      func5: L5 => Out,
      func6: L6 => Out,
    )(
      implicit basis: Basis[
        L,
        L0 :+: L1 :+: L2 :+: L3 :+: L4 :+: L5 :+: L6 :+: CNil,
      ],
      toKnownCop: L =:= (L0 :+: L1 :+: L2 :+: L3 :+: L4 :+: L5 :+: L6 :+: CNil),
      errorTrans: ErrorTrans[F, L, R],
    ): F[Out, R] = {
      errorTrans.transformError(in) { err =>
        toKnownCop(err) match {
          case Inl(x)                                  => func0(x)
          case Inr(Inl(x))                             => func1(x)
          case Inr(Inr(Inl(x)))                        => func2(x)
          case Inr(Inr(Inr(Inl(x))))                   => func3(x)
          case Inr(Inr(Inr(Inr(Inl(x)))))              => func4(x)
          case Inr(Inr(Inr(Inr(Inr(Inl(x))))))         => func5(x)
          case Inr(Inr(Inr(Inr(Inr(Inr(Inl(x)))))))    => func6(x)
          case Inr(Inr(Inr(Inr(Inr(Inr(Inr(cnil))))))) => cnil.impossible
        }
      }
    }

    def handle[L0, L1, L2, L3, L4, L5, L6, L7, Out, BasisRest <: Coproduct, UniqueOut <: Coproduct](
      func0: L0 => Out,
      func1: L1 => Out,
      func2: L2 => Out,
      func3: L3 => Out,
      func4: L4 => Out,
      func5: L5 => Out,
      func6: L6 => Out,
      func7: L7 => Out,
    )(
      implicit basis: Basis[
        L,
        L0 :+: L1 :+: L2 :+: L3 :+: L4 :+: L5 :+: L6 :+: L7 :+: CNil,
      ],
      toKnownCop: L =:= (L0 :+: L1 :+: L2 :+: L3 :+: L4 :+: L5 :+: L6 :+: L7 :+: CNil),
      errorTrans: ErrorTrans[F, L, R],
    ): F[Out, R] = {
      errorTrans.transformError(in) { err =>
        toKnownCop(err) match {
          case Inl(x)                                       => func0(x)
          case Inr(Inl(x))                                  => func1(x)
          case Inr(Inr(Inl(x)))                             => func2(x)
          case Inr(Inr(Inr(Inl(x))))                        => func3(x)
          case Inr(Inr(Inr(Inr(Inl(x)))))                   => func4(x)
          case Inr(Inr(Inr(Inr(Inr(Inl(x))))))              => func5(x)
          case Inr(Inr(Inr(Inr(Inr(Inr(Inl(x)))))))         => func6(x)
          case Inr(Inr(Inr(Inr(Inr(Inr(Inr(Inl(x))))))))    => func7(x)
          case Inr(Inr(Inr(Inr(Inr(Inr(Inr(Inr(cnil)))))))) => cnil.impossible
        }
      }
    }

    def handle[
      L0,
      L1,
      L2,
      L3,
      L4,
      L5,
      L6,
      L7,
      L8,
      Out,
      BasisRest <: Coproduct,
      UniqueOut <: Coproduct,
    ](
      func0: L0 => Out,
      func1: L1 => Out,
      func2: L2 => Out,
      func3: L3 => Out,
      func4: L4 => Out,
      func5: L5 => Out,
      func6: L6 => Out,
      func7: L7 => Out,
      func8: L8 => Out,
    )(
      implicit basis: Basis[
        L,
        L0 :+: L1 :+: L2 :+: L3 :+: L4 :+: L5 :+: L6 :+: L7 :+: L8 :+: CNil,
      ],
      toKnownCop: L =:= (L0 :+: L1 :+: L2 :+: L3 :+: L4 :+: L5 :+: L6 :+: L7 :+: L8 :+: CNil),
      errorTrans: ErrorTrans[F, L, R],
    ): F[Out, R] = {
      errorTrans.transformError(in) { err =>
        toKnownCop(err) match {
          case Inl(x)                                         => func0(x)
          case Inr(Inl(x))                                    => func1(x)
          case Inr(Inr(Inl(x)))                               => func2(x)
          case Inr(Inr(Inr(Inl(x))))                          => func3(x)
          case Inr(Inr(Inr(Inr(Inl(x)))))                     => func4(x)
          case Inr(Inr(Inr(Inr(Inr(Inl(x))))))                => func5(x)
          case Inr(Inr(Inr(Inr(Inr(Inr(Inl(x)))))))           => func6(x)
          case Inr(Inr(Inr(Inr(Inr(Inr(Inr(Inl(x))))))))      => func7(x)
          case Inr(Inr(Inr(Inr(Inr(Inr(Inr(Inr(Inl(x))))))))) => func8(x)
          case Inr(Inr(Inr(Inr(Inr(Inr(Inr(Inr(Inr(cnil))))))))) =>
            cnil.impossible
        }
      }
    }

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
