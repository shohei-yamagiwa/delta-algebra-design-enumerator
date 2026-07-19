package dev.shoheiyamagiwa.enumerator.benchmark;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

/**
 * Shared triangulation utilities used by both {@link DesignEnumerator} and {@link DesignSampler}:
 * recursive triangulation enumeration, boundary-edge detection, and diagonal/ear extraction.
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
        int length = vertices.length;
        List<List<int[]>> res = new ArrayList<>();

        if (length < 3) {
            res.add(new ArrayList<>());
            return res;
        }

        int i = vertices[0];
        int j = vertices[length - 1];

        for (int a = 1; a < length - 1; a++) {
            int[] triangle = {i, vertices[a], j};
            int[] left = Arrays.copyOfRange(vertices, 0, a + 1);
            int[] right = Arrays.copyOfRange(vertices, a, length);

            for (List<int[]> L : triFill(left)) {
                for (List<int[]> R : triFill(right)) {
                    List<int[]> combo = new ArrayList<>(L.size() + R.size() + 1);

                    combo.addAll(L);
                    combo.add(triangle);
                    combo.addAll(R);
                    res.add(combo);
                }
            }
        }
        return res;
    }

    /**
     * Determines whether the edge (a, c) is a boundary edge of the convex polygon on vertices 0...k-1.
     *
     * @param a the smaller vertex index of the edge
     * @param c the larger vertex index of the edge
     * @param k the number of vertices of the convex polygon
     * @return {@code true} if (a, c) is a boundary edge, {@code false} otherwise
     */
    public static boolean isBoundary(int a, int c, int k) {
        return (c - a == 1) || (a == 0 && c == k - 1);
    }

    /**
     * Derive the diagonals (canonical order) and the ear count from a triangle list.
     *
     * @param triangles    the triangles forming the triangulation, each represented as an array of three vertex indices
     * @param k            the number of vertices of the convex polygon
     * @param diagonalsOut if non-null, populated with the canonical diagonals (sorted, each as {@code {a, c}} with {@code a < c})
     * @return a two-element array {@code {diagonalCount, earCount}}
     */
    public static int[] diagonalCountAndEars(List<int[]> triangles, int k, List<int[]> diagonalsOut) {
        TreeSet<long[]> diagonalSet = new TreeSet<>(TriangulationUtils::comparePair);
        int ears = 0;

        for (int[] triangle : triangles) {
            int boundaries = 0;
            int[][] edges = {{triangle[0], triangle[1]}, {triangle[1], triangle[2]}, {triangle[0], triangle[2]}};

            for (int[] edge : edges) {
                int a = Math.min(edge[0], edge[1]);
                int c = Math.max(edge[0], edge[1]);

                if (isBoundary(a, c, k)) {
                    boundaries++;
                } else {
                    diagonalSet.add(new long[]{a, c});
                }
            }

            if (boundaries >= 2) {
                ears++;
            }
        }

        if (diagonalsOut != null) {
            for (long[] diagonal : diagonalSet) {
                diagonalsOut.add(new int[]{(int) diagonal[0], (int) diagonal[1]});
            }
        }

        return new int[]{diagonalSet.size(), ears};
    }

    /**
     * Compares two vertex pairs lexicographically, first by the first element, then by the second.
     *
     * @param x the first vertex pair to compare
     * @param y the second vertex pair to compare
     * @return a negative integer, zero, or a positive integer as {@code x} is less than, equal to, or greater than {@code y}
     */
    private static int comparePair(long[] x, long[] y) {
        return x[0] != y[0] ? Long.compare(x[0], y[0]) : Long.compare(x[1], y[1]);
    }
}
