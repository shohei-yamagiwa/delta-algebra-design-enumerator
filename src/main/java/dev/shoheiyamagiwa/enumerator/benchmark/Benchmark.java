package dev.shoheiyamagiwa.enumerator.benchmark;

import dev.shoheiyamagiwa.enumerator.model.Polygon;
import dev.shoheiyamagiwa.enumerator.model.RefEdge;
import dev.shoheiyamagiwa.enumerator.model.Triangulation;

import java.util.*;

import static dev.shoheiyamagiwa.enumerator.benchmark.DesignEnumerator.triangulations;

public class Benchmark {
    private static final DesignEvaluator PLACEHOLDER = (p, tri, m) -> {
        int nDiag = tri.getDiagonals().length;
        long mask = (nDiag == 64) ? -1L : ((1L << nDiag) - 1);
        return Long.bitCount(m & mask) + (tri.getEars() - 2);
    };

    static void main() {
        // (1) sanity: triangulation count must equal Catalan(n)
        System.out.println("== triangulation count vs Catalan ==");

        for (int k = 3; k <= 10; k++) {
            int n = k - 2;
            long got = triangulations(k).size();

            System.out.printf("k=%2d (n=%2d): triangulations=%-8d catalan=%-8d %s%n", k, n, got, catalan(n), got == catalan(n) ? "OK" : "FAIL");
        }

        // (2) clock app: enumerate all 4 designs explicitly
        System.out.println("\n== clock app (n=2): all designs ==");

        Polygon clockApp = clockApp();
        List<Triangulation> ct = triangulations(clockApp.vertices());
        long dir = 1L << (clockApp.deltas() - 1);

        for (int t = 0; t < ct.size(); t++) {
            for (long m = 0; m < dir; m++) {
                int v = PLACEHOLDER.violations(clockApp, ct.get(t), m);

                System.out.printf("  id=%d  t=%d (ears=%d, diagonals=%s)  m=%d  violations=%d  %s%n", t * dir + m, t, ct.get(t).getEars(), Arrays.deepToString(ct.get(t).getDiagonals()), m, v, v == 0 ? "<- Demeter-satisfying" : "");
            }
        }

        // (3) single-core scaling baseline
        System.out.println("\n== single-core scaling baseline ==");
        System.out.printf("%-4s %-16s %-10s %-8s %-14s%n", "n", "N", "time(ms)", "minV", "designs/sec");

        for (int n = 4; n <= 12; n++) {
            Polygon p = syntheticGon(n);
            Result r = DesignEnumerator.enumerate(p, PLACEHOLDER);

            System.out.printf("%-4d %-16d %-10.1f %-8d %-14.3e%n", n, r.getN(), r.getMs(), r.getMinViol(), r.getN() / (r.getMs() / 1000.0));
        }

        // For large n
        long seed = 12345L;

        // (1) uniformity: Rémy must hit each of the C_4 = 14 triangulations equally often
        System.out.println("== uniformity check (n=4, 14 triangulations) ==");

        int nU = 4, S1 = 1_400_000;
        Map<String, Integer> freq = new HashMap<>();

        for (long i = 0; i < S1; i++) {
            List<int[]> tris = DesignSampler.remyTriangles(nU, DesignSampler.rngForSample(seed, i));
            List<int[]> diagonals = new ArrayList<>();

            DesignSampler.diagonalCountAndEars(tris, nU + 2, diagonals);

            StringBuilder sb = new StringBuilder();
            for (int[] diagonal : diagonals) {
                sb.append(diagonal[0]).append('-').append(diagonal[1]).append(',');
            }

            freq.merge(sb.toString(), 1, Integer::sum);
        }

        int mn = Integer.MAX_VALUE;
        int mx = 0;

        for (int f : freq.values()) {
            mn = Math.min(mn, f);
            mx = Math.max(mx, f);
        }

        System.out.printf("distinct triangulations hit=%d (expect 14), per-bucket expect=%.0f, min=%d max=%d%n", freq.size(), S1 / 14.0, mn, mx);

        // (2) validation: sampled estimates vs exact, small n
        System.out.println("\n== estimate vs exact ==");
        System.out.printf("%-4s %-12s %-14s %-14s %-10s %-10s%n", "n", "N", "satFrac(exact)", "satFrac(est)", "min(exact)", "min(est)");

        int S2 = 2_000_000;

        for (int n : new int[]{4, 6, 8}) {
            long[] ex = DesignSampler.exact(n);
            double satExact = (double) ex[1] / ex[2];
            long hit = 0;
            int estMin = Integer.MAX_VALUE;

            for (long i = 0; i < S2; i++) {
                int v = DesignSampler.sampleViolations(n, seed, i);

                if (v == 0) {
                    hit++;
                }
                if (v < estMin) {
                    estMin = v;
                }
            }

            System.out.printf("%-4d %-12d %-14.6f %-14.6f %-10d %-10d%n", n, ex[2], satExact, (double) hit / S2, ex[0], estMin);
        }

        // (3) large-n sampling: exhaustive is utterly impossible here
        System.out.println("\n== large-n sampling (exhaustive impossible) ==");
        System.out.printf("%-6s %-12s %-12s %-14s %-14s%n", "n", "samples", "time(ms)", "bestViol", "samples/sec");

        int S3 = 1_000_000;

        for (int n : new int[]{30, 60, 120, 200}) {
            long t0 = System.nanoTime();
            int best = Integer.MAX_VALUE;
            long satFound = 0;

            for (long i = 0; i < S3; i++) {
                int v = DesignSampler.sampleViolations(n, seed, i);
                if (v < best) {
                    best = v;
                }
                if (v == 0) {
                    satFound++;
                }
            }

            double ms = (System.nanoTime() - t0) / 1e6;

            System.out.printf("%-6d %-12d %-12.1f %-14d %-14.3e  (satisfying found: %d)%n", n, S3, ms, best, S3 / (ms / 1000.0), satFound);
        }

        System.out.println("\nNote: at large n, uniform samples almost never hit a satisfying design");
        System.out.println("(they are astronomically rare) -> motivates guided/importance sampling as future work.");
    }

    private static Polygon clockApp() {
        String[] names = {"DigitalView", "Clock", "Time", "Hour"};
        RefEdge[] refs = {
                new RefEdge("clock", 0, 1, true), // DigitalView -> Clock (field)
                new RefEdge("time", 1, 2, true), // Clock -> Time (field)
                new RefEdge("hour", 2, 3, true), // Time -> Hour (field)
                new RefEdge("h", 3, 0, false), // Hour -> DigitalView (local)
        };
        return new Polygon(names, refs);
    }

    static Polygon syntheticGon(int n) {
        int k = n + 2;
        String[] names = new String[k];
        RefEdge[] refs = new RefEdge[k];

        for (int i = 0; i < k; i++) {
            names[i] = "C" + i;
            refs[i] = new RefEdge("e" + i, i, (i + 1) % k, (i % 2 == 0));
        }

        return new Polygon(names, refs);
    }

    private static long catalan(int n) {
        long c = 1;

        for (int i = 0; i < n; i++) {
            c = c * 2 * (2L * i + 1) / (i + 2);
        }

        return c;
    }
}
