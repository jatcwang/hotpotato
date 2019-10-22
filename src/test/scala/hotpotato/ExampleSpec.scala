package hotpotato

import cats.data.EitherT
import cats.implicits._
import org.scalatest.{Assertion, AsyncWordSpec, WordSpec}
import cats.effect._
import Examples._
import ErrorTrans._
import zio.{DefaultRuntime, ZIO}

class ExampleSpec extends WordSpec {
  val runtime = new DefaultRuntime {}

  "fixme" in {
    implicit val embedder: Embedder[Err4[E1, E2, E3, E4]] = Embedder.make[Err4[E1, E2, E3, E4]]

    val v: ZIO[Any, Err4[E1, E2, E3, E4], Assertion] = for {
      _ <- zio_E1.embedError
      _ <- zio_E1_E2.embedError
    } yield succeed

  }
}
