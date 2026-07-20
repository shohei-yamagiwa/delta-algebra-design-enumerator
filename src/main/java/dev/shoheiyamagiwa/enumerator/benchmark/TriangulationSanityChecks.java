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

        for (int vertexCount = 3; vertexCount <= 10; vertexCount++) {
            int deltaCount = vertexCount - 2;
            long triangulationCount = triangulations(vertexCount).size();

            System.out.printf("k=%2d (n=%2d): triangulations=%-8d catalan=%-8d %s%n", vertexCount, deltaCount,
                    triangulationCount, catalan(deltaCount), triangulationCount == catalan(deltaCount) ? "OK" : "FAIL");
        }
    }

    /**
     * Enumerates all designs of the small "clock app" example (n=2) explicitly, printing which of
     * them satisfy the Demeter rule (zero violations).
     */
    private static void demoClockApp() {
        System.out.println("\n== clock app (n=2): all designs ==");

        Polygon clockApp = clockApp();
        List<Triangulation> clockAppTriangulations = triangulations(clockApp.vertices());
        long directionCombinationCount = 1L << (clockApp.deltas() - 1);

        for (int triangulationIndex = 0; triangulationIndex < clockAppTriangulations.size(); triangulationIndex++) {
            for (long directionBits = 0; directionBits < directionCombinationCount; directionBits++) {
                Triangulation triangulation = clockAppTriangulations.get(triangulationIndex);
                int violations = DesignEvaluator.STANDARD.violations(clockApp, triangulation, directionBits);
                long designId = triangulationIndex * directionCombinationCount + directionBits;

                System.out.printf("  id=%d  t=%d (ears=%d, diagonals=%s)  m=%d  violations=%d  %s%n", designId,
                        triangulationIndex, triangulation.getEars(), Arrays.deepToString(triangulation.getDiagonals()),
                        directionBits, violations, violations == 0 ? "<- Demeter-satisfying" : "");
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

        for (int deltaCount = 4; deltaCount <= 12; deltaCount++) {
            Polygon polygon = syntheticGon(deltaCount);
            Result result = DesignEnumerator.enumerate(polygon, DesignEvaluator.STANDARD);

            System.out.printf("%-4d %-16d %-10.1f %-8d %-14.3e%n", deltaCount, result.getTotalDesignCount(),
                    result.getElapsedMillis(), result.getMinViolations(),
                    result.getTotalDesignCount() / (result.getElapsedMillis() / 1000.0));
        }
    }

    /**
     * Builds the small "clock app" example: a 4-class design (DigitalView -&gt; Clock -&gt; Time -&gt;
     * Hour -&gt; DigitalView) with alternating field/local references.
     *
     * @return the "clock app" polygon
     */
    private static Polygon clockApp() {
        String[] classNames = {"DigitalView", "Clock", "Time", "Hour"};
        RefEdge[] boundary = {
                new RefEdge("clock", 0, 1, true), // DigitalView -> Clock (field)
                new RefEdge("time", 1, 2, true), // Clock -> Time (field)
                new RefEdge("hour", 2, 3, true), // Time -> Hour (field)
                new RefEdge("h", 3, 0, false), // Hour -> DigitalView (local)
        };
        return new Polygon(classNames, boundary);
    }

    /**
     * Builds a synthetic {@code (n + 2)}-gon with alternating field/local references, used as a
     * generic input of a given size for enumeration.
     *
     * @param deltaCount the number of diagonals/deltas of the resulting polygon
     * @return the synthetic polygon
     */
    private static Polygon syntheticGon(int deltaCount) {
        int vertexCount = deltaCount + 2;
        String[] classNames = new String[vertexCount];
        RefEdge[] boundary = new RefEdge[vertexCount];

        for (int vertexIndex = 0; vertexIndex < vertexCount; vertexIndex++) {
            classNames[vertexIndex] = "C" + vertexIndex;
            boundary[vertexIndex] = new RefEdge("e" + vertexIndex, vertexIndex, (vertexIndex + 1) % vertexCount, (vertexIndex % 2 == 0));
        }
        return new Polygon(classNames, boundary);
    }

    /**
     * Computes the {@code catalanIndex}-th Catalan number.
     *
     * @param catalanIndex the index of the Catalan number to compute
     * @return the {@code catalanIndex}-th Catalan number
     */
    private static long catalan(int catalanIndex) {
        long catalanNumber = 1;

        for (int index = 0; index < catalanIndex; index++) {
            catalanNumber = catalanNumber * 2 * (2L * index + 1) / (index + 2);
        }
        return catalanNumber;
    }
}
