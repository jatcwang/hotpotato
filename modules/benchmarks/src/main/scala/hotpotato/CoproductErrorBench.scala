package hotpotato

// Must not be in default package
import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations.{Benchmark, BenchmarkMode, Mode, OutputTimeUnit}
import hotpotato.Examples._
import hotpotato.ZioExamples._
import BenchmarkSetup.TracedRuntime

@OutputTimeUnit(TimeUnit.MILLISECONDS)
@BenchmarkMode(Array(Mode.Throughput))
class CoproductErrorBench {

  @Benchmark
  def copDeepBigErrorTail(): Unit = {
    implicit val embedder: Embedder[E1to8] = new Embedder[E1to8]
    val io = for {
      _ <- g_E1_E2.embedError
      _ <- g_E1_E2.embedError
      _ <- g_E1_E2.embedError
      _ <- g_E1_E2.embedError
      _ <- g_E1_E2.embedError
      _ <- g_E1_E2.embedError
      _ <- g_E1_E2.embedError
      _ <- g_E1_E2.embedError
      _ <- g_E1_E2.embedError
      _ <- g_E1_E2.embedError
      _ <- g_E1_E2.embedError
      _ <- g_E1_E2.embedError
      _ <- g_E1_E2.embedError
      _ <- g_E1_E2.embedError
      _ <- g_E1_E2.embedError
      _ <- g_E1_E2.embedError
      _ <- g_E1_E2.embedError
      _ <- g_E1_E2.embedError
      _ <- g_E1_E2.embedError
      _ <- g_E1_E2.embedError
      _ <- g_E1_E2.embedError
      _ <- g_E1_E2.embedError
      _ <- g_E1_E2.embedError
      _ <- g_E1_E2.embedError
      _ <- b_E1to8_8.embedError
    } yield ()
    val _ = TracedRuntime.unsafeRun(io.either)
  }

  @Benchmark
  def copDeepBigErrorHead(): Unit = {
    implicit val embedder: Embedder[E1_E2_E3_E4] = new Embedder[E1_E2_E3_E4]
    val io = for {
      _ <- g_E1_E2.embedError
      _ <- g_E1_E2.embedError
      _ <- g_E1_E2.embedError
      _ <- g_E1_E2.embedError
      _ <- g_E1_E2.embedError
      _ <- g_E1_E2.embedError
      _ <- g_E1_E2.embedError
      _ <- g_E1_E2.embedError
      _ <- g_E1_E2.embedError
      _ <- g_E1_E2.embedError
      _ <- g_E1_E2.embedError
      _ <- g_E1_E2.embedError
      _ <- g_E1_E2.embedError
      _ <- g_E1_E2.embedError
      _ <- g_E1_E2.embedError
      _ <- g_E1_E2.embedError
      _ <- g_E1_E2.embedError
      _ <- g_E1_E2.embedError
      _ <- g_E1_E2.embedError
      _ <- g_E1_E2.embedError
      _ <- g_E1_E2.embedError
      _ <- g_E1_E2.embedError
      _ <- g_E1_E2.embedError
      _ <- g_E1_E2.embedError
      _ <- b_E1234_1.embedError
    } yield ()
    val _ = TracedRuntime.unsafeRun(io.either)
  }

  @Benchmark
  def norDeepError(): Unit = {
    val io = for {
      _ <- g_E1
      _ <- g_E1
      _ <- g_E1
      _ <- g_E1
      _ <- g_E1
      _ <- g_E1
      _ <- g_E1
      _ <- g_E1
      _ <- g_E1
      _ <- g_E1
      _ <- g_E1
      _ <- g_E1
      _ <- g_E1
      _ <- g_E1
      _ <- g_E1
      _ <- g_E1
      _ <- g_E1
      _ <- g_E1
      _ <- g_E1
      _ <- g_E1
      _ <- g_E1
      _ <- g_E1
      _ <- g_E1
      _ <- g_E1
      _ <- b_E4
    } yield ()
    val _ = TracedRuntime.unsafeRun(io.either)
  }

}
