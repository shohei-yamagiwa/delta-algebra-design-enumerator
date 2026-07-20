package dev.shoheiyamagiwa.enumerator.core;

import dev.shoheiyamagiwa.enumerator.model.algebra.CompositionDelta;
import dev.shoheiyamagiwa.enumerator.model.algebra.Delta;
import dev.shoheiyamagiwa.enumerator.model.algebra.PrimitiveDelta;
import dev.shoheiyamagiwa.enumerator.model.algebra.Reference;
import dev.shoheiyamagiwa.enumerator.model.algebra.ReferenceType;

import java.util.Iterator;

public class DeltaDesignEvaluator {
    private DeltaDesignEvaluator() {
        // No instantiation
    }

    /**
     * Evaluate if the given Delta satisfies the law of Demeter.
     * <p>
     * The law of Demeter forbids a method from calling methods of any class other than the ones reachable
     * through a field or a method argument. A primitive delta {@code (r1, r2, def)} with {@code r1: A -> B},
     * {@code r2: B -> C} and {@code def: A -> C} denotes a method of {@code A} that calls a method of {@code B}
     * on the object reached through {@code r1}. Hence the delta violates the law exactly when {@code r1} is a
     * local or a temporary reference, i.e. when the receiver was obtained as the result of another call.
     * A composition delta is evaluated as the sum over the primitive deltas it is composed of.
     *
     * @param delta The delta to be evaluated
     * @return The number of violations of the law of Demeter
     */
    public static long evaluateLawOfDemeter(Delta delta) {
        if (delta instanceof PrimitiveDelta primitiveDelta) {
            return violates(primitiveDelta) ? 1 : 0;
        }

        if (delta instanceof CompositionDelta compositionDelta) {
            long violations = 0;

            for (PrimitiveDelta primitiveDelta : compositionDelta.deltas()) {
                if (violates(primitiveDelta)) {
                    violations++;
                }
            }

            return violations;
        }

        throw new IllegalArgumentException("The given Delta cannot be evaluated!");
    }

    /**
     * Tell whether the receiver of the call denoted by the given primitive delta is reachable through a field
     * or a method argument.
     *
     * @param delta the primitive delta to be checked
     * @return {@code true} if the delta violates the law of Demeter
     */
    private static boolean violates(PrimitiveDelta delta) {
        ReferenceType type = receiverRef(delta).getType();

        return type == ReferenceType.LOCAL || type == ReferenceType.TEMPORARY;
    }

    /**
     * Find the use reference the call of the given primitive delta is made on. For a delta {@code (r1, r2, def)}
     * with {@code r1: A -> B}, {@code r2: B -> C} and {@code def: A -> C}, that is {@code r1}: the use reference
     * starting at the source of the definition reference and ending at the source of the other use reference.
     * The shape is derived from the references themselves rather than from their order, so that the orientation
     * assigned by the enumerator is respected.
     *
     * @param delta the primitive delta whose receiver reference is looked up
     * @return the use reference the call is made on
     */
    private static Reference receiverRef(PrimitiveDelta delta) {
        Iterator<Reference> it = delta.useRefs().iterator();
        Reference first = it.next();
        Reference second = it.next();
        Reference defRef = delta.defRef();

        if (first.getSource().equals(defRef.getSource()) && second.getSource().equals(first.getTarget())) {
            return first;
        }

        if (second.getSource().equals(defRef.getSource()) && first.getSource().equals(second.getTarget())) {
            return second;
        }

        throw new IllegalArgumentException("The given Delta is not a triangle of two chained references!");
    }
}
