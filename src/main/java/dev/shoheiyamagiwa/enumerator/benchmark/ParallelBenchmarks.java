package dev.shoheiyamagiwa.enumerator.benchmark;

import java.util.Arrays;

/**
 * Benchmarks about parallelizing the zero-allocation {@link Sampler}-based sampling loop (see
 * {@link DeltaParallel}): correctness of the multicore path against the serial one, and how well it
 * scales with the number of threads.
 */
public class ParallelBenchmarks {
    private static final long SEED = 12345L;

    /**
     * Runs all parallel-sampling benchmarks, printing their results to standard output.
     *
     * @throws Exception if a parallel sampling task fails
     */
    public static void run() throws Exception {
        int availableProcessorCount = Runtime.getRuntime().availableProcessors();

        System.out.println("availableProcessors = " + availableProcessorCount + "\n");

        checkReproducesReferenceNumbers();
        checkMulticoreMatchesSerial();
        measureStrongScaling();
    }

    /**
     * Checks that the zero-allocation sampling path reproduces the reference satisfaction-fraction
     * number previously obtained from {@link DesignSampler} at n=6.
     */
    private static void checkReproducesReferenceNumbers() {
        System.out.println("== zero-alloc reproduces reference numbers ==");

        long sampleCount = 2_000_000;
        Acc accumulator = DeltaParallel.serial(6, SEED, sampleCount);

        System.out.printf("n=6  satFrac(est)=%.6f  (DeltaSampler gave 0.015125)  best=%d%n%n",
                (double) accumulator.satisfyingCount / sampleCount, accumulator.bestViolationCount);
    }

    /**
     * Checks that running the sampling loop across multiple threads produces results identical to
     * the serial run (determinism / correctness of the parallel accumulation).
     *
     * @throws Exception if a parallel sampling task fails
     */
    private static void checkMulticoreMatchesSerial() throws Exception {
        System.out.println("== multicore result == serial result ==");

        int deltaCount = 60;
        long sampleCount = 300_000;
        Acc serialResult = DeltaParallel.serial(deltaCount, SEED, sampleCount);
        Acc parallelResult = DeltaParallel.parallel(deltaCount, SEED, sampleCount, 8);
        boolean identical = (serialResult.bestViolationCount == parallelResult.bestViolationCount)
                && (serialResult.satisfyingCount == parallelResult.satisfyingCount)
                && Arrays.equals(serialResult.violationHistogram, parallelResult.violationHistogram);

        System.out.printf("n=%d S=%d : serial(best=%d,sat=%d) vs 8-thread(best=%d,sat=%d) -> %s%n%n",
                deltaCount, sampleCount, serialResult.bestViolationCount, serialResult.satisfyingCount,
                parallelResult.bestViolationCount, parallelResult.satisfyingCount, identical ? "IDENTICAL" : "MISMATCH");
    }

    /**
     * Measures how sampling throughput scales with the number of threads (strong scaling), printing
     * speedup and efficiency relative to the single-threaded run.
     *
     * @throws Exception if a parallel sampling task fails
     */
    private static void measureStrongScaling() throws Exception {
        int deltaCount = 60;
        long sampleCount = 300_000;

        System.out.println("== strong scaling (n=" + deltaCount + ", S=" + sampleCount + ") ==");
        System.out.printf("%-8s %-12s %-10s %-12s%n", "threads", "time(ms)", "speedup", "efficiency");

        double baselineElapsedMillis = -1;

        for (int threadCount : new int[]{1, 2, 4, 8, 12}) {
            long startTimeNanos = System.nanoTime();

            DeltaParallel.parallel(deltaCount, SEED, sampleCount, threadCount);

            double elapsedMillis = (System.nanoTime() - startTimeNanos) / 1e6;

            if (baselineElapsedMillis < 0) {
                baselineElapsedMillis = elapsedMillis;
            }

            System.out.printf("%-8d %-12.1f %-10.2f %-12.2f%n", threadCount, elapsedMillis,
                    baselineElapsedMillis / elapsedMillis, (baselineElapsedMillis / elapsedMillis) / threadCount);
        }
    }
}
