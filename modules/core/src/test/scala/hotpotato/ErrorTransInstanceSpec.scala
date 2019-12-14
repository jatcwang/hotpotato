package hotpotato

import cats.Eq
import cats.data.EitherT
import cats.implicits._
import cats.laws.discipline.arbitrary._
import hotpotato.ErrorTransInstanceSpec.SimpleError
import hotpotato.laws.{ErrorTransTests, ErrorTransThrowTests}
import org.scalacheck.Arbitrary
import org.scalatest.funsuite.AnyFunSuite
import org.typelevel.discipline.scalatest.Discipline

class ErrorTransInstanceSpec extends AnyFunSuite with Discipline {

  checkAll(
    "Either.ErrorTransLaws",
    ErrorTransTests[Either[*, *]].errorTrans[Int, Int, Int, String],
  )

  checkAll(
    "EitherT.ErrorTransLaws",
    ErrorTransTests[EitherT[Option, *, *]].errorTrans[Int, Int, Int, String],
  )

  // Cheat a little bit by only generating only one type of throwable
  private implicit val arbThrowable: Arbitrary[Throwable] = Arbitrary(
    Arbitrary.arbitrary[String].map(SimpleError).map(x => x: Throwable),
  )
  private implicit val eqThrowable: Eq[Throwable] = new Eq[Throwable] {
    override def eqv(x: Throwable, y: Throwable): Boolean = x == y
  }

  checkAll(
    "EitherT.ErrorTransThrowLaws",
    ErrorTransThrowTests[EitherT[Either[Throwable, *], *, *]]
      .errorTransThrow[Int, Int, Int, String],
  )
}

object ErrorTransInstanceSpec {
  final case class SimpleError(message: String) extends Exception(message)
}
