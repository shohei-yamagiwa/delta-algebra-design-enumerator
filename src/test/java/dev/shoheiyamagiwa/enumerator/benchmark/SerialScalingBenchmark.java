package dev.shoheiyamagiwa.enumerator.benchmark;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

/**
 * True serial baseline for {@link StrongScalingBenchmark}: calls {@link DeltaParallel#serial}
 * directly, with no {@link java.util.concurrent.ExecutorService} involved at all.
 * <p>
 * {@link StrongScalingBenchmark}'s {@code threadCount=1} row still goes through
 * {@code DeltaParallel.parallel}, i.e. it creates a one-thread {@code ExecutorService}, submits a
 * single {@link java.util.concurrent.Future}, and waits on it — that is "parallel processing with
 * thread count 1", not a plain sequential loop. This benchmark measures the plain loop itself, so
 * that the thread-pool overhead baked into the {@code threadCount=1} row can be quantified by
 * comparing the two.
 * <p>
 * Same fixed total work ({@link #SAMPLE_COUNT} samples) and same {@code deltaCount} as
 * {@link StrongScalingBenchmark}, so the two can be compared directly.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
@State(Scope.Benchmark)
public class SerialScalingBenchmark {
    private static final long SEED = 12345L;
    private static final long SAMPLE_COUNT = 200_000;

    @Param({"60"})
    public int deltaCount;

    /**
     * Returns the accumulator so that JMH consumes it: without a returned value the whole sampling
     * run is dead code and may be optimised away.
     *
     * @return the accumulated statistics of the run
     */
    @Benchmark
    public Accumulator serialSampling() {
        return DeltaParallel.serial(deltaCount, SEED, SAMPLE_COUNT);
    }
}
