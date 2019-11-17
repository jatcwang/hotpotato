package hotpotato

import org.scalatest.{FreeSpec, Inside, Matchers}
import PureExamples._
import ErrorTrans._
import hotpotato.Examples.E1

class ErrorTransformSpec extends FreeSpec with Matchers with Inside {
  "unifyError should" - {
    "unify the error into one" in {
      inside(b_E12_1.unifyError) {
        case Left(E1()) => succeed
      }
    }
  }
}
