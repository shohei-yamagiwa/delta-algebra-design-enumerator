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

        int deltaCount = 4;
        long sampleCount = 1_400_000;
        Map<String, Integer> triangulationFrequency = new HashMap<>();

        for (long sampleIndex = 0; sampleIndex < sampleCount; sampleIndex++) {
            List<int[]> triangles = DesignSampler.remyTriangles(deltaCount, DesignSampler.rngForSample(SEED, sampleIndex));
            List<int[]> diagonals = new ArrayList<>();

            TriangulationUtils.diagonalCountAndEars(triangles, deltaCount + 2, diagonals);

            StringBuilder diagonalSignature = new StringBuilder();
            for (int[] diagonal : diagonals) {
                diagonalSignature.append(diagonal[0]).append('-').append(diagonal[1]).append(',');
            }

            triangulationFrequency.merge(diagonalSignature.toString(), 1, Integer::sum);
        }

        int minFrequency = Integer.MAX_VALUE;
        int maxFrequency = 0;

        for (int frequency : triangulationFrequency.values()) {
            minFrequency = Math.min(minFrequency, frequency);
            maxFrequency = Math.max(maxFrequency, frequency);
        }

        System.out.printf("distinct triangulations hit=%d (expect 14), per-bucket expect=%.0f, min=%d max=%d%n",
                triangulationFrequency.size(), sampleCount / 14.0, minFrequency, maxFrequency);
    }

    /**
     * Compares sampled violation-count estimates against exact values (computed via exhaustive
     * enumeration) for small {@code n} (4, 6, 8).
     */
    private static void compareEstimateVsExact() {
        System.out.println("\n== estimate vs exact ==");
        System.out.printf("%-4s %-12s %-14s %-14s %-10s %-10s%n", "n", "N", "satFrac(exact)", "satFrac(est)", "min(exact)", "min(est)");

        long sampleCount = 2_000_000;

        for (int deltaCount : new int[]{4, 6, 8}) {
            long[] exact = DesignSampler.exact(deltaCount);
            double exactSatisfyingFraction = (double) exact[1] / exact[2];
            long satisfyingHitCount = 0;
            int estimatedMinViolations = Integer.MAX_VALUE;

            for (long sampleIndex = 0; sampleIndex < sampleCount; sampleIndex++) {
                int violations = DesignSampler.sampleViolations(deltaCount, SEED, sampleIndex);

                if (violations == 0) {
                    satisfyingHitCount++;
                }
                if (violations < estimatedMinViolations) {
                    estimatedMinViolations = violations;
                }
            }

            System.out.printf("%-4d %-12d %-14.6f %-14.6f %-10d %-10d%n", deltaCount, exact[2], exactSatisfyingFraction,
                    (double) satisfyingHitCount / sampleCount, exact[0], estimatedMinViolations);
        }
    }

    /**
     * Samples designs for {@code n} values too large for exhaustive enumeration (30, 60, 120, 200),
     * printing throughput and the best violation count found.
     */
    private static void sampleLargeN() {
        System.out.println("\n== large-n sampling (exhaustive impossible) ==");
        System.out.printf("%-6s %-12s %-12s %-14s %-14s%n", "n", "samples", "time(ms)", "bestViol", "samples/sec");

        long sampleCount = 1_000_000;

        for (int deltaCount : new int[]{30, 60, 120, 200}) {
            long startTimeNanos = System.nanoTime();
            int bestViolations = Integer.MAX_VALUE;
            long satisfyingFoundCount = 0;

            for (long sampleIndex = 0; sampleIndex < sampleCount; sampleIndex++) {
                int violations = DesignSampler.sampleViolations(deltaCount, SEED, sampleIndex);
                if (violations < bestViolations) {
                    bestViolations = violations;
                }
                if (violations == 0) {
                    satisfyingFoundCount++;
                }
            }

            double elapsedMillis = (System.nanoTime() - startTimeNanos) / 1e6;

            System.out.printf("%-6d %-12d %-12.1f %-14d %-14.3e  (satisfying found: %d)%n", deltaCount, sampleCount,
                    elapsedMillis, bestViolations, sampleCount / (elapsedMillis / 1000.0), satisfyingFoundCount);
        }
    }
}
