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
        for (int deltaCount = 2; deltaCount <= 12; deltaCount++) {
            Sampler sampler = new Sampler(deltaCount);

            for (long sampleIndex = 0; sampleIndex < 200; sampleIndex++) {
                assertEquals(DesignSampler.sampleViolations(deltaCount, SEED, sampleIndex), sampler.eval(deltaCount, SEED, sampleIndex),
                        "n=" + deltaCount + " sample=" + sampleIndex);
            }
        }
    }

    @Test
    @DisplayName("マルチコア実行が直列実行と完全に一致する")
    public void parallelMatchesSerial() throws Exception {
        int deltaCount = 20;
        long sampleCount = 50_000;
        Acc serialResult = DeltaParallel.serial(deltaCount, SEED, sampleCount);

        for (int threadCount : new int[]{2, 3, 4, 8}) {
            Acc parallelResult = DeltaParallel.parallel(deltaCount, SEED, sampleCount, threadCount);

            assertEquals(serialResult.bestViolationCount, parallelResult.bestViolationCount, "best (threads=" + threadCount + ")");
            assertEquals(serialResult.satisfyingCount, parallelResult.satisfyingCount, "sat (threads=" + threadCount + ")");
            assertArrayEquals(serialResult.violationHistogram, parallelResult.violationHistogram, "hist (threads=" + threadCount + ")");
        }
    }

    @Test
    @DisplayName("標本数がスレッド数で割り切れなくても直列実行と一致する")
    public void parallelMatchesSerialOnUnevenChunks() throws Exception {
        int deltaCount = 8;
        long sampleCount = 9_973; // prime: never divides evenly by the thread counts below
        Acc serialResult = DeltaParallel.serial(deltaCount, SEED, sampleCount);

        for (int threadCount : new int[]{3, 7, 12}) {
            Acc parallelResult = DeltaParallel.parallel(deltaCount, SEED, sampleCount, threadCount);

            assertEquals(serialResult.satisfyingCount, parallelResult.satisfyingCount, "sat (threads=" + threadCount + ")");
            assertArrayEquals(serialResult.violationHistogram, parallelResult.violationHistogram, "hist (threads=" + threadCount + ")");
            assertEquals(sampleCount, Arrays.stream(parallelResult.violationHistogram).sum(), "every sample counted exactly once");
        }
    }

    @Test
    @DisplayName("二つの独立な全列挙実装が同じ厳密解を返す")
    public void bothExactEnumerationsAgree() {
        for (int deltaCount = 2; deltaCount <= 8; deltaCount++) {
            long[] expected = DesignSampler.exact(deltaCount); // {minViolations, satisfying, total}
            Result actual = DesignEnumerator.enumerate(polygonOf(deltaCount), DesignEvaluator.STANDARD);

            assertEquals(expected[2], actual.getTotalDesignCount(), "total designs (n=" + deltaCount + ")");
            assertEquals(expected[0], actual.getMinViolations(), "min violations (n=" + deltaCount + ")");
            assertEquals(expected[1], actual.getSatisfyingDesignCount(), "satisfying designs (n=" + deltaCount + ")");
        }
    }

    /**
     * Builds a boundary polygon for {@code deltaCount} primitive deltas. The standard evaluator is
     * structure-only, so the edge names and reference types carry no meaning here.
     *
     * @param deltaCount the number of primitive deltas, i.e. the polygon has {@code deltaCount + 2} vertices
     * @return a polygon on {@code deltaCount + 2} vertices with a field reference along each boundary edge
     */
    private static Polygon polygonOf(int deltaCount) {
        int vertexCount = deltaCount + 2;
        String[] classNames = new String[vertexCount];
        RefEdge[] boundary = new RefEdge[vertexCount];

        for (int vertexIndex = 0; vertexIndex < vertexCount; vertexIndex++) {
            classNames[vertexIndex] = "C" + vertexIndex;
            boundary[vertexIndex] = new RefEdge("r" + vertexIndex, vertexIndex, (vertexIndex + 1) % vertexCount, true);
        }

        return new Polygon(classNames, boundary);
    }
}
