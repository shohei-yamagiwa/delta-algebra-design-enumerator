package dev.shoheiyamagiwa.enumerator.benchmark;

import dev.shoheiyamagiwa.enumerator.core.DesignEnumerator;
import dev.shoheiyamagiwa.enumerator.core.DesignEvaluator;
import dev.shoheiyamagiwa.enumerator.core.Result;
import dev.shoheiyamagiwa.enumerator.model.Polygon;
import dev.shoheiyamagiwa.enumerator.model.RefEdge;
import dev.shoheiyamagiwa.enumerator.model.Triangulation;

import java.util.Arrays;
import java.util.List;

import static dev.shoheiyamagiwa.enumerator.core.DesignEnumerator.triangulations;

/**
 * Sanity checks about the exhaustive triangulation enumeration itself: that the enumerated count
 * matches the known Catalan numbers, that a small hand-checkable example ("clock app") produces the
 * expected Demeter-satisfying designs, and how exhaustive enumeration scales with {@code n}.
 */
public class TriangulationSanityChecks {
    /**
     * Runs all triangulation sanity checks, printing their results to standard output.
     */
    public static void run() {
        checkCatalanCounts();
        demoClockApp();
        measureScalingBaseline();
    }

    /**
     * Checks that the number of enumerated triangulations of a {@code k}-gon equals the Catalan
     * number {@code C(k - 2)}, for {@code k} from 3 to 10.
     */
    private static void checkCatalanCounts() {
        System.out.println("== triangulation count vs Catalan ==");

        for (int k = 3; k <= 10; k++) {
            int n = k - 2;
            long got = triangulations(k).size();

            System.out.printf("k=%2d (n=%2d): triangulations=%-8d catalan=%-8d %s%n", k, n, got, catalan(n), got == catalan(n) ? "OK" : "FAIL");
        }
    }

    /**
     * Enumerates all designs of the small "clock app" example (n=2) explicitly, printing which of
     * them satisfy the Demeter rule (zero violations).
     */
    private static void demoClockApp() {
        System.out.println("\n== clock app (n=2): all designs ==");

        Polygon clockApp = clockApp();
        List<Triangulation> ct = triangulations(clockApp.vertices());
        long dir = 1L << (clockApp.deltas() - 1);

        for (int t = 0; t < ct.size(); t++) {
            for (long m = 0; m < dir; m++) {
                int v = DesignEvaluator.STANDARD.violations(clockApp, ct.get(t), m);

                System.out.printf("  id=%d  t=%d (ears=%d, diagonals=%s)  m=%d  violations=%d  %s%n", t * dir + m, t, ct.get(t).getEars(), Arrays.deepToString(ct.get(t).getDiagonals()), m, v, v == 0 ? "<- Demeter-satisfying" : "");
            }
        }
    }

    /**
     * Measures how long exhaustive enumeration takes on synthetic polygons of increasing size
     * (n=4..12), printing the resulting throughput.
     */
    private static void measureScalingBaseline() {
        System.out.println("\n== single-core scaling baseline ==");
        System.out.printf("%-4s %-16s %-10s %-8s %-14s%n", "n", "N", "time(ms)", "minV", "designs/sec");

        for (int n = 4; n <= 12; n++) {
            Polygon p = syntheticGon(n);
            Result r = DesignEnumerator.enumerate(p, DesignEvaluator.STANDARD);

            System.out.printf("%-4d %-16d %-10.1f %-8d %-14.3e%n", n, r.getN(), r.getMs(), r.getMinViol(), r.getN() / (r.getMs() / 1000.0));
        }
    }

    /**
     * Builds the small "clock app" example: a 4-class design (DigitalView -&gt; Clock -&gt; Time -&gt;
     * Hour -&gt; DigitalView) with alternating field/local references.
     *
     * @return the "clock app" polygon
     */
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

    /**
     * Builds a synthetic {@code (n + 2)}-gon with alternating field/local references, used as a
     * generic input of a given size for enumeration.
     *
     * @param n the number of diagonals/deltas of the resulting polygon
     * @return the synthetic polygon
     */
    private static Polygon syntheticGon(int n) {
        int k = n + 2;
        String[] names = new String[k];
        RefEdge[] refs = new RefEdge[k];

        for (int i = 0; i < k; i++) {
            names[i] = "C" + i;
            refs[i] = new RefEdge("e" + i, i, (i + 1) % k, (i % 2 == 0));
        }
        return new Polygon(names, refs);
    }

    /**
     * Computes the {@code n}-th Catalan number.
     *
     * @param n the index of the Catalan number to compute
     * @return the {@code n}-th Catalan number
     */
    private static long catalan(int n) {
        long c = 1;

        for (int i = 0; i < n; i++) {
            c = c * 2 * (2L * i + 1) / (i + 2);
        }
        return c;
    }
}
