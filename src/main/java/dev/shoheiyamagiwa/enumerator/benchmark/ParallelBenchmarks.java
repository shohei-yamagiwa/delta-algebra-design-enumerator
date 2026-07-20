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
        int cores = Runtime.getRuntime().availableProcessors();

        System.out.println("availableProcessors = " + cores + "\n");

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

        Acc chk = DeltaParallel.serial(6, SEED, 2_000_000);

        System.out.printf("n=6  satFrac(est)=%.6f  (DeltaSampler gave 0.015125)  best=%d%n%n", (double) chk.sat / 2_000_000, chk.best);
    }

    /**
     * Checks that running the sampling loop across multiple threads produces results identical to
     * the serial run (determinism / correctness of the parallel accumulation).
     *
     * @throws Exception if a parallel sampling task fails
     */
    private static void checkMulticoreMatchesSerial() throws Exception {
        System.out.println("== multicore result == serial result ==");

        int nC = 60;
        long Sc = 300_000;
        Acc s1 = DeltaParallel.serial(nC, SEED, Sc);
        Acc s8 = DeltaParallel.parallel(nC, SEED, Sc, 8);
        boolean same = (s1.best == s8.best) && (s1.sat == s8.sat) && Arrays.equals(s1.hist, s8.hist);

        System.out.printf("n=%d S=%d : serial(best=%d,sat=%d) vs 8-thread(best=%d,sat=%d) -> %s%n%n", nC, Sc, s1.best, s1.sat, s8.best, s8.sat, same ? "IDENTICAL" : "MISMATCH");
    }

    /**
     * Measures how sampling throughput scales with the number of threads (strong scaling), printing
     * speedup and efficiency relative to the single-threaded run.
     *
     * @throws Exception if a parallel sampling task fails
     */
    private static void measureStrongScaling() throws Exception {
        int nC = 60;
        long Sc = 300_000;

        System.out.println("== strong scaling (n=" + nC + ", S=" + Sc + ") ==");
        System.out.printf("%-8s %-12s %-10s %-12s%n", "threads", "time(ms)", "speedup", "efficiency");

        double base = -1;

        for (int t : new int[]{1, 2, 4, 8, 12}) {
            long t0 = System.nanoTime();

            DeltaParallel.parallel(nC, SEED, Sc, t);

            double ms = (System.nanoTime() - t0) / 1e6;

            if (base < 0) {
                base = ms;
            }

            System.out.printf("%-8d %-12.1f %-10.2f %-12.2f%n", t, ms, base / ms, (base / ms) / t);
        }

        System.out.println("\n(On this 1-core container speedup stays ~1x; run on the ARM64 laptop for real scaling.)");
    }
}
