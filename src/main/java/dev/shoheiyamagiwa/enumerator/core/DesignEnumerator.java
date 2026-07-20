package dev.shoheiyamagiwa.enumerator.core;

import dev.shoheiyamagiwa.enumerator.model.Polygon;
import dev.shoheiyamagiwa.enumerator.model.Triangulation;

import java.util.ArrayList;
import java.util.List;

public class DesignEnumerator {
    public static Result enumerate(Polygon polygon, DesignEvaluator evaluator) {
        int deltaCount = polygon.deltas();
        List<Triangulation> allTriangulations = triangulations(polygon.vertices());
        long directionCombinationCount = 1L << (deltaCount - 1); // 2 ^ (deltaCount - 1)  (deltaCount >= 1; for deltaCount == 0 handle trivially)
        long totalDesignCount = (long) allTriangulations.size() * directionCombinationCount;
        int minViolations = Integer.MAX_VALUE;
        long satisfyingDesignCount = 0;

        long startTimeNanos = System.nanoTime();

        for (Triangulation triangulation : allTriangulations) {
            for (long directionBits = 0; directionBits < directionCombinationCount; directionBits++) {
                int violations = evaluator.violations(polygon, triangulation, directionBits);

                if (violations < minViolations) {
                    minViolations = violations;
                }
                if (violations == 0) {
                    satisfyingDesignCount++;
                }
            }
        }

        double elapsedMillis = (System.nanoTime() - startTimeNanos) / 1e6;

        return new Result(totalDesignCount, minViolations, satisfyingDesignCount, elapsedMillis);
    }

    /**
     * All triangulations of the convex polygon on vertices 0...k-1 (base edge = (0,k-1)).
     *
     * @param vertexCount the number of vertices of the convex polygon
     * @return the list of all possible {@link Triangulation}s of the polygon
     */
    public static List<Triangulation> triangulations(int vertexCount) {
        int[] vertices = new int[vertexCount];

        for (int vertexIndex = 0; vertexIndex < vertexCount; vertexIndex++) {
            vertices[vertexIndex] = vertexIndex;
        }

        List<List<int[]>> triangleListsPerTriangulation = TriangulationUtils.triFill(vertices);
        List<Triangulation> triangulationList = new ArrayList<>(triangleListsPerTriangulation.size());

        for (List<int[]> triangleList : triangleListsPerTriangulation) {
            triangulationList.add(finish(triangleList, vertexCount));
        }

        return triangulationList;
    }

    /**
     * Derive diagonals (canonical order) + ear count from a triangle list.
     *
     * @param triangles   the triangles forming the triangulation, each represented as an array of three vertex indices
     * @param vertexCount the number of vertices of the convex polygon
     * @return the resulting {@link Triangulation}, containing the triangles, canonical diagonals and ear count
     */
    private static Triangulation finish(List<int[]> triangles, int vertexCount) {
        List<int[]> diagonalsList = new ArrayList<>();
        int[] diagonalCountAndEarCount = TriangulationUtils.diagonalCountAndEars(triangles, vertexCount, diagonalsList);

        return new Triangulation(triangles.toArray(new int[0][]), diagonalsList.toArray(new int[0][]), diagonalCountAndEarCount[1]);
    }
}
