package dev.shoheiyamagiwa.enumerator.benchmark;

import dev.shoheiyamagiwa.enumerator.core.ReferenceSetAlgebra;
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
import org.openjdk.jmh.infra.Blackhole;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Scalar versus SIMD-vectorized reference-set composition {@code (use1 | use2) & ~def1}, over a
 * range of reference-set sizes. The speedup is expected to approach the lane count of the platform's
 * vector species (2 on ARM64 NEON) as the sets grow and the per-call overhead is amortised.
 * <p>
 * JMH forks a fresh JVM for measurement, and the project only passes
 * {@code --add-modules jdk.incubator.vector} to the <em>compiler</em>. The flag is therefore
 * repeated in {@link Fork#jvmArgsAppend()}: without it the forked JVM cannot load the Vector API and
 * the benchmark fails before it runs.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(value = 2, jvmArgsAppend = "--add-modules=jdk.incubator.vector")
@State(Scope.Thread)
public class ReferenceSetAlgebraBenchmark {
    @Param({"64", "256", "1024", "4096", "16384", "65536"})
    public int referenceCount;

    private long[] useBits1;
    private long[] defBits1;
    private long[] useBits2;
    private long[] defBits2;
    private long[] outputUseBits;
    private long[] outputDefBits;

    /**
     * Allocates the operand bitsets and verifies that the two implementations agree, so that a
     * broken vectorization is reported as a failure rather than measured as a speedup.
     */
    @Setup(Level.Trial)
    public void setUp() {
        int wordCount = (referenceCount + 63) >>> 6;
        Random random = new Random(42);

        useBits1 = randomBits(random, wordCount);
        defBits1 = randomBits(random, wordCount);
        useBits2 = randomBits(random, wordCount);
        defBits2 = randomBits(random, wordCount);
        outputUseBits = new long[wordCount];
        outputDefBits = new long[wordCount];

        long[] scalarUse = new long[wordCount];
        long[] scalarDef = new long[wordCount];
        long[] vectorUse = new long[wordCount];
        long[] vectorDef = new long[wordCount];

        ReferenceSetAlgebra.composeScalar(useBits1, defBits1, useBits2, defBits2, scalarUse, scalarDef);
        ReferenceSetAlgebra.composeVector(useBits1, defBits1, useBits2, defBits2, vectorUse, vectorDef);

        if (!Arrays.equals(scalarUse, vectorUse) || !Arrays.equals(scalarDef, vectorDef)) {
            throw new IllegalStateException("composeVector disagrees with composeScalar at referenceCount=" + referenceCount);
        }
    }

    @Benchmark
    public void composeScalar(Blackhole blackhole) {
        ReferenceSetAlgebra.composeScalar(useBits1, defBits1, useBits2, defBits2, outputUseBits, outputDefBits);

        blackhole.consume(outputUseBits);
        blackhole.consume(outputDefBits);
    }

    @Benchmark
    public void composeVector(Blackhole blackhole) {
        ReferenceSetAlgebra.composeVector(useBits1, defBits1, useBits2, defBits2, outputUseBits, outputDefBits);

        blackhole.consume(outputUseBits);
        blackhole.consume(outputDefBits);
    }

    /**
     * Generates an array of random {@code long} words, used as a random reference-set bitmap.
     *
     * @param random    the source of randomness
     * @param wordCount the number of {@code long} words to generate
     * @return the generated array of random bits
     */
    private static long[] randomBits(Random random, int wordCount) {
        long[] bits = new long[wordCount];

        for (int wordIndex = 0; wordIndex < wordCount; wordIndex++) {
            bits[wordIndex] = random.nextLong();
        }

        return bits;
    }
}
