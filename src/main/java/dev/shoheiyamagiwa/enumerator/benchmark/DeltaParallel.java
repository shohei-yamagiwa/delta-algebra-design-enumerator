package dev.shoheiyamagiwa.enumerator.benchmark;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class DeltaParallel {
    public static Accumulator serial(int deltaCount, long seed, long sampleCount) {
        Sampler sampler = new Sampler(deltaCount);
        Accumulator accumulator = new Accumulator();

        for (long sampleIndex = 0; sampleIndex < sampleCount; sampleIndex++) {
            reduce(accumulator, sampler.eval(deltaCount, seed, sampleIndex));
        }

        return accumulator;
    }

    public static Accumulator parallel(int deltaCount, long seed, long sampleCount, int threadCount) throws Exception {
        ExecutorService threadPool = Executors.newFixedThreadPool(threadCount);
        List<Future<Accumulator>> futures = new ArrayList<>();
        long samplesPerThread = (sampleCount + threadCount - 1) / threadCount;

        for (int threadIndex = 0; threadIndex < threadCount; threadIndex++) {
            final long firstSampleIndex = threadIndex * samplesPerThread;
            final long lastSampleIndexExclusive = Math.min(sampleCount, firstSampleIndex + samplesPerThread);

            futures.add(threadPool.submit(() -> {
                Sampler sampler = new Sampler(deltaCount);
                Accumulator accumulator = new Accumulator();

                for (long sampleIndex = firstSampleIndex; sampleIndex < lastSampleIndexExclusive; sampleIndex++) {
                    reduce(accumulator, sampler.eval(deltaCount, seed, sampleIndex));
                }

                return accumulator;
            }));
        }

        Accumulator combinedAccumulator = new Accumulator();

        for (Future<Accumulator> future : futures) {
            merge(combinedAccumulator, future.get());
        }

        threadPool.shutdown();

        return combinedAccumulator;
    }

    private static void reduce(Accumulator accumulator, int violationCount) {
        if (violationCount < accumulator.bestViolationCount) {
            accumulator.bestViolationCount = violationCount;
        }

        if (violationCount == 0) {
            accumulator.satisfyingCount++;
        }

        accumulator.violationHistogram[Math.clamp(violationCount, 0, Accumulator.HISTOGRAM_CAPACITY - 1)]++;
    }

    private static void merge(Accumulator target, Accumulator source) {
        target.bestViolationCount = Math.min(target.bestViolationCount, source.bestViolationCount);
        target.satisfyingCount += source.satisfyingCount;

        for (int bucketIndex = 0; bucketIndex < Accumulator.HISTOGRAM_CAPACITY; bucketIndex++) {
            target.violationHistogram[bucketIndex] += source.violationHistogram[bucketIndex];
        }
    }
}
