package dev.shoheiyamagiwa.enumerator.benchmark;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class DeltaParallel {
    public static Acc serial(int deltaCount, long seed, long sampleCount) {
        Sampler sampler = new Sampler(deltaCount);
        Acc accumulator = new Acc();

        for (long sampleIndex = 0; sampleIndex < sampleCount; sampleIndex++) {
            reduce(accumulator, sampler.eval(deltaCount, seed, sampleIndex));
        }

        return accumulator;
    }

    public static Acc parallel(int deltaCount, long seed, long sampleCount, int threadCount) throws Exception {
        ExecutorService threadPool = Executors.newFixedThreadPool(threadCount);
        List<Future<Acc>> futures = new ArrayList<>();
        long samplesPerThread = (sampleCount + threadCount - 1) / threadCount;

        for (int threadIndex = 0; threadIndex < threadCount; threadIndex++) {
            final long firstSampleIndex = threadIndex * samplesPerThread;
            final long lastSampleIndexExclusive = Math.min(sampleCount, firstSampleIndex + samplesPerThread);

            futures.add(threadPool.submit(() -> {
                Sampler sampler = new Sampler(deltaCount);
                Acc accumulator = new Acc();

                for (long sampleIndex = firstSampleIndex; sampleIndex < lastSampleIndexExclusive; sampleIndex++) {
                    reduce(accumulator, sampler.eval(deltaCount, seed, sampleIndex));
                }

                return accumulator;
            }));
        }

        Acc combinedAccumulator = new Acc();

        for (Future<Acc> future : futures) {
            merge(combinedAccumulator, future.get());
        }

        threadPool.shutdown();

        return combinedAccumulator;
    }

    private static void reduce(Acc accumulator, int violationCount) {
        if (violationCount < accumulator.bestViolationCount) {
            accumulator.bestViolationCount = violationCount;
        }

        if (violationCount == 0) {
            accumulator.satisfyingCount++;
        }

        accumulator.violationHistogram[Math.clamp(violationCount, 0, Acc.HISTOGRAM_CAPACITY - 1)]++;
    }

    private static void merge(Acc target, Acc source) {
        target.bestViolationCount = Math.min(target.bestViolationCount, source.bestViolationCount);
        target.satisfyingCount += source.satisfyingCount;

        for (int bucketIndex = 0; bucketIndex < Acc.HISTOGRAM_CAPACITY; bucketIndex++) {
            target.violationHistogram[bucketIndex] += source.violationHistogram[bucketIndex];
        }
    }
}
