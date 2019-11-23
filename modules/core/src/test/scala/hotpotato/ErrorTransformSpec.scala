package hotpotato

import hotpotato.ErrorTrans._
import hotpotato.Examples._
import org.scalatest.{Inside, Matchers, WordSpec}
import zio.{Cause, Exit}
import shapeless.syntax.inject._

class ErrorTransformSpec extends WordSpec with Matchers with Inside {
  "unifyError" should {
    "unify the error into one" in {
      import hotpotato.PureExamples._
      inside(b_E12_1.unifyError) {
        case Left(E1()) => succeed
      }
    }
  }

  "dieIf" should {
    "dieIf throws the case as the exception (one type param)" in {
      import ZioExamples._

      (unsafeRun(b_E1234_1.dieIf[E1]): Exit[E234, String]) shouldBe Exit.die(e1)

      (unsafeRun(b_E1234_2.dieIf[E1]): Exit[E234, String]) shouldBe Exit.fail(e2.inject[E234])
      (unsafeRun(b_E1234_3.dieIf[E1]): Exit[E234, String]) shouldBe Exit.fail(e3.inject[E234])
    }

    "throws all selected types when selected" in {
      import ZioExamples._

      (unsafeRun(b_E1234_1.dieIf[E1, E3]): Exit[E24, String]) shouldBe Exit.die(e1)

      (unsafeRun(b_E1234_2.dieIf[E1, E3]): Exit[E24, String]) shouldBe Exit.fail(e2.inject[E24])

      (unsafeRun(b_E1234_3.dieIf[E1, E3]): Exit[E24, String]) shouldBe Exit.die(e3)

      (unsafeRun(b_E1234_4.dieIf[E1, E3]): Exit[E24, String]) shouldBe Exit.fail(e4.inject[E24])
    }
  }
}
