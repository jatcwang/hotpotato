package hotpotato

import shapeless._
import Examples._
import PureExamples._
import org.scalatest.{FreeSpec, Matchers}
import ErrorTrans._
import shapeless.ops.coproduct.Basis
import cats.implicits._
import hotpotato.coproduct.SameElem

class ErrorCombineSpec extends FreeSpec with Matchers {

  "Error combines" in {
    implicit val embedder: Embedder[E1_E2_E3_E4] = Embedder.make

    val result = for {
      _ <- g_E12.embedError
      _ <- g_E23.embedError
      _ <- g_E4.embedError
    } yield ()

    result shouldBe Right(())

  }

  "SameElem" in {
    import shapeless.ops.coproduct._
    import SameElem._

    implicitly[SameElem[E1 :+: CNil, E1 :+: CNil]]
    implicitly[SameElem[E1_E2_E3_E4, E1_E2_E3_E4]]
    implicitly[SameElem[CNil, CNil]]

    implicitly[SameElem[E2 :+: E1 :+: CNil, E1 :+: E2 :+: E1 :+: CNil]]

  }

}
