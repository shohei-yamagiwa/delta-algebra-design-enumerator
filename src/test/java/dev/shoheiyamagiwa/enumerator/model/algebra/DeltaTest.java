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

        SequencedSet<Reference> usedRefsDelta1 = new LinkedHashSet<>(List.of(refAtoB, refBtoC));
        Delta delta1 = new Delta(usedRefsDelta1, refAtoC);

        SequencedSet<Reference> usedRefsDelta2 = new LinkedHashSet<>(List.of(refAtoC, refAtoD));
        Delta delta2 = new Delta(usedRefsDelta2, refDtoC);

        SequencedSet<Reference> expectedUsedRefs = new LinkedHashSet<>(List.of(refAtoB, refBtoC, refAtoD));
        SequencedSet<Reference> expectedSharedRefs = new LinkedHashSet<>(List.of(refAtoC));

        Delta expected = new Delta(expectedUsedRefs, refDtoC, expectedSharedRefs);
        Delta actual = delta1.combine(delta2);

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
        Delta delta1circ2 = new Delta(deltaUsedRefs, refDtoC, deltaSharedRefs);

        SequencedSet<Reference> usedRefsForDelta3 = new LinkedHashSet<>(List.of(refDtoC, refCtoE));
        Delta delta3 = new Delta(usedRefsForDelta3, refEtoD);

        SequencedSet<Reference> expectedUsedRefs = new LinkedHashSet<>(List.of(refAtoB, refBtoC, refAtoD, refCtoE));
        SequencedSet<Reference> expectedSharedRefs = new LinkedHashSet<>(List.of(refAtoC, refDtoC));

        Delta expected = new Delta(expectedUsedRefs, refEtoD, expectedSharedRefs);
        Delta actual = delta1circ2.combine(delta3);

        assertEquals(expected, actual);
    }
}