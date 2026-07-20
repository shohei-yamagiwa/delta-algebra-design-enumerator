package dev.shoheiyamagiwa.enumerator.benchmark;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

/**
 * Single-sample throughput of the zero-allocation fast path against the allocating slow reference.
 * This is what the reusable-buffer rewrite of {@link ViolationSampler} was for, and it is the
 * per-sample cost that the strong-scaling numbers of {@link StrongScalingBenchmark} are made of.
 * <p>
 * The state is per-thread because {@link ViolationSampler} keeps mutable buffers across calls:
 * sharing one instance between JMH threads would be a data race and would corrupt the results
 * rather than merely slow them down.
 * <p>
 * Each invocation advances {@code sampleIndex}, so consecutive invocations evaluate different
 * designs. That keeps the benchmark from measuring a single cached tree shape, at the cost of the
 * sample sequence depending on how many invocations an iteration happens to fit in — which is fine
 * here, because every sample of a given {@code deltaCount} costs the same by construction.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(2)
@State(Scope.Thread)
public class ViolationSamplerBenchmark {
    private static final long SEED = 12345L;

    @Param({"12", "30", "60"})
    public int deltaCount;

    private ViolationSampler sampler;
    private long sampleIndex;

    @Setup(Level.Iteration)
    public void setUp() {
        sampler = new ViolationSampler(deltaCount);
        sampleIndex = 0;
    }

    @Benchmark
    public int zeroAllocationFastPath() {
        return sampler.eval(deltaCount, SEED, sampleIndex++);
    }

    @Benchmark
    public int allocatingReference() {
        return NaiveViolationSampler.sampleViolations(deltaCount, SEED, sampleIndex++);
    }
}
