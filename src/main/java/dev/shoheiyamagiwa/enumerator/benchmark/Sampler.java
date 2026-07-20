package dev.shoheiyamagiwa.enumerator.benchmark;

import dev.shoheiyamagiwa.enumerator.core.DesignEvaluator;
import dev.shoheiyamagiwa.enumerator.core.TriangulationUtils;

/**
 * Zero-allocation, reusable-buffer counterpart of {@link DesignSampler}'s Rémy-tree sampling, used
 * by {@link DeltaParallel} for high-throughput (parallel) benchmarking. Its Rémy-tree construction
 * and ear-counting recursion intentionally duplicate the algorithms in {@link DesignSampler} and
 * {@link TriangulationUtils}: unlike those, this class reuses pre-allocated instance arrays across
 * calls to {@link #eval} (instead of allocating a fresh {@code List}/arrays per sample), which is
 * the whole point of this class on the hot sampling path. The RNG mixing step ({@link #mix64}) and
 * the boundary-edge test ({@link #isBnd}) have no such allocation concern, so they simply delegate
 * to {@link SplitMix64#mix64} and {@link TriangulationUtils#isBoundary} respectively.
 */
public class Sampler {
    private static final long GAMMA = 0x9E3779B97F4A7C15L;

    private final int[] Lc, Rc, par, side, list, leaves;
    private final boolean[] internal;
    private int earCount;

    int gk; // full polygon size k = n+2

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
        long s = mix64(seed ^ mix64(i)); // counter-based per-sample state
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

            long r1 = mix64(s);
            int pick = (int) Math.floorMod(r1, 2 * k - 1);

            s += GAMMA;

            long r2 = mix64(s);
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
        countEars(root, 0, n + 1);

        int ears = earCount, nd = n - 1, set = 0;

        for (int d = 0; d < nd; d++) {
            s += GAMMA;

            long r = mix64(s);

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

    void countEars(int node, int lo, int hi) {
        if (node == -1 || !internal[node]) {
            return;
        }

        int mid = lo + leaves[Lc[node]], k = hi - lo + 1; // NOTE: k here is local arc, boundary test uses global (0..n+1)
        int b = 0;

        if (isBnd(lo, mid)) {
            b++;
        }

        if (isBnd(mid, hi)) {
            b++;
        }

        if (isBnd(lo, hi)) {
            b++;
        }

        if (b >= 2) {
            earCount++;
        }

        countEars(Lc[node], lo, mid);
        countEars(Rc[node], mid, hi);
    }

    public static long mix64(long z) {
        return SplitMix64.mix64(z);
    }

    boolean isBnd(int a, int c) {
        return TriangulationUtils.isBoundary(a, c, gk);
    }
}
