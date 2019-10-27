package hotpotato
import shapeless._
import shapeless.ops.coproduct.Inject

object Examples {

  val MSG: String = "MSG"
  val MSG_4: String = "MSG_4"
  val MSG_8: String = "MSG_8"

  // Some error types which we will freely combine in our coproduct type
  sealed trait AllErrors extends Serializable
  final case class E1() extends AllErrors
  final case class E2() extends AllErrors
  final case class E3() extends AllErrors
  final case class E4() extends AllErrors
  final case class E5() extends AllErrors
  final case class E6() extends AllErrors
  final case class E7() extends AllErrors
  final case class E8() extends AllErrors

  type E1_E2_E3 = E1 :+: E2 :+: E3 :+: CNil
  type E3_E4_E1 = E3 :+: E4 :+: E1 :+: CNil
  type E1_E2 = E1 :+: E2 :+: CNil
  type E2_E3 = E2 :+: E3 :+: CNil
  type E3_E4 = E3 :+: E4 :+: CNil
  type E2_E3_E4 = E2 :+: E3 :+: E4 :+: CNil
  type E1_E2_E3_E4 = E1 :+: E2 :+: E3 :+: E4 :+: CNil
  type E1to8 = E1 :+: E2 :+: E3 :+: E4 :+: E5 :+: E6 :+: E7 :+: E8 :+: CNil
  type E4_E2_E3_E1 = E4 :+: E3 :+: E2 :+: E1 :+: CNil

  val e1: E1 = E1()
  val e2: E2 = E2()
  val e3: E3 = E3()
  val e4: E4 = E4()
  val e5: E5 = E5()
  val e6: E6 = E6()
  val e7: E7 = E7()
  val e8: E8 = E8()

  def func_E1: Either[E1, String] = Right("")
  def func_E1_E2: Either[E1 :+: E2 :+: CNil, String] = Right("")
  def func_E1_E2_E3: Either[E1 :+: E2 :+: E3 :+: CNil, String] = Right("")
  def func_E1_E2_E3_E4: Either[E1 :+: E2 :+: E3 :+: E4 :+: CNil, String] = Right("")
  def func_E1_E3: Either[E1 :+: E2 :+: CNil, String] = Right("")
  def func_E2_E3: Either[E2 :+: E3 :+: CNil, String] = Right("")
  def func_E3_E4: Either[E3_E4, String] = Right("")
  def func_E4: Either[E4, String] = Right("")

  import zio.IO
  def g_E1: IO[E1, String] = IO.succeed("")
  def g_E1_E2: IO[Err2[E1, E2], String] = IO.succeed("")
  def g_E1_E2_E3: IO[Err3[E1, E2, E3], String] = IO.succeed("")
  def g_E1_E2_E3_E4: IO[Err4[E1, E2, E3, E4], String] = IO.succeed("")

  val b_allError_1: IO[AllErrors, String] = IO.fail(e1)
  val b_allError_4: IO[AllErrors, String] = IO.fail(e4)
  val b_allError_8: IO[AllErrors, String] = IO.fail(e8)
  val b_E1: IO[E1, String] = IO.fail(e1)
  val b_E2: IO[E2, String] = IO.fail(e2)
  val b_E3: IO[E3, String] = IO.fail(e3)
  val b_E4: IO[E4, String] = IO.fail(e4)
  val b_E1234_4: IO[E1 :+: E2 :+: E3 :+: E4 :+: CNil, String] = IO.fail(Inject[E1_E2_E3_E4, E4].apply(e4))
  val b_E1234_1: IO[E1 :+: E2 :+: E3 :+: E4 :+: CNil, String] = IO.fail(Inject[E1_E2_E3_E4, E1].apply(e1))
  val b_E1to8_1: IO[E1to8, String] = IO.fail(Inject[E1to8, E1].apply(e1))
  val b_E1to8_8: IO[E1to8, String] = IO.fail(Inject[E1to8, E8].apply(e8))
  val b_E34: IO[E3 :+: E4 :+: CNil, String] = IO.fail(Inr(Inl(e4)))

  type Err1[E1] = E1 :+: Throwable :+: CNil
  type Err2[E1, E2] = E1 :+: E2 :+: CNil
  type Err3[E1, E2, E3] = E1 :+: E2 :+: E3 :+: CNil
  type Err4[E1, E2, E3, E4] = E1 :+: E2 :+: E3 :+: E4 :+: CNil

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
