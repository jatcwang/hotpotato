package hotpotato

import zio.DefaultRuntime
import zio.internal.{PlatformLive, Tracing}

object BenchmarkSetup {
  val TracedRuntime: DefaultRuntime = new DefaultRuntime {
    override val Platform = PlatformLive.Benchmark.withTracing(Tracing.enabled)
  }
}
