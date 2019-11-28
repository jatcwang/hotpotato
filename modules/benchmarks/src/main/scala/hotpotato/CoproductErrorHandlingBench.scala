package hotpotato

// Must not be in default package
import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations.{Benchmark, BenchmarkMode, Mode, OutputTimeUnit}
import zio._
import ErrorTrans._
import hotpotato.Examples._
import hotpotato.ZioExamples._
import BenchmarkSetup.TracedRuntime

@OutputTimeUnit(TimeUnit.MILLISECONDS)
@BenchmarkMode(Array(Mode.Throughput))
class CoproductErrorHandlingBench {

  @Benchmark
  def copBigMapErrorSome1: Unit = {
    val io = b_E1to8_1.mapErrorSome(
      (e1: E1) => MSG,
      (e4: E8) => MSG_8,
    )
    TracedRuntime.unsafeRun(io.either)
  }

  @Benchmark
  def copBigMapErrorSome8: Unit = {
    val io = b_E1to8_8.mapErrorSome(
      (e1: E1) => MSG,
      (e4: E8) => MSG_8,
    )
    TracedRuntime.unsafeRun(io.either)
  }

  @Benchmark
  def copBigMapAdtSome: Unit = {
    import shapeless._
    val io = b_allError_8.mapErrorSomeAdt(
      (e1: E1) => MSG,
      (e4: E8) => MSG_8,
    )
    TracedRuntime.unsafeRun(io.either)
  }

  //
  // @Benchmark
  // def copHandleBaseline: Unit = {
  //   val io = b_allError_4.mapError {
  //     case E1() => MSG
  //     case E2() => MSG
  //     case E3() => MSG
  //     case E4() => MSG
  //     case E5() => MSG
  //     case E6() => MSG
  //     case E7() => MSG
  //     case E8() => MSG
  //   }
  //
  //   TracedRuntime.unsafeRun(io.either)
  // }

}
