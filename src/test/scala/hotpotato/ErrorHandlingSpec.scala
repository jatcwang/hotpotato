package hotpotato

import org.scalatest.{FreeSpec, Matchers}
import shapeless._
import ErrorHandlingSpec._

//TODOO: handleTo[SomeType] => handle all case into a particular type
class ErrorHandlingSpec extends FreeSpec with Matchers {
  import ErrorTrans._
  implicit val embedder: Embedder[E1_E2_E3] = Embedder.make

  "handle all coproduct cases into a single type" in {
    // Exhaustive error handling
    def exec(either: Either[E1_E2_E3, Unit]): Either[String, Unit] =
      either.handle(
        (s: E1) => "e1",
        (s: E2) => "e2",
        (s: E3) => "e3",
      )

    exec(Left(Coproduct[E1_E2_E3](E1()))) shouldBe Left("e1")
    exec(Left(Coproduct[E1_E2_E3](E2()))) shouldBe Left("e2")
    exec(Left(Coproduct[E1_E2_E3](E3()))) shouldBe Left("e3")

  }

  "handle some cases in a coproduct, resulting in a coproduct consisting of converted types plus unhandled cases" in {
    type ResultError = String :+: Int :+: E1 :+: CNil

    def exec(either: Either[E1_E2_E3, Unit]): Either[ResultError, Unit] =
      either.handleSome(
        (e: E3) => "e3",
        (e: E2) => 0,
      )

    exec(Left(Coproduct[E1_E2_E3](E3()))) shouldBe Left(Coproduct[ResultError]("e3"))
    exec(Left(Coproduct[E1_E2_E3](E2()))) shouldBe Left(Coproduct[ResultError](0))
    exec(Left(Coproduct[E1_E2_E3](E1()))) shouldBe Left(Coproduct[ResultError](E1()))
  }

  "Handle only some cases from a sealed trait. Unhandled cases will appear in the result coproduct" in {
    type ResultError = String :+: Int :+: Child3 :+: CNil

    def exec(err: Either[Sealed, Unit]): Either[ResultError, Unit] = {
      err.handleSomeAdt(
        (c: Child1) => "child1",
        (c: Child2) => 0,
      )
    }

    exec(Left(Child1())) shouldBe Left(Coproduct[ResultError]("child1"))
    exec(Left(Child2())) shouldBe Left(Coproduct[ResultError](0))
    exec(Left(Child3())) shouldBe Left(Coproduct[ResultError](Child3()))
  }
}

object ErrorHandlingSpec {

  // Some error types which we will freely combine in our coproduct type
  final case class E1() extends Exception("e1")
  final case class E2() extends Exception("e2")
  final case class E3() extends Exception("e3")

  type E1_E2_E3 = E1 :+: E2 :+: E3 :+: CNil

  def func_E1_E2: Either[E1 :+: E2 :+: CNil, String] = Right("")
  def func_E2_E3: Either[E2 :+: E3 :+: CNil, String] = Right("")
  def func_E1: Either[E1, String]                    = Right("")

  // Another layer of error which our layer 1 errors may need to unify into
  // (e.g. hiding internal errors)
  type F1_F2 = F1 :+: F2 :+: CNil
  case class F1(msg: String) extends Exception(msg)
  case class F2(msg: String) extends Exception(msg)

  sealed trait Sealed
  final case class Child1() extends Sealed
  final case class Child2() extends Sealed
  final case class Child3() extends Sealed

}
