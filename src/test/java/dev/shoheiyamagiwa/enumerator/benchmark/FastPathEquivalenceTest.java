package dev.shoheiyamagiwa.enumerator.benchmark;

import dev.shoheiyamagiwa.enumerator.core.DesignEnumerator;
import dev.shoheiyamagiwa.enumerator.core.DesignEvaluator;
import dev.shoheiyamagiwa.enumerator.core.Result;
import dev.shoheiyamagiwa.enumerator.model.Polygon;
import dev.shoheiyamagiwa.enumerator.model.RefEdge;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pins down the invariant the whole project rests on: the fast paths must agree with the slow
 * references they were derived from. These assertions are deliberately independent of the
 * evaluation rule itself, so that swapping the placeholder evaluator for the real law-of-Demeter
 * one cannot silently break the sampling or the parallel accumulation.
 */
public class FastPathEquivalenceTest {
    private static final long SEED = 12345L;

    @Test
    @DisplayName("ゼロアロケーションの Sampler が遅い基準の DesignSampler と標本ごとに一致する")
    public void samplerMatchesDesignSamplerPerSample() {
        for (int n = 2; n <= 12; n++) {
            Sampler sampler = new Sampler(n);

            for (long i = 0; i < 200; i++) {
                assertEquals(DesignSampler.sampleViolations(n, SEED, i), sampler.eval(n, SEED, i),
                        "n=" + n + " sample=" + i);
            }
        }
    }

    @Test
    @DisplayName("マルチコア実行が直列実行と完全に一致する")
    public void parallelMatchesSerial() throws Exception {
        int n = 20;
        long samples = 50_000;
        Acc serial = DeltaParallel.serial(n, SEED, samples);

        for (int threads : new int[]{2, 3, 4, 8}) {
            Acc parallel = DeltaParallel.parallel(n, SEED, samples, threads);

            assertEquals(serial.best, parallel.best, "best (threads=" + threads + ")");
            assertEquals(serial.sat, parallel.sat, "sat (threads=" + threads + ")");
            assertArrayEquals(serial.hist, parallel.hist, "hist (threads=" + threads + ")");
        }
    }

    @Test
    @DisplayName("標本数がスレッド数で割り切れなくても直列実行と一致する")
    public void parallelMatchesSerialOnUnevenChunks() throws Exception {
        int n = 8;
        long samples = 9_973; // prime: never divides evenly by the thread counts below
        Acc serial = DeltaParallel.serial(n, SEED, samples);

        for (int threads : new int[]{3, 7, 12}) {
            Acc parallel = DeltaParallel.parallel(n, SEED, samples, threads);

            assertEquals(serial.sat, parallel.sat, "sat (threads=" + threads + ")");
            assertArrayEquals(serial.hist, parallel.hist, "hist (threads=" + threads + ")");
            assertEquals(samples, Arrays.stream(parallel.hist).sum(), "every sample counted exactly once");
        }
    }

    @Test
    @DisplayName("二つの独立な全列挙実装が同じ厳密解を返す")
    public void bothExactEnumerationsAgree() {
        for (int n = 2; n <= 8; n++) {
            long[] expected = DesignSampler.exact(n); // {minViolations, satisfying, total}
            Result actual = DesignEnumerator.enumerate(polygonOf(n), DesignEvaluator.STANDARD);

            assertEquals(expected[2], actual.getN(), "total designs (n=" + n + ")");
            assertEquals(expected[0], actual.getMinViol(), "min violations (n=" + n + ")");
            assertEquals(expected[1], actual.getSatCount(), "satisfying designs (n=" + n + ")");
        }
    }

    /**
     * Builds a boundary polygon for {@code n} primitive deltas. The standard evaluator is
     * structure-only, so the edge names and reference types carry no meaning here.
     *
     * @param n the number of primitive deltas, i.e. the polygon has {@code n + 2} vertices
     * @return a polygon on {@code n + 2} vertices with a field reference along each boundary edge
     */
    private static Polygon polygonOf(int n) {
        int k = n + 2;
        String[] classNames = new String[k];
        RefEdge[] boundary = new RefEdge[k];

        for (int v = 0; v < k; v++) {
            classNames[v] = "C" + v;
            boundary[v] = new RefEdge("r" + v, v, (v + 1) % k, true);
        }

        return new Polygon(classNames, boundary);
    }
}
