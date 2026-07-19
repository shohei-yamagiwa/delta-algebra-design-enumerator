package dev.shoheiyamagiwa.enumerator.benchmark;

import dev.shoheiyamagiwa.enumerator.model.Polygon;
import dev.shoheiyamagiwa.enumerator.model.Triangulation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

public class DesignEnumerator {
    public static Result enumerate(Polygon polygon, DesignEvaluator evaluator) {
        int n = polygon.deltas();
        List<Triangulation> triangulations = triangulations(polygon.vertices());
        long dirCount = 1L << (n - 1); // 2 ^ (n - 1)  (n >= 1; for n == 0 handle trivially)
        long N = (long) triangulations.size() * dirCount;
        int minViol = Integer.MAX_VALUE;
        long sat = 0;

        long t0 = System.nanoTime();

        for (Triangulation t : triangulations) {
            for (long m = 0; m < dirCount; m++) {
                int v = evaluator.violations(polygon, t, m);

                if (v < minViol) {
                    minViol = v;
                }
                if (v == 0) {
                    sat++;
                }
            }
        }

        double ms = (System.nanoTime() - t0) / 1e6;

        return new Result(N, minViol, sat, ms);
    }

    /**
     * All triangulations of the convex polygon on vertices 0...k-1 (base edge = (0,k-1)).
     *
     * @param k the number of vertices of the convex polygon
     * @return the list of all possible {@link Triangulation}s of the polygon
     */
    public static List<Triangulation> triangulations(int k) {
        int[] vertices = new int[k];

        for (int i = 0; i < k; i++) {
            vertices[i] = i;
        }

        List<List<int[]>> raw = triFill(vertices);
        List<Triangulation> out = new ArrayList<>(raw.size());

        for (List<int[]> tris : raw) {
            out.add(finish(tris, k));
        }

        return out;
    }

    /**
     * Recursively enumerate triangulations of a contiguous arc; base edge = (vertices[0], vertices[last]).
     *
     * @param vertices the vertices of the arc, in order, whose first and last elements form the base edge
     * @return the list of triangulations of the arc, where each triangulation is represented as a list of triangles
     */
    private static List<List<int[]>> triFill(int[] vertices) {
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
     * Derive diagonals (canonical order) + ear count from a triangle list.
     *
     * @param triangles the triangles forming the triangulation, each represented as an array of three vertex indices
     * @param k         the number of vertices of the convex polygon
     * @return the resulting {@link Triangulation}, containing the triangles, canonical diagonals and ear count
     */
    private static Triangulation finish(List<int[]> triangles, int k) {
        TreeSet<long[]> diagonalSet = new TreeSet<>(DesignEnumerator::comparePair);
        int ears = 0;

        for (int[] triangle : triangles) {
            int boundaries = 0;
            int[][] es = {{triangle[0], triangle[1]}, {triangle[1], triangle[2]}, {triangle[0], triangle[2]}};

            for (int[] e : es) {
                int a = Math.min(e[0], e[1]), c = Math.max(e[0], e[1]);

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

        int[][] diagonals = new int[diagonalSet.size()][2];
        int index = 0;

        for (long[] d : diagonalSet) {
            diagonals[index++] = new int[]{(int) d[0], (int) d[1]};
        }

        return new Triangulation(triangles.toArray(new int[0][]), diagonals, ears);
    }

    /**
     * Determines whether the edge (a, c) is a boundary edge of the convex polygon on vertices 0...k-1.
     *
     * @param a the smaller vertex index of the edge
     * @param c the larger vertex index of the edge
     * @param k the number of vertices of the convex polygon
     * @return {@code true} if (a, c) is a boundary edge, {@code false} otherwise
     */
    private static boolean isBoundary(int a, int c, int k) {
        return (c - a == 1) || (a == 0 && c == k - 1);
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
