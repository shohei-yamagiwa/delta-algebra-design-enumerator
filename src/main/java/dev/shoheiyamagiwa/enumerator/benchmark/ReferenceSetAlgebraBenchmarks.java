package dev.shoheiyamagiwa.enumerator.benchmark;

import dev.shoheiyamagiwa.enumerator.core.ReferenceSetAlgebra;

import java.util.Arrays;
import java.util.Random;

/**
 * Micro-benchmark comparing {@link ReferenceSetAlgebra}'s scalar and SIMD-vectorized reference-set
 * composition implementations, unrelated to triangulation enumeration or Demeter-rule sampling.
 */
public class ReferenceSetAlgebraBenchmarks {
    /**
     * Runs the scalar-vs-vector composition micro-benchmark, printing its results to standard
     * output.
     */
    public static void run() {
        System.out.println("vector species = " + ReferenceSetAlgebra.LONG_SPECIES
                + "  (lanes = " + ReferenceSetAlgebra.LONG_SPECIES.length()
                + ", " + ReferenceSetAlgebra.LONG_SPECIES.length() * 64 + " bits per vector)");
        System.out.println("On ARM64 without SVE this is NEON: 2 lanes / 128 bits.\n");

        int[] referenceCountValues = {64, 256, 1024, 4096, 16384, 65536};
        int compositionsPerMeasurement = 200_000;
        Random random = new Random(42);

        System.out.printf("%-14s %-16s %-16s %-10s %-10s%n", "referenceCount", "scalar (ns/op)", "vector (ns/op)", "speedup", "correct");

        for (int referenceCount : referenceCountValues) {
            int wordCount = (referenceCount + 63) >>> 6;

            long[] useBits1 = randomBits(random, wordCount);
            long[] defBits1 = randomBits(random, wordCount);
            long[] useBits2 = randomBits(random, wordCount);
            long[] defBits2 = randomBits(random, wordCount);

            long[] outputUseScalar = new long[wordCount];
            long[] outputDefScalar = new long[wordCount];
            long[] outputUseVector = new long[wordCount];
            long[] outputDefVector = new long[wordCount];

            // warm up the just-in-time compiler
            for (int iteration = 0; iteration < 50_000; iteration++) {
                ReferenceSetAlgebra.composeScalar(useBits1, defBits1, useBits2, defBits2, outputUseScalar, outputDefScalar);
                ReferenceSetAlgebra.composeVector(useBits1, defBits1, useBits2, defBits2, outputUseVector, outputDefVector);
            }

            boolean correct = Arrays.equals(outputUseScalar, outputUseVector) && Arrays.equals(outputDefScalar, outputDefVector);

            long checksum = 0;

            long scalarStartTime = System.nanoTime();
            for (int iteration = 0; iteration < compositionsPerMeasurement; iteration++) {
                ReferenceSetAlgebra.composeScalar(useBits1, defBits1, useBits2, defBits2, outputUseScalar, outputDefScalar);
                checksum += outputUseScalar[iteration % wordCount];
            }
            long scalarElapsedNanos = System.nanoTime() - scalarStartTime;

            long vectorStartTime = System.nanoTime();
            for (int iteration = 0; iteration < compositionsPerMeasurement; iteration++) {
                ReferenceSetAlgebra.composeVector(useBits1, defBits1, useBits2, defBits2, outputUseVector, outputDefVector);
                checksum += outputUseVector[iteration % wordCount];
            }
            long vectorElapsedNanos = System.nanoTime() - vectorStartTime;

            double scalarNanosPerOp = (double) scalarElapsedNanos / compositionsPerMeasurement;
            double vectorNanosPerOp = (double) vectorElapsedNanos / compositionsPerMeasurement;

            System.out.printf("%-14d %-16.1f %-16.1f %-10.2f %-10s%s%n",
                    referenceCount, scalarNanosPerOp, vectorNanosPerOp,
                    scalarNanosPerOp / vectorNanosPerOp, correct ? "OK" : "MISMATCH",
                    (checksum == Long.MIN_VALUE) ? " " : "");
        }

        System.out.println("\nHonest notes for the report:");
        System.out.println(" - The scalar long[] loop is itself auto-vectorized by C2; for a fair");
        System.out.println("   'un-vectorized baseline' run it with -XX:-UseSuperWord.");
        System.out.println(" - On NEON (2 lanes) the ceiling for long-wise ops is ~2x; the speedup");
        System.out.println("   grows with referenceCount as the loop becomes compute-bound.");
        System.out.println(" - Take final numbers with JMH (warmup separated, dead-code elimination guarded).");
    }

    /**
     * Generates an array of random {@code long} words, used as random reference-set bitmaps.
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
