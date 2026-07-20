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
 * Strong scaling of the sampling loop: the total amount of work ({@link #SAMPLE_COUNT} samples) is
 * held fixed while the number of threads varies, so that the reported average times divide into a
 * speedup curve.
 * <p>
 * One invocation is one complete parallel run, measured single-threaded from JMH's point of view.
 * That is deliberate: strong scaling is a statement about time-to-solution for a fixed problem, so
 * the harness must time the whole job rather than sample throughput inside it. The cost of creating
 * and shutting down the thread pool is therefore included in every measurement; {@link #SAMPLE_COUNT}
 * is chosen large enough for that fixed cost to stay negligible against the sampling itself.
 * <p>
 * Note that speedup has to be computed from the {@code threadCount=1} row of the results — JMH
 * reports absolute times per parameter combination and knows nothing about the baseline.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
@State(Scope.Benchmark)
public class StrongScalingBenchmark {
    private static final long SEED = 12345L;
    private static final long SAMPLE_COUNT = 200_000;

    @Param({"1", "2", "4", "8", "12"})
    public int threadCount;

    @Param({"60"})
    public int deltaCount;

    /**
     * Returns the accumulator so that JMH consumes it: without a returned value the whole sampling
     * run is dead code and may be optimised away.
     *
     * @return the accumulated statistics of the run
     * @throws Exception if a sampling task fails
     */
    @Benchmark
    public Accumulator parallelSampling() throws Exception {
        return DeltaParallel.parallel(deltaCount, SEED, SAMPLE_COUNT, threadCount);
    }
}
