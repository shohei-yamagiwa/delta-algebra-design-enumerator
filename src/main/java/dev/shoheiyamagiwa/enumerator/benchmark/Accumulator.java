package dev.shoheiyamagiwa.enumerator.benchmark;

public final class Accumulator {
    public static final int HISTOGRAM_CAPACITY = 256;

    int bestViolationCount = Integer.MAX_VALUE;

    long satisfyingCount = 0;

    long[] violationHistogram = new long[HISTOGRAM_CAPACITY];
}