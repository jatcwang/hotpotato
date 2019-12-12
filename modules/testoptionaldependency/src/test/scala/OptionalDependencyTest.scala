import org.scalatest.wordspec.AnyWordSpec
import hotpotato._
import cats.data.EitherT
import cats.implicits._
import cats.effect.{IO => CatsIO}
import zio.{IO         => ZIOIO}

class OptionalDependencyTest extends AnyWordSpec {
  "Cats EitherT" should {
    "have ErrorTrans instance" in {
      implicitly[ErrorTrans[EitherT[Option, *, *]]]
    }

    "have ErrorTransThrow instance" in {
      implicitly[ErrorTransThrow[EitherT[CatsIO, *, *]]]
    }
  }

  "ZIO IO" should {
    "have ErrorTrans instance" in {
      implicitly[ErrorTrans[ZIOIO]]
    }

    "have ErrorTransThrow instance" in {
      implicitly[ErrorTransThrow[ZIOIO]]
    }
  }

}
