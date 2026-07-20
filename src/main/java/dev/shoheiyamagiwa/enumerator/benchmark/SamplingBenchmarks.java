package dev.shoheiyamagiwa.enumerator.benchmark;

import dev.shoheiyamagiwa.enumerator.core.TriangulationUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Benchmarks and checks about {@link DesignSampler}'s Rémy-tree-based random sampling: that it
 * samples each triangulation uniformly, that its violation estimates match the exact values for
 * small {@code n}, and how it performs for {@code n} values too large for exhaustive enumeration.
 */
public class SamplingBenchmarks {
    private static final long SEED = 12345L;

    /**
     * Runs all sampling benchmarks/checks, printing their results to standard output.
     */
    public static void run() {
        checkUniformity();
        compareEstimateVsExact();
        sampleLargeN();
    }

    /**
     * Checks that Rémy sampling hits each of the 14 triangulations of a 6-gon (n=4) with roughly
     * equal frequency.
     */
    private static void checkUniformity() {
        System.out.println("== uniformity check (n=4, 14 triangulations) ==");

        int nU = 4, S1 = 1_400_000;
        Map<String, Integer> freq = new HashMap<>();

        for (long i = 0; i < S1; i++) {
            List<int[]> tris = DesignSampler.remyTriangles(nU, DesignSampler.rngForSample(SEED, i));
            List<int[]> diagonals = new ArrayList<>();

            TriangulationUtils.diagonalCountAndEars(tris, nU + 2, diagonals);

            StringBuilder sb = new StringBuilder();
            for (int[] diagonal : diagonals) {
                sb.append(diagonal[0]).append('-').append(diagonal[1]).append(',');
            }

            freq.merge(sb.toString(), 1, Integer::sum);
        }

        int mn = Integer.MAX_VALUE;
        int mx = 0;

        for (int f : freq.values()) {
            mn = Math.min(mn, f);
            mx = Math.max(mx, f);
        }

        System.out.printf("distinct triangulations hit=%d (expect 14), per-bucket expect=%.0f, min=%d max=%d%n", freq.size(), S1 / 14.0, mn, mx);
    }

    /**
     * Compares sampled violation-count estimates against exact values (computed via exhaustive
     * enumeration) for small {@code n} (4, 6, 8).
     */
    private static void compareEstimateVsExact() {
        System.out.println("\n== estimate vs exact ==");
        System.out.printf("%-4s %-12s %-14s %-14s %-10s %-10s%n", "n", "N", "satFrac(exact)", "satFrac(est)", "min(exact)", "min(est)");

        int S2 = 2_000_000;

        for (int n : new int[]{4, 6, 8}) {
            long[] ex = DesignSampler.exact(n);
            double satExact = (double) ex[1] / ex[2];
            long hit = 0;
            int estMin = Integer.MAX_VALUE;

            for (long i = 0; i < S2; i++) {
                int v = DesignSampler.sampleViolations(n, SEED, i);

                if (v == 0) {
                    hit++;
                }
                if (v < estMin) {
                    estMin = v;
                }
            }

            System.out.printf("%-4d %-12d %-14.6f %-14.6f %-10d %-10d%n", n, ex[2], satExact, (double) hit / S2, ex[0], estMin);
        }
    }

    /**
     * Samples designs for {@code n} values too large for exhaustive enumeration (30, 60, 120, 200),
     * printing throughput and the best violation count found.
     */
    private static void sampleLargeN() {
        System.out.println("\n== large-n sampling (exhaustive impossible) ==");
        System.out.printf("%-6s %-12s %-12s %-14s %-14s%n", "n", "samples", "time(ms)", "bestViol", "samples/sec");

        int S3 = 1_000_000;

        for (int n : new int[]{30, 60, 120, 200}) {
            long t0 = System.nanoTime();
            int best = Integer.MAX_VALUE;
            long satFound = 0;

            for (long i = 0; i < S3; i++) {
                int v = DesignSampler.sampleViolations(n, SEED, i);
                if (v < best) {
                    best = v;
                }
                if (v == 0) {
                    satFound++;
                }
            }

            double ms = (System.nanoTime() - t0) / 1e6;

            System.out.printf("%-6d %-12d %-12.1f %-14d %-14.3e  (satisfying found: %d)%n", n, S3, ms, best, S3 / (ms / 1000.0), satFound);
        }
    }
}
