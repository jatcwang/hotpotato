package hotpotato

import shapeless._
import Examples.{E1_E2_E3, E1_E2_E3_E4, _}
import org.scalatest.{FreeSpec, Matchers}
import ErrorTrans._
import shapeless.ops.coproduct.Basis
import cats.implicits._
import hotpotato.coproduct.SameElem

class ErrorCombineSpec extends FreeSpec with Matchers {

  "Error combines" in {
    implicit val embedder: Embedder[E1_E2_E3_E4] = Embedder.make

    val result = for {
      _ <- func_E1_E2.embedError
      _ <- func_E2_E3.embedError
      _ <- func_E4.embedError
    } yield ()

    result shouldBe Right(())

  }

  "tno" in {
    import shapeless.ops.coproduct._
    import SameElem._

    implicitly[SameElem[E1 :+: CNil, E1 :+: CNil]]
    implicitly[SameElem[E1_E2_E3_E4, E1_E2_E3_E4]]
    implicitly[SameElem[CNil, CNil]]

    implicitly[SameElem[E2 :+: E1 :+: CNil, E1 :+: E2 :+: E1 :+: CNil]]

  }

  "flatmap combines error using wrapper" in {

    val vv: Either[E1_E2_E3_E4, Unit] = (for {
      _ <- func_E1_E2.wrapC[E1_E2_E3_E4]
      _ <- func_E3_E4.wrap
    } yield ()).unwrap

  }

}
