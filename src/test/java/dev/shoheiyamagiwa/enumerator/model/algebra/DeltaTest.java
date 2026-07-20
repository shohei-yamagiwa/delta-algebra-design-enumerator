package dev.shoheiyamagiwa.enumerator.model.algebra;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DeltaTest {
    @Test
    @DisplayName("原始デルタ同士の合成が正しくできる")
    public void combinePrimitiveDeltas() {
        Class classA = new Class("A");
        Class classB = new Class("B");
        Class classC = new Class("C");
        Class classD = new Class("D");

        Reference refAtoB = new Reference(classA, classB, ReferenceType.FIELD);
        Reference refBtoC = new Reference(classB, classC, ReferenceType.FIELD);
        Reference refAtoC = new Reference(classA, classC, ReferenceType.LOCAL);
        Reference refAtoD = new Reference(classA, classD, ReferenceType.FIELD);
        Reference refDtoC = new Reference(classD, classC, ReferenceType.FIELD);

        PrimitiveDelta primitiveDelta1 = new PrimitiveDelta(refAtoB, refBtoC, refAtoC);
        PrimitiveDelta primitiveDelta2 = new PrimitiveDelta(refAtoC, refAtoD, refDtoC);

        SequencedSet<Reference> expectedUsedRefs = new LinkedHashSet<>(List.of(refAtoB, refBtoC, refAtoD));
        SequencedSet<Reference> expectedSharedRefs = new LinkedHashSet<>(List.of(refAtoC));

        CompositionDelta expected = new CompositionDelta(expectedUsedRefs, refDtoC, expectedSharedRefs);
        CompositionDelta actual = primitiveDelta1.combine(primitiveDelta2);

        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("原始デルタとデルタの合成が正しくできる")
    public void combineDeltas() {
        Class classA = new Class("A");
        Class classB = new Class("B");
        Class classC = new Class("C");
        Class classD = new Class("D");
        Class classE = new Class("E");

        Reference refAtoB = new Reference(classA, classB, ReferenceType.FIELD);
        Reference refBtoC = new Reference(classB, classC, ReferenceType.FIELD);
        Reference refAtoC = new Reference(classA, classC, ReferenceType.LOCAL);
        Reference refAtoD = new Reference(classA, classD, ReferenceType.FIELD);
        Reference refDtoC = new Reference(classD, classC, ReferenceType.LOCAL);
        Reference refCtoE = new Reference(classC, classE, ReferenceType.FIELD);
        Reference refEtoD = new Reference(classE, classD, ReferenceType.FIELD);

        SequencedSet<Reference> deltaUsedRefs = new LinkedHashSet<>(List.of(refAtoB, refBtoC, refAtoD));
        SequencedSet<Reference> deltaSharedRefs = new LinkedHashSet<>(List.of(refAtoC));
        CompositionDelta compositionDelta1Circ2 = new CompositionDelta(deltaUsedRefs, refDtoC, deltaSharedRefs);

        PrimitiveDelta primitiveDelta3 = new PrimitiveDelta(refDtoC, refCtoE, refEtoD);

        SequencedSet<Reference> expectedUsedRefs = new LinkedHashSet<>(List.of(refAtoB, refBtoC, refAtoD, refCtoE));
        SequencedSet<Reference> expectedSharedRefs = new LinkedHashSet<>(List.of(refAtoC, refDtoC));

        CompositionDelta expected = new CompositionDelta(expectedUsedRefs, refEtoD, expectedSharedRefs);
        CompositionDelta actual = compositionDelta1Circ2.combine(primitiveDelta3);

        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("同値だが別インスタンスの参照からなるデルタが等しいと判定される")
    public void combineDeltasBuiltFromEqualButDistinctReferences() {
        Reference refAtoB = new Reference(new Class("A"), new Class("B"), ReferenceType.FIELD);
        Reference refBtoC = new Reference(new Class("B"), new Class("C"), ReferenceType.FIELD);
        Reference refAtoC = new Reference(new Class("A"), new Class("C"), ReferenceType.LOCAL);
        Reference refAtoD = new Reference(new Class("A"), new Class("D"), ReferenceType.FIELD);
        Reference refDtoC = new Reference(new Class("D"), new Class("C"), ReferenceType.FIELD);

        PrimitiveDelta primitiveDelta1 = new PrimitiveDelta(refAtoB, refBtoC, refAtoC);
        PrimitiveDelta primitiveDelta2 = new PrimitiveDelta(refAtoC, refAtoD, refDtoC);

        // Rebuilt from scratch: no instance is shared with the deltas above.
        SequencedSet<Reference> expectedUsedRefs = new LinkedHashSet<>(List.of(
                new Reference(new Class("A"), new Class("B"), ReferenceType.FIELD),
                new Reference(new Class("B"), new Class("C"), ReferenceType.FIELD),
                new Reference(new Class("A"), new Class("D"), ReferenceType.FIELD)));
        SequencedSet<Reference> expectedSharedRefs = new LinkedHashSet<>(List.of(
                new Reference(new Class("A"), new Class("C"), ReferenceType.LOCAL)));
        Reference expectedDefRef = new Reference(new Class("D"), new Class("C"), ReferenceType.FIELD);

        CompositionDelta expected = new CompositionDelta(expectedUsedRefs, expectedDefRef, expectedSharedRefs);
        CompositionDelta actual = primitiveDelta1.combine(primitiveDelta2);

        assertEquals(expected, actual);
    }
}