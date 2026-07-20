package dev.shoheiyamagiwa.enumerator.benchmark;

import dev.shoheiyamagiwa.enumerator.core.DesignEvaluator;
import dev.shoheiyamagiwa.enumerator.core.TriangulationUtils;

import java.util.List;

/**
 * Allocating, "slow-path" Monte-Carlo estimator of violations for a single Δ-partition sample:
 * builds one triangulation via {@link RemyTriangulationSampler}, counts its ears, draws random
 * diagonal directions, and reports {@link DesignEvaluator#violations}. Used as the reference
 * implementation that {@link ViolationSampler}'s zero-allocation fast path is checked against.
 */
public class NaiveViolationSampler {
    public static int sampleViolations(int deltaCount, long globalSeed, long sampleIndex) {
        SplitMix64 rng = RemyTriangulationSampler.rngForSample(globalSeed, sampleIndex);
        List<int[]> triangles = RemyTriangulationSampler.remyTriangles(deltaCount, rng);
        int[] diagonalCountAndEarCount = TriangulationUtils.diagonalCountAndEars(triangles, deltaCount + 2, null); // {#diagonals, ears}
        int diagonalCount = diagonalCountAndEarCount[0];
        int ears = diagonalCountAndEarCount[1]; // diagonalCount == deltaCount-1
        int setDirectionBits = 0;

        for (int diagonalIndex = 0; diagonalIndex < diagonalCount; diagonalIndex++) {
            if (rng.nextBit()) setDirectionBits++; // directions
        }

        return DesignEvaluator.violations(setDirectionBits, ears);
    }
}
