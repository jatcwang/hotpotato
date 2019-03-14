import shapeless._

import scala.language.higherKinds

object Main {

  // Some error types which we will freely combine in our coproduct type
  case class E1() extends Exception("e1")
  case class E2() extends Exception("e2")
  case class E3() extends Exception("e3")

  type E1_E2_E3 = E1 :+: E2 :+: E3 :+: CNil

  def func_E1_E2: Either[E1 :+: E2 :+: CNil, String] = ???
  def func_E2_E3: Either[E2 :+: E3 :+: CNil, String] = ???
  def func_E1: Either[E1, String] = ???

  // Another layer of error which our layer 1 errors may need to unify into
  // (e.g. hiding internal errors)
  type F1_F2 = F1 :+: F2 :+: CNil
  case class F1(msg: String) extends Exception(msg)
  case class F2(msg: String) extends Exception(msg)

  implicit val embedder: Embedder[E1_E2_E3] = Embedder.make[E1_E2_E3]
  import ErrorTrans._

  // Exhaustive error handling
  val handleAllIntoOne: Either[String, Unit] = (for {
    _ <- func_E1.embedError
    _ <- func_E1_E2.embedError
    _ <- func_E2_E3.embedError
  } yield ()).handle(
    (s: E1) => s.toString,
    (s: E2) => s.toString,
    (s: E3) => s.toString
  )

  // Partial error handling
  val handleOnlySomeCases: Either[String :+: Int :+: E1 :+: CNil, Unit] = (for {
    _ <- func_E1_E2.embedError
    _ <- func_E2_E3.embedError
  } yield ()).handleSome(
    (e: E2) => e.toString,
    (e: E3) => 12,
  )

}

