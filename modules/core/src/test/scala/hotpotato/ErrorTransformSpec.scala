package hotpotato

import hotpotato.ErrorTrans._
import hotpotato.Examples.E1
import hotpotato.PureExamples._
import org.scalatest.{Inside, Matchers, WordSpec}

class ErrorTransformSpec extends WordSpec with Matchers with Inside {
  "unifyError" should {
    "unify the error into one" in {
      inside(b_E12_1.unifyError) {
        case Left(E1()) => succeed
      }
    }
  }
}
