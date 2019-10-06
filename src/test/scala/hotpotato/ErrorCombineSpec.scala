package hotpotato

import Examples.{E1_E2_E3, _}
import org.scalatest.{FreeSpec, Matchers}
import ErrorTrans._
import shapeless.ops.coproduct.Basis
import shapeless.{:+:, Coproduct}
import cats.implicits._

class ErrorCombineSpec extends FreeSpec with Matchers {

  "Error combines" in {
    implicit val embedder: Embedder[E1_E2_E3_E4] = Embedder.make

    val result = for {
      _ <- func_E1_E2.embedError
      _ <- func_E2_E3.embedError
      _ <- func_E4.embedError
    } yield ()

    result shouldBe Right("")

  }

  "flatmap combines error using wrapper" in {

    func_E1_E2_E3.wrap.flatMap(_ => func_E2_E3.wrap)

    func_E1_E2.wrap.flatMap(_ => func_E1_E2_E3.wrap)

  }

}
