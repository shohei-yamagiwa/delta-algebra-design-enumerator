package dev.shoheiyamagiwa.enumerator.benchmark;

import dev.shoheiyamagiwa.enumerator.core.DesignEvaluator;
import dev.shoheiyamagiwa.enumerator.core.TriangulationUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DesignSampler {
    public static List<int[]> remyTriangles(int n, SplitMix64 random) {
        int NN = 2 * n + 1;
        int[] Lc = new int[NN];
        int[] Rc = new int[NN];
        int[] par = new int[NN];
        int[] side = new int[NN];
        boolean[] internal = new boolean[NN];

        Arrays.fill(Lc, -1);
        Arrays.fill(Rc, -1);
        Arrays.fill(par, -1);
        Arrays.fill(side, -1);

        int[] list = new int[NN];
        int count = 0;
        int root = 0;

        list[count++] = 0; // start: single leaf (node 0)

        for (int k = 1; k <= n; k++) {
            int intn = 2 * k - 1;
            int leaf = 2 * k;
            int i = list[random.nextInt(2 * k - 1)]; // uniform among current 2k-1 nodes
            int b = random.nextBit() ? 1 : 0;
            int p = par[i], ps = side[i];

            par[intn] = p;
            side[intn] = ps;
            internal[intn] = true;

            if (p == -1) {
                root = intn;
            } else {
                if (ps == 0) Lc[p] = intn;
                else Rc[p] = intn;
            }

            if (b == 0) {
                Lc[intn] = i;
                par[i] = intn;
                side[i] = 0;
                Rc[intn] = leaf;
                par[leaf] = intn;
                side[leaf] = 1;
            } else {
                Lc[intn] = leaf;
                par[leaf] = intn;
                side[leaf] = 0;
                Rc[intn] = i;
                par[i] = intn;
                side[i] = 1;
            }

            list[count++] = intn;
            list[count++] = leaf;
        }

        int[] leaves = new int[NN];

        countLeaves(root, internal, Lc, Rc, leaves);

        List<int[]> triangles = new ArrayList<>(n);
        buildTriangles(root, 0, n + 1, internal, Lc, Rc, leaves, triangles);

        return triangles;
    }

    private static int countLeaves(int node, boolean[] in, int[] Lc, int[] Rc, int[] leaves) {
        if (node == -1) {
            return 0;
        }

        if (!in[node]) {
            leaves[node] = 1;
            return 1;
        }

        int c = countLeaves(Lc[node], in, Lc, Rc, leaves) + countLeaves(Rc[node], in, Lc, Rc, leaves);
        leaves[node] = c;

        return c;
    }

    private static void buildTriangles(int node, int lo, int hi, boolean[] in, int[] Lc, int[] Rc, int[] leaves, List<int[]> out) {
        if (node == -1 || !in[node]) {
            return;
        }

        int mid = lo + leaves[Lc[node]];

        out.add(new int[]{lo, mid, hi});

        buildTriangles(Lc[node], lo, mid, in, Lc, Rc, leaves, out);
        buildTriangles(Rc[node], mid, hi, in, Lc, Rc, leaves, out);
    }

    public static SplitMix64 rngForSample(long globalSeed, long i) {
        return new SplitMix64(SplitMix64.mix64(globalSeed ^ SplitMix64.mix64(i)));
    }

    static int violationsFromParts(int setDirBits, int ears) {
        return DesignEvaluator.violations(setDirBits, ears);
    }

    static int sampleViolations(int n, long globalSeed, long i) {
        SplitMix64 rng = rngForSample(globalSeed, i);
        List<int[]> tris = remyTriangles(n, rng);
        int[] de = TriangulationUtils.diagonalCountAndEars(tris, n + 2, null); // {#diagonals, ears}
        int nd = de[0];
        int ears = de[1]; // nd == n-1
        int set = 0;

        for (int d = 0; d < nd; d++) {
            if (rng.nextBit()) set++; // directions
        }

        return violationsFromParts(set, ears);
    }

    public static long[] exact(int n) {
        int k = n + 2;
        int[] v = new int[k];

        for (int x = 0; x < k; x++) {
            v[x] = x;
        }

        List<List<int[]>> all = TriangulationUtils.triFill(v);
        long dir = 1L << (n - 1);
        int minV = Integer.MAX_VALUE;
        long sat = 0;

        for (List<int[]> tris : all) {
            int ears = TriangulationUtils.diagonalCountAndEars(tris, k, null)[1];

            for (long m = 0; m < dir; m++) {
                int viol = DesignEvaluator.violations(Long.bitCount(m), ears);

                if (viol < minV) {
                    minV = viol;
                }
                if (viol == 0) {
                    sat++;
                }
            }
        }
        return new long[]{minV, sat, (long) all.size() * dir};
    }
}
