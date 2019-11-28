package hotpotato

import hotpotato.Examples._
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import shapeless._
import shapeless.syntax.inject._
import cats.implicits._
import hotpotato.util.AssertType

class ErrorHandlingSpec extends AnyWordSpec with Matchers {
  import ErrorTrans._
  implicit val embedder: Embedder[E123] = Embedder.make

  "mapErrorAllInto should handle all coproduct cases into a single type" in {
    // Exhaustive error handling
    def exec(either: Either[E123, Unit]): Either[String, Unit] =
      either.mapErrorAllInto(
        (_: E1) => "e1",
        (_: E2) => "e2",
        (_: E3) => "e3",
      )

    exec(Left(Coproduct[E123](E1()))) shouldBe Left("e1")
    exec(Left(Coproduct[E123](E2()))) shouldBe Left("e2")
    exec(Left(Coproduct[E123](E3()))) shouldBe Left("e3")

  }

  "mapErrorSome" should {

    "handle some cases (each into a different type), with the result type being the unique combination of all types + the unhandled cases" in {
      type ResultError = String :+: Int :+: E1 :+: CNil

      def exec(either: Either[E123, Unit]): Either[ResultError, Unit] =
        either.mapErrorSome(
          (_: E3) => "e3",
          (_: E2) => 0,
        )

      exec(Left(Coproduct[E123](E1()))) shouldBe Left(Coproduct[ResultError](E1()))
      exec(Left(Coproduct[E123](E2()))) shouldBe Left(Coproduct[ResultError](0))
      exec(Left(Coproduct[E123](E3()))) shouldBe Left(Coproduct[ResultError]("e3"))
    }

    "handle some cases (each into a different type), with deduplication of output types" in {
      type ResultError = String :+: Int :+: E1 :+: CNil

      def exec(either: Either[E1_E2_E3_E4, Unit]): Either[String :+: Int :+: E1 :+: CNil, Unit] =
        either.mapErrorSome(
          (_: E3) => "e3",
          (_: E2) => "e2",
          (_: E4) => 0,
        )

      exec(Left(Coproduct[E1_E2_E3_E4](E1()))) shouldBe Left(Coproduct[ResultError](E1()))
      exec(Left(Coproduct[E1_E2_E3_E4](E2()))) shouldBe Left(Coproduct[ResultError]("e2"))
      exec(Left(Coproduct[E1_E2_E3_E4](E3()))) shouldBe Left(Coproduct[ResultError]("e3"))
      exec(Left(Coproduct[E1_E2_E3_E4](E4()))) shouldBe Left(Coproduct[ResultError](0))
    }

    "does not modify the success result" in {
      import PureExamples._
      AssertType(
        g_E123.mapErrorSome(
          (_: E1) => "1",
          (_: E2) => 2,
        ),
      ).is[Either[String :+: Int :+: E3 :+: CNil, String]] shouldBe Right("")
    }

  }

  "mapErrorSomeAdt" should {
    "Handle only some cases from a sealed trait (each into a different type). Unhandled cases will appear in the result coproduct" in {
      type ResultError = String :+: Int :+: Child3 :+: CNil

      def exec(err: Either[Sealed, Unit]): Either[ResultError, Unit] =
        err.mapErrorSomeAdt(
          (_: Child1) => "child1",
          (_: Child2) => 0,
        )

      exec(Left(Child1())) shouldBe Left(Coproduct[ResultError]("child1"))
      exec(Left(Child2())) shouldBe Left(Coproduct[ResultError](0))
      exec(Left(Child3())) shouldBe Left(Coproduct[ResultError](Child3()))
    }

    "Handle ony some cases of a sealed trait into one type. Unhandled cases will appear in the final ADT" in {
      type ResultError = String :+: Child3 :+: CNil

      def exec(err: Either[Sealed, Unit]): Either[ResultError, Unit] =
        err.mapErrorSomeAdt(
          (_: Child1) => "child1",
          (_: Child2) => "child2",
        )

      exec(Left(Child1())) shouldBe Left(Coproduct[ResultError]("child1"))
      exec(Left(Child2())) shouldBe Left(Coproduct[ResultError]("child2"))
      exec(Left(Child3())) shouldBe Left(Coproduct[ResultError](Child3()))
    }
  }

  "mapErrorAllInto" should {
    import PureExamples._

    "map a single error into one type" in {
      (Left(e1.inject): Either[E1 :+: CNil, String]).mapErrorAllInto(
        (_: E1) => "e1",
      ) shouldBe Left("e1")
    }

    "map error all into one type" in {

      def exec(either: Either[E123, String]) =
        either.mapErrorAllInto(
          (_: E1) => "e1",
          (_: E2) => "e2",
          (_: E3) => "e3",
        )
      exec(b_E123_1) shouldBe Left("e1")
      exec(b_E123_2) shouldBe Left("e2")
      exec(b_E123_3) shouldBe Left("e3")
    }

    "does not modify the success result" in {
      g_E123.mapErrorAllInto(
        (_: E1) => 1,
        (_: E2) => 2,
        (_: E3) => 3,
      ) shouldBe Right("")
    }

  }

  "mapErrorAll" should {
    import PureExamples._
    "Allow each error type to map to a different error type, with the result being the unique combination of them" in {
      type ExpectedOut = String :+: Int :+: Right[Nothing, Int] :+: CNil
      def exec(either: Either[E1234, String]): Either[ExpectedOut, String] =
        either.mapErrorAll(
          (_: E1) => "e1",
          (_: E2) => 2,
          (_: E3) => 3,
          (_: E4) => Right(1),
        )

      exec(b_E1234_1) shouldBe Left("e1".inject[ExpectedOut])
      exec(b_E1234_2) shouldBe Left(2.inject[ExpectedOut])
      exec(b_E1234_3) shouldBe Left(3.inject[ExpectedOut])
      exec(b_E1234_4) shouldBe Left(Right(1).inject[ExpectedOut])
    }

    "mapping a single error" in {
      (Left(e1.inject): Either[E1 :+: CNil, String]).mapErrorAll(
        e1 => "e1",
      ) shouldBe Left("e1".inject[String :+: CNil])
    }

    "does not modify the success result" in {
      g_E123.mapErrorAll(
        (_: E1) => "e1",
        (_: E2) => 2,
        (_: E3) => 3,
      ) shouldBe Right("")
    }
  }

  "flatMapErrorAllInto" should {
    import PureExamples._
    "use the successful result if the recovery succeeds" in {
      def exec(either: Either[E123, String]): Either[Unit, String] =
        AssertType {
          either.flatMapErrorAllInto(
            (_: E1) => "1".asRight[Unit],
            (_: E2) => "2".asRight[Unit],
            (_: E3) => "3".asRight[Unit],
          )
        }.is[Either[Unit, String]]

      exec(b_E123_1) shouldBe Right("1")
      exec(b_E123_2) shouldBe Right("2")
      exec(b_E123_3) shouldBe Right("3")
    }

    "use error returned from recovery effect if failed" in {
      def exec(either: Either[E123, String]): Either[Int, String] =
        AssertType {
          either.flatMapErrorAllInto(
            (_: E1) => 1.asLeft[String],
            (_: E2) => 2.asLeft[String],
            (_: E3) => 3.asLeft[String],
          )
        }.is[Either[Int, String]]

      exec(b_E123_1) shouldBe Left(1)
      exec(b_E123_2) shouldBe Left(2)
      exec(b_E123_3) shouldBe Left(3)
    }

    "does not modify the success result" in {
      g_E123.flatMapErrorAllInto(
        (_: E1) => 1.asLeft[String],
        (_: E2) => 2.asLeft[String],
        (_: E3) => 3.asLeft[String],
      ) shouldBe Right("")
    }

  }

  "flatMapErrorAll" should {
    import PureExamples._
    "Allow each error type to map to a different error type, with the result error being the unique combination of them" in {
      type ExpectedError = String :+: Int :+: Boolean :+: CNil
      def exec(either: Either[E123, String]): Either[ExpectedError, String] =
        AssertType {
          either.flatMapErrorAll(
            (_: E1) => "1".asLeft[String],
            (_: E2) => 2.asLeft[String],
            (_: E3) => Left(true),
          )
        }.is[Either[ExpectedError, String]]

      exec(b_E123_1) shouldBe Left("1".inject[ExpectedError])
      exec(b_E123_2) shouldBe Left(2.inject[ExpectedError])
      exec(b_E123_3) shouldBe Left(true.inject[ExpectedError])
    }

    "use error returned from recovery effect if failed" in {
      type ExpectedError = String :+: Int :+: Boolean :+: CNil
      def exec(either: Either[E123, String]) =
        AssertType {
          either.flatMapErrorAll(
            (_: E1) => "1".asLeft[String],
            (_: E2) => 2.asLeft[String],
            (_: E3) => true.asLeft[String],
          )
        }.is[Either[ExpectedError, String]]

      exec(b_E123_1) shouldBe Left("1".inject[ExpectedError])
      exec(b_E123_2) shouldBe Left(2.inject[ExpectedError])
      exec(b_E123_3) shouldBe Left(true.inject[ExpectedError])
    }

    "does not modify the success result" in {
      g_E123.flatMapErrorAll(
        (_: E1) => "1".asLeft[String],
        (_: E2) => 2.asLeft[String],
        (_: E3) => Left(true),
      ) shouldBe Right("")
    }
  }

  "flatMapErrorSome" should {
    import PureExamples._

    "Allow each error type to map to a different error type, with the result error being the unique combination of them" in {
      type ExpectedError = String :+: Int :+: E3 :+: CNil
      def exec(either: Either[E123, String]): Either[ExpectedError, String] =
        AssertType {
          either.flatMapErrorSome(
            (_: E1) => "1".asLeft[String],
            (_: E2) => 2.asLeft[String],
          )
        }.is[Either[ExpectedError, String]]

      exec(b_E123_1) shouldBe Left("1".inject[ExpectedError])
      exec(b_E123_2) shouldBe Left(2.inject[ExpectedError])
      exec(b_E123_3) shouldBe Left(e3.inject[ExpectedError])
    }

    "use error returned from recovery effect if failed" in {
      type ExpectedError = String :+: Int :+: E3 :+: CNil
      def exec(either: Either[E123, String]) =
        AssertType {
          either.flatMapErrorSome(
            (_: E1) => "1".asLeft[String],
            (_: E2) => 2.asLeft[String],
          )
        }.is[Either[ExpectedError, String]]

      exec(b_E123_1) shouldBe Left("1".inject[ExpectedError])
      exec(b_E123_2) shouldBe Left(2.inject[ExpectedError])
      exec(b_E123_3) shouldBe Left(e3.inject[ExpectedError])
    }

    "does not modify the success result" in {
      g_E123.flatMapErrorSome(
        (_: E1) => "1".asLeft[String],
        (_: E2) => 2.asLeft[String],
      ) shouldBe Right("")
    }
  }

}
