package hotpotato

import zio.Runtime
import zio.internal.Tracing

object BenchmarkSetup {
  val TracedRuntime: Runtime[zio.ZEnv] = Runtime.default.withTracing(Tracing.enabled)
}
