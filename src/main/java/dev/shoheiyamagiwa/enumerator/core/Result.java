package dev.shoheiyamagiwa.enumerator.core;

public class Result {
    private final long totalDesignCount;

    private final int minViolations;

    private final long satisfyingDesignCount;

    private final double elapsedMillis;

    public Result(long totalDesignCount, int minViolations, long satisfyingDesignCount, double elapsedMillis) {
        this.totalDesignCount = totalDesignCount;
        this.minViolations = minViolations;
        this.satisfyingDesignCount = satisfyingDesignCount;
        this.elapsedMillis = elapsedMillis;
    }

    public long getTotalDesignCount() {
        return totalDesignCount;
    }

    public int getMinViolations() {
        return minViolations;
    }

    public long getSatisfyingDesignCount() {
        return satisfyingDesignCount;
    }

    public double getElapsedMillis() {
        return elapsedMillis;
    }
}
