package dev.shoheiyamagiwa.enumerator.benchmark;

public final class Acc {
    public static final int HCAP = 256;

    int best = Integer.MAX_VALUE;

    long sat = 0;

    long[] hist = new long[HCAP];
}