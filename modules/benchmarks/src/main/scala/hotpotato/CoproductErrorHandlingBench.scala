package hotpotato

// Must not be in default package
import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations.{Benchmark, BenchmarkMode, Mode, OutputTimeUnit}
import hotpotato.Examples._
import hotpotato.ZioExamples._
import BenchmarkSetup.TracedRuntime

@OutputTimeUnit(TimeUnit.MILLISECONDS)
@BenchmarkMode(Array(Mode.Throughput))
class CoproductErrorHandlingBench {

  @Benchmark
  def copBigMapErrorSome1(): Unit = {
    val io = b_E1to8_1.mapErrorSome(
      (_: E1) => MSG,
      (_: E8) => MSG_8,
    )
    val _ = TracedRuntime.unsafeRun(io.either)
  }

  @Benchmark
  def copBigMapErrorSome8(): Unit = {
    val io = b_E1to8_8.mapErrorSome(
      (_: E1) => MSG,
      (_: E8) => MSG_8,
    )
    val _ = TracedRuntime.unsafeRun(io.either)
  }

}
