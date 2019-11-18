package hotpotato

import hotpotato.Examples._
import org.scalatest.{Matchers, WordSpec}
import shapeless._

class ErrorHandlingSpec extends WordSpec with Matchers {
  import ErrorTrans._
  implicit val embedder: Embedder[E1_E2_E3] = Embedder.make

  "handleX should handle all coproduct cases into a single type" in {
    // Exhaustive error handling
    def exec(either: Either[E1_E2_E3, Unit]): Either[String, Unit] =
      either.handle3(
        (s: E1) => "e1",
        (s: E2) => "e2",
        (s: E3) => "e3",
      )

    exec(Left(Coproduct[E1_E2_E3](E1()))) shouldBe Left("e1")
    exec(Left(Coproduct[E1_E2_E3](E2()))) shouldBe Left("e2")
    exec(Left(Coproduct[E1_E2_E3](E3()))) shouldBe Left("e3")

  }

  "handleSomeX" should {

    "handle some cases (each into a different type), with the result type being the unique combination of all types + the unhandled cases" in {
      type ResultError = String :+: Int :+: E1 :+: CNil

      def exec(either: Either[E1_E2_E3, Unit]): Either[ResultError, Unit] =
        either.handleSome(
          (e: E3) => "e3",
          (e: E2) => 0,
        )

      exec(Left(Coproduct[E1_E2_E3](E1()))) shouldBe Left(Coproduct[ResultError](E1()))
      exec(Left(Coproduct[E1_E2_E3](E2()))) shouldBe Left(Coproduct[ResultError](0))
      exec(Left(Coproduct[E1_E2_E3](E3()))) shouldBe Left(Coproduct[ResultError]("e3"))
    }

    "handle some cases (each into a different type), with deduplication of output types" in {
      type ResultError = String :+: Int :+: E1 :+: CNil

      def exec(either: Either[E1_E2_E3_E4, Unit]): Either[String :+: Int :+: E1 :+: CNil, Unit] =
        either.handleSome(
          (e: E3) => "e3",
          (e: E2) => "e2",
          (e: E4) => 0,
        )

      exec(Left(Coproduct[E1_E2_E3_E4](E1()))) shouldBe Left(Coproduct[ResultError](E1()))
      exec(Left(Coproduct[E1_E2_E3_E4](E2()))) shouldBe Left(Coproduct[ResultError]("e2"))
      exec(Left(Coproduct[E1_E2_E3_E4](E3()))) shouldBe Left(Coproduct[ResultError]("e3"))
      exec(Left(Coproduct[E1_E2_E3_E4](E4()))) shouldBe Left(Coproduct[ResultError](0))
    }

  }

  "Handle only some cases from a sealed trait (each into a different type). Unhandled cases will appear in the result coproduct" in {
    type ResultError = String :+: Int :+: Child3 :+: CNil

    def exec(err: Either[Sealed, Unit]): Either[ResultError, Unit] =
      err.handleSomeAdt(
        (c: Child1) => "child1",
        (c: Child2) => 0,
      )

    exec(Left(Child1())) shouldBe Left(Coproduct[ResultError]("child1"))
    exec(Left(Child2())) shouldBe Left(Coproduct[ResultError](0))
    exec(Left(Child3())) shouldBe Left(Coproduct[ResultError](Child3()))
  }

  "Handle ony some cases of a sealed trait into one type. Unhandled cases will appear in the final ADT" in {
    type ResultError = String :+: Child3 :+: CNil
    def exec(err: Either[Sealed, Unit]): Either[ResultError, Unit] =
      err.handleSomeAdtInto(
        (c: Child1) => "child1",
        (c: Child2) => "child2",
      )

    exec(Left(Child1())) shouldBe Left(Coproduct[ResultError]("child1"))
    exec(Left(Child2())) shouldBe Left(Coproduct[ResultError]("child2"))
    exec(Left(Child3())) shouldBe Left(Coproduct[ResultError](Child3()))
  }
}
