import hotpotato.ErrorTrans
import org.scalatest.wordspec.AnyWordSpec

/** Test that optional dependency of core being missing does not inhibit resolution
  * of instances for supported classes that has the necessary dependency in the classpath */
class OptionalDependencyMissingTest extends AnyWordSpec {
  "ErrorTrans instance" should {
    "for Either" in {
      val _ = implicitly[ErrorTrans[Either]]
    }
  }
}
