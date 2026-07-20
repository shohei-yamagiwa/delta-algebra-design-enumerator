package dev.shoheiyamagiwa.enumerator.benchmark;

import dev.shoheiyamagiwa.enumerator.core.DesignEvaluator;
import dev.shoheiyamagiwa.enumerator.core.TriangulationUtils;

import java.util.List;

/**
 * Exhaustively (not randomly) enumerates every triangulation and every diagonal-direction
 * combination of a Δ-partition, to compute the exact minimum violations, the number of
 * combinations satisfying the design, and the total combination count.
 */
public class ExactDesignEvaluator {
    public static long[] exact(int deltaCount) {
        int vertexCount = deltaCount + 2;
        int[] vertices = new int[vertexCount];

        for (int vertexIndex = 0; vertexIndex < vertexCount; vertexIndex++) {
            vertices[vertexIndex] = vertexIndex;
        }

        List<List<int[]>> allTriangulations = TriangulationUtils.triFill(vertices);
        long directionCombinationCount = 1L << (deltaCount - 1);
        int minViolations = Integer.MAX_VALUE;
        long satisfyingCount = 0;

        for (List<int[]> triangles : allTriangulations) {
            int ears = TriangulationUtils.diagonalCountAndEars(triangles, vertexCount, null)[1];

            for (long directionBits = 0; directionBits < directionCombinationCount; directionBits++) {
                int violations = DesignEvaluator.violations(Long.bitCount(directionBits), ears);

                if (violations < minViolations) {
                    minViolations = violations;
                }
                if (violations == 0) {
                    satisfyingCount++;
                }
            }
        }
        return new long[]{minViolations, satisfyingCount, (long) allTriangulations.size() * directionCombinationCount};
    }
}
