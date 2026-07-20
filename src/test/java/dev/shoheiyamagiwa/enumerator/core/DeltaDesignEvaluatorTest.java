package dev.shoheiyamagiwa.enumerator.core;

import dev.shoheiyamagiwa.enumerator.model.algebra.*;
import dev.shoheiyamagiwa.enumerator.model.algebra.Class;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DeltaDesignEvaluatorTest {
    @Test
    @DisplayName("デメテルの法則を満たしているデルタに対して violations=0 を返す")
    public void testLawOfDemeterSatisfiedDelta() {
        Class clock = new Class("Clock");
        Class digitalView = new Class("DigitalView");
        Class time = new Class("Time");
        Class hour = new Class("Hour");

        Reference clockToTime = new Reference(clock, time, ReferenceType.FIELD);
        Reference timeToHour = new Reference(time, hour, ReferenceType.FIELD);
        Reference clockToHour = new Reference(clock, hour, ReferenceType.LOCAL);
        PrimitiveDelta delta1 = new PrimitiveDelta(clockToTime, timeToHour, clockToHour);

        Reference digitalViewToClock = new Reference(digitalView, clock, ReferenceType.FIELD);
        Reference digitalViewToHour = new Reference(digitalView, hour, ReferenceType.LOCAL);
        PrimitiveDelta delta2 = new PrimitiveDelta(digitalViewToClock, clockToHour, digitalViewToHour);

        // TODO: It's not an ideal delta since the order of method calls is incorrect.
        CompositionDelta delta = delta1.combine(delta2);

        long violations = DeltaDesignEvaluator.evaluateLawOfDemeter(delta);

        assertEquals(0, violations);
    }

    @Test
    @DisplayName("デメテルの法則を満たしていないデルタに対して violations=1 を返す")
    public void testLawOfDemeterNotSatisfiedDelta() {
        Class clock = new Class("Clock");
        Class digitalView = new Class("DigitalView");
        Class time = new Class("Time");
        Class hour = new Class("Hour");

        Reference digitalViewToClock = new Reference(digitalView, clock, ReferenceType.FIELD);
        Reference clockToTime = new Reference(clock, time, ReferenceType.FIELD);
        Reference digitalViewToTime = new Reference(digitalView, time, ReferenceType.LOCAL);
        PrimitiveDelta delta1 = new PrimitiveDelta(digitalViewToClock, clockToTime, digitalViewToTime);

        Reference timeToHour = new Reference(time, hour, ReferenceType.FIELD);
        Reference digitalViewToHour = new Reference(digitalView, hour, ReferenceType.FIELD);
        PrimitiveDelta delta2 = new PrimitiveDelta(digitalViewToTime, timeToHour, digitalViewToHour);

        // TODO: It's not an ideal delta since the order of method calls is incorrect.
        CompositionDelta delta = delta1.combine(delta2);

        long violations = DeltaDesignEvaluator.evaluateLawOfDemeter(delta);

        assertEquals(1, violations);
    }
}
