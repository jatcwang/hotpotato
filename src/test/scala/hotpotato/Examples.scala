package hotpotato
import shapeless._

object Examples {


  // Some error types which we will freely combine in our coproduct type
  final case class E1() extends Exception("e1")
  final case class E2() extends Exception("e2")
  final case class E3() extends Exception("e3")
  final case class E4() extends Exception("e4")

  type E1_E2_E3 = E1 :+: E2 :+: E3 :+: CNil
  type E1_E2 = E1 :+: E2 :+: CNil
  type E1_E2_E3_E4 = E1 :+: E2 :+: E3 :+: E4 :+: CNil

  def func_E1: Either[E1, String] = Right("")
  def func_E1_E2: Either[E1 :+: E2 :+: CNil, String] = Right("")
  def func_E1_E2_E3: Either[E1 :+: E2 :+: E3 :+: CNil, String] = Right("")
  def func_E1_E2_E3_E4: Either[E1 :+: E2 :+: E3 :+: E4 :+: CNil, String] = Right("")
  def func_E1_E3: Either[E1 :+: E2 :+: CNil, String] = Right("")
  def func_E2_E3: Either[E2 :+: E3 :+: CNil, String] = Right("")
  def func_E4: Either[E4, String] = Right("")

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
