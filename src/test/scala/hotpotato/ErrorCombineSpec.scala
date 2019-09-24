package hotpotato

import hotpotato.Examples.E1_E2_E3
import org.scalatest.{FreeSpec, Matchers}
import Examples._
import ErrorTrans._
import shapeless.:+:

class ErrorCombineSpec extends FreeSpec with Matchers {

  "Error combines" in {
    implicit val embedder: Embedder[E1_E2_E3_E4] = Embedder.make

    val result = for {
      _ <- func_E1_E2.embedError
      _ <- func_E2_E3.embedError
      _ <- func_E2_E3.embedError
    } yield ()

    result shouldBe Right("")

  }

}
