package hotpotato

import hotpotato.ErrorTrans._
import hotpotato.Examples._
import hotpotato.PureExamples._
import org.scalatest.{Matchers, WordSpec}
import shapeless.syntax.inject._

class ErrorEmbedSpec extends WordSpec with Matchers {

  "Error embedding" should {
    "Embed/combine errors in the success case" in {
      implicit val embedder: Embedder[E1_E2_E3_E4] = Embedder.make

      val result = for {
        _ <- g_E12.embedError
        _ <- g_E23.embedError
        _ <- g_E4.embedError
      } yield ()

      result shouldBe Right(())
    }
  }

  "Embedding errors preserves the error that occured" in {
    implicit val embedder: Embedder[E1_E2_E3_E4] = Embedder.make

    def withError(
      e1: Option[E1] = None,
      e2: Option[E2] = None,
      e3: Option[E3] = None,
      e4: Option[E4] = None,
    ) = {
      for {
        _ <- e1.toLeft(()).embedError
        _ <- e2.toLeft(()).embedError
        _ <- e3.toLeft(()).embedError
        _ <- e4.toLeft(()).embedError
      } yield ()
    }

    withError(e1 = Some(e1)) shouldBe Left(e1.inject[E1_E2_E3_E4])
    withError(e2 = Some(e2)) shouldBe Left(e2.inject[E1_E2_E3_E4])
    withError(e3 = Some(e3)) shouldBe Left(e3.inject[E1_E2_E3_E4])
    withError(e4 = Some(e4)) shouldBe Left(e4.inject[E1_E2_E3_E4])
  }

}
