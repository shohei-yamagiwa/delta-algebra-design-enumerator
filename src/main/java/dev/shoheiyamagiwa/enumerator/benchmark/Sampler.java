package dev.shoheiyamagiwa.enumerator.benchmark;

import dev.shoheiyamagiwa.enumerator.core.DesignEvaluator;
import dev.shoheiyamagiwa.enumerator.core.TriangulationUtils;

/**
 * Zero-allocation, reusable-buffer counterpart of {@link DesignSampler}'s Rémy-tree sampling, used
 * by {@link DeltaParallel} for high-throughput (parallel) benchmarking. Its Rémy-tree construction
 * and ear-counting recursion intentionally duplicate the algorithms in {@link DesignSampler} and
 * {@link TriangulationUtils}: unlike those, this class reuses pre-allocated instance arrays across
 * calls to {@link #eval} (instead of allocating a fresh {@code List}/arrays per sample), which is
 * the whole point of this class on the hot sampling path. The RNG mixing step and
 * the boundary-edge test have no such allocation concern, so they simply delegate
 * to {@link SplitMix64#mix64} and {@link TriangulationUtils#isBoundary} respectively.
 */
public class Sampler {
    private static final long GAMMA = 0x9E3779B97F4A7C15L;

    private final int[] Lc, Rc, par, side, list, leaves;
    private final boolean[] internal;
    private int earCount;

    public Sampler(int maxN) {
        int NN = 2 * maxN + 1;

        Lc = new int[NN];
        Rc = new int[NN];
        par = new int[NN];
        side = new int[NN];
        list = new int[NN];
        leaves = new int[NN];
        internal = new boolean[NN];
    }

    /**
     * violations for sample i — no heap allocation. RNG draw order matches DeltaSampler.
     */
    public int eval(int n, long seed, long i) {
        long s = SplitMix64.mix64(seed ^ SplitMix64.mix64(i)); // counter-based per-sample state
        int NN = 2 * n + 1;

        for (int x = 0; x < NN; x++) {
            Lc[x] = -1;
            Rc[x] = -1;
            par[x] = -1;
            side[x] = -1;
            internal[x] = false;
        }

        int cnt = 0, root = 0;

        list[cnt++] = 0;

        for (int k = 1; k <= n; k++) {
            int intn = 2 * k - 1, leaf = 2 * k;

            s += GAMMA;

            long r1 = SplitMix64.mix64(s);
            int pick = Math.floorMod(r1, 2 * k - 1);

            s += GAMMA;

            long r2 = SplitMix64.mix64(s);
            int b = (int) (r2 & 1L);
            int iN = list[pick];
            int p = par[iN], ps = side[iN];

            par[intn] = p;
            side[intn] = ps;
            internal[intn] = true;

            if (p == -1) {
                root = intn;
            } else {
                if (ps == 0) {
                    Lc[p] = intn;
                } else {
                    Rc[p] = intn;
                }
            }

            if (b == 0) {
                Lc[intn] = iN;
                par[iN] = intn;
                side[iN] = 0;
                Rc[intn] = leaf;
                par[leaf] = intn;
                side[leaf] = 1;
            } else {
                Lc[intn] = leaf;
                par[leaf] = intn;
                side[leaf] = 0;
                Rc[intn] = iN;
                par[iN] = intn;
                side[iN] = 1;
            }

            list[cnt++] = intn;
            list[cnt++] = leaf;
        }

        leafCount(root);
        earCount = 0;
        countEars(root, 0, n + 1, n + 2);

        int ears = earCount, nd = n - 1, set = 0;

        for (int d = 0; d < nd; d++) {
            s += GAMMA;

            long r = SplitMix64.mix64(s);

            if ((r & 1L) != 0) {
                set++;
            }
        }

        return DesignEvaluator.violations(set, ears);
    }

    int leafCount(int node) {
        if (node == -1) {
            return 0;
        }

        if (!internal[node]) {
            leaves[node] = 1;
            return 1;
        }

        int c = leafCount(Lc[node]) + leafCount(Rc[node]);

        leaves[node] = c;

        return c;
    }

    /**
     * @param node the subtree whose triangles are counted
     * @param lo   the first vertex of the arc this subtree spans
     * @param hi   the last vertex of the arc this subtree spans
     * @param k    the size of the *whole* polygon (n+2), not of the local arc: the boundary test has
     *             to recognise the closing edge (0, k-1), which a local arc cannot tell apart from a
     *             diagonal
     */
    void countEars(int node, int lo, int hi, int k) {
        if (node == -1 || !internal[node]) {
            return;
        }

        int mid = lo + leaves[Lc[node]];
        int b = 0;

        if (TriangulationUtils.isBoundary(lo, mid, k)) {
            b++;
        }
        if (TriangulationUtils.isBoundary(mid, hi, k)) {
            b++;
        }
        if (TriangulationUtils.isBoundary(lo, hi, k)) {
            b++;
        }

        if (b >= 2) {
            earCount++;
        }

        countEars(Lc[node], lo, mid, k);
        countEars(Rc[node], mid, hi, k);
    }
}
