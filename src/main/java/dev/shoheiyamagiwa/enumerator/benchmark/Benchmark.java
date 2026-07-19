package dev.shoheiyamagiwa.enumerator.benchmark;

import dev.shoheiyamagiwa.enumerator.model.Polygon;
import dev.shoheiyamagiwa.enumerator.model.RefEdge;
import dev.shoheiyamagiwa.enumerator.model.Triangulation;

import java.util.Arrays;
import java.util.List;

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
