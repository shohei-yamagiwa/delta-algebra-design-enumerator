package dev.shoheiyamagiwa.enumerator.core;

import dev.shoheiyamagiwa.enumerator.model.Polygon;
import dev.shoheiyamagiwa.enumerator.model.Triangulation;

import java.util.ArrayList;
import java.util.List;

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

        List<List<int[]>> raw = TriangulationUtils.triFill(vertices);
        List<Triangulation> out = new ArrayList<>(raw.size());

        for (List<int[]> tris : raw) {
            out.add(finish(tris, k));
        }

        return out;
    }

    /**
     * Derive diagonals (canonical order) + ear count from a triangle list.
     *
     * @param triangles the triangles forming the triangulation, each represented as an array of three vertex indices
     * @param k         the number of vertices of the convex polygon
     * @return the resulting {@link Triangulation}, containing the triangles, canonical diagonals and ear count
     */
    private static Triangulation finish(List<int[]> triangles, int k) {
        List<int[]> diagonalsList = new ArrayList<>();
        int[] counts = TriangulationUtils.diagonalCountAndEars(triangles, k, diagonalsList);

        return new Triangulation(triangles.toArray(new int[0][]), diagonalsList.toArray(new int[0][]), counts[1]);
    }
}
