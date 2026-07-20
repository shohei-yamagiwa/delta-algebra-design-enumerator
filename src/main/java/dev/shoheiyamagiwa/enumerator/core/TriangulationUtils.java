package dev.shoheiyamagiwa.enumerator.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

/**
 * Shared triangulation utilities used by both {@link DesignEnumerator} and
 * {@code dev.shoheiyamagiwa.enumerator.benchmark.RemyTriangulationSampler}: recursive triangulation
 * enumeration, boundary-edge detection, and diagonal/ear extraction.
 * Consolidating this logic here avoids the two classes drifting apart when one of them is changed.
 */
public final class TriangulationUtils {
    private TriangulationUtils() {
    }

    /**
     * Recursively enumerate triangulations of a contiguous arc; base edge = (vertices[0], vertices[last]).
     *
     * @param vertices the vertices of the arc, in order, whose first and last elements form the base edge
     * @return the list of triangulations of the arc, where each triangulation is represented as a list of triangles
     */
    public static List<List<int[]>> triFill(int[] vertices) {
        int vertexCount = vertices.length;
        List<List<int[]>> result = new ArrayList<>();

        if (vertexCount < 3) {
            result.add(new ArrayList<>());
            return result;
        }

        int firstVertex = vertices[0];
        int lastVertex = vertices[vertexCount - 1];

        for (int apexIndex = 1; apexIndex < vertexCount - 1; apexIndex++) {
            int[] triangle = {firstVertex, vertices[apexIndex], lastVertex};
            int[] leftArc = Arrays.copyOfRange(vertices, 0, apexIndex + 1);
            int[] rightArc = Arrays.copyOfRange(vertices, apexIndex, vertexCount);

            for (List<int[]> leftTriangulation : triFill(leftArc)) {
                for (List<int[]> rightTriangulation : triFill(rightArc)) {
                    List<int[]> combinedTriangles = new ArrayList<>(leftTriangulation.size() + rightTriangulation.size() + 1);

                    combinedTriangles.addAll(leftTriangulation);
                    combinedTriangles.add(triangle);
                    combinedTriangles.addAll(rightTriangulation);
                    result.add(combinedTriangles);
                }
            }
        }
        return result;
    }

    /**
     * Determines whether the edge (smallerVertex, largerVertex) is a boundary edge of the convex
     * polygon on vertices 0...vertexCount-1.
     *
     * @param smallerVertex the smaller vertex index of the edge
     * @param largerVertex  the larger vertex index of the edge
     * @param vertexCount   the number of vertices of the convex polygon
     * @return {@code true} if (smallerVertex, largerVertex) is a boundary edge, {@code false} otherwise
     */
    public static boolean isBoundary(int smallerVertex, int largerVertex, int vertexCount) {
        return (largerVertex - smallerVertex == 1) || (smallerVertex == 0 && largerVertex == vertexCount - 1);
    }

    /**
     * Derive the diagonals (canonical order) and the ear count from a triangle list.
     *
     * @param triangles    the triangles forming the triangulation, each represented as an array of three vertex indices
     * @param vertexCount  the number of vertices of the convex polygon
     * @param diagonalsOut if non-null, populated with the canonical diagonals (sorted, each as {@code {smallerVertex, largerVertex}} with {@code smallerVertex < largerVertex})
     * @return a two-element array {@code {diagonalCount, earCount}}
     */
    public static int[] diagonalCountAndEars(List<int[]> triangles, int vertexCount, List<int[]> diagonalsOut) {
        TreeSet<long[]> diagonalSet = new TreeSet<>(TriangulationUtils::comparePair);
        int earCount = 0;

        for (int[] triangle : triangles) {
            int boundaryEdgeCount = 0;
            int[][] edges = {{triangle[0], triangle[1]}, {triangle[1], triangle[2]}, {triangle[0], triangle[2]}};

            for (int[] edge : edges) {
                int smallerVertex = Math.min(edge[0], edge[1]);
                int largerVertex = Math.max(edge[0], edge[1]);

                if (isBoundary(smallerVertex, largerVertex, vertexCount)) {
                    boundaryEdgeCount++;
                } else {
                    diagonalSet.add(new long[]{smallerVertex, largerVertex});
                }
            }

            if (boundaryEdgeCount >= 2) {
                earCount++;
            }
        }

        if (diagonalsOut != null) {
            for (long[] diagonal : diagonalSet) {
                diagonalsOut.add(new int[]{(int) diagonal[0], (int) diagonal[1]});
            }
        }

        return new int[]{diagonalSet.size(), earCount};
    }

    /**
     * Compares two vertex pairs lexicographically, first by the first element, then by the second.
     *
     * @param firstPair  the first vertex pair to compare
     * @param secondPair the second vertex pair to compare
     * @return a negative integer, zero, or a positive integer as {@code firstPair} is less than, equal to, or greater than {@code secondPair}
     */
    private static int comparePair(long[] firstPair, long[] secondPair) {
        return firstPair[0] != secondPair[0] ? Long.compare(firstPair[0], secondPair[0]) : Long.compare(firstPair[1], secondPair[1]);
    }
}
