package dev.shoheiyamagiwa.enumerator.core;

import jdk.incubator.vector.LongVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

public class ReferenceSetAlgebra {
    public static final VectorSpecies<Long> LONG_SPECIES = LongVector.SPECIES_PREFERRED;

    private ReferenceSetAlgebra() {
        // No instantiation.
    }

    /**
     * Scalar composition of two reference-set-based deltas.
     * Reproduces CompositionDelta#combine's set algebra on a bitset representation.
     */
    public static void composeScalar(
            long[] useBits1, long[] defBits1,
            long[] useBits2, long[] defBits2,
            long[] outputUseBits, long[] outputDefBits) {

        int wordCount = useBits1.length;
        for (int wordIndex = 0; wordIndex < wordCount; wordIndex++) {
            outputUseBits[wordIndex] = (useBits1[wordIndex] | useBits2[wordIndex]) & ~defBits1[wordIndex];
            outputDefBits[wordIndex] = defBits2[wordIndex];
        }
    }

    /**
     * Vectorized composition — identical result to composeScalar, computed SIMD-wide.
     * The Java runtime lowers the lane-wise OR / AND / NOT to NEON (ARM64) or AVX (x86).
     */
    public static void composeVector(long[] useBits1, long[] defBits1, long[] useBits2, long[] defBits2, long[] outputUseBits, long[] outputDefBits) {
        int wordCount = useBits1.length;
        int vectorLoopBound = LONG_SPECIES.loopBound(wordCount);

        int wordIndex = 0;
        for (; wordIndex < vectorLoopBound; wordIndex += LONG_SPECIES.length()) {
            LongVector useVector1 = LongVector.fromArray(LONG_SPECIES, useBits1, wordIndex);
            LongVector useVector2 = LongVector.fromArray(LONG_SPECIES, useBits2, wordIndex);
            LongVector defVector1 = LongVector.fromArray(LONG_SPECIES, defBits1, wordIndex);

            LongVector composedUseVector = useVector1.or(useVector2).and(defVector1.not());   // (use1 | use2) & ~def1

            composedUseVector.intoArray(outputUseBits, wordIndex);
            LongVector.fromArray(LONG_SPECIES, defBits2, wordIndex).intoArray(outputDefBits, wordIndex);
        }

        for (; wordIndex < wordCount; wordIndex++) {                    // remainder (tail)
            outputUseBits[wordIndex] = (useBits1[wordIndex] | useBits2[wordIndex]) & ~defBits1[wordIndex];
            outputDefBits[wordIndex] = defBits2[wordIndex];
        }
    }

    /**
     * Composability test: is Def(delta1) INTERSECT Use(delta2) non-empty?
     * Vectorized OR-reduction over the AND of the two bitsets.
     */
    public static boolean isComposableVector(long[] defBits1, long[] useBits2) {
        int wordCount = defBits1.length;
        int vectorLoopBound = LONG_SPECIES.loopBound(wordCount);

        LongVector intersectionAccumulator = LongVector.zero(LONG_SPECIES);
        int wordIndex = 0;
        for (; wordIndex < vectorLoopBound; wordIndex += LONG_SPECIES.length()) {
            LongVector defVector1 = LongVector.fromArray(LONG_SPECIES, defBits1, wordIndex);
            LongVector useVector2 = LongVector.fromArray(LONG_SPECIES, useBits2, wordIndex);
            intersectionAccumulator = intersectionAccumulator.or(defVector1.and(useVector2));
        }
        long anyIntersectingBits = intersectionAccumulator.reduceLanes(VectorOperators.OR);

        for (; wordIndex < wordCount; wordIndex++) {
            anyIntersectingBits |= (defBits1[wordIndex] & useBits2[wordIndex]);
        }
        return anyIntersectingBits != 0L;
    }
}
