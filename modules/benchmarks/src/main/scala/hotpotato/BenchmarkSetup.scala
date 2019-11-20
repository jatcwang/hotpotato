package hotpotato

import zio.DefaultRuntime
import zio.internal.{PlatformLive, Tracing}

object BenchmarkSetup {
  val TracedRuntime: DefaultRuntime = new DefaultRuntime {
    override val platform = PlatformLive.Benchmark.withTracing(Tracing.enabled)
  }
}
