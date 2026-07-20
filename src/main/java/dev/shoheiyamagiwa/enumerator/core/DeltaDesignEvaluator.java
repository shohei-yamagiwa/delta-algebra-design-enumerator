package dev.shoheiyamagiwa.enumerator.core;

import dev.shoheiyamagiwa.enumerator.model.algebra.Delta;

public class DeltaDesignEvaluator {
    private DeltaDesignEvaluator() {
        // No instantiation
    }

    /**
     * Evaluate if the given Delta satisfies the law of Demeter.
     *
     * @param delta The delta to be evaluated
     * @return The number of violations of the law of Demeter
     */
    public static long evaluateLawOfDemeter(Delta delta) {
        long violations = 0;
        return violations;
    }
}
