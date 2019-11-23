package hotpotato.utils

// Dummy param
final class DummyParam()

object DummyParam {
  implicit val dummyParam: DummyParam = new DummyParam
}
