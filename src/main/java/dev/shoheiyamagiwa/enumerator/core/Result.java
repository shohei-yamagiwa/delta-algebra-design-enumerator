package dev.shoheiyamagiwa.enumerator.core;

public class Result {
    private final long N;

    private final int minViol;

    private final long satCount;

    private final double ms;

    public Result(long N, int minViol, long satCount, double ms) {
        this.N = N;
        this.minViol = minViol;
        this.satCount = satCount;
        this.ms = ms;
    }

    public long getN() {
        return N;
    }

    public int getMinViol() {
        return minViol;
    }

    public long getSatCount() {
        return satCount;
    }

    public double getMs() {
        return ms;
    }
}
