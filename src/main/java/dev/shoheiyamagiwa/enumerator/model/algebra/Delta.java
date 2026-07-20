package dev.shoheiyamagiwa.enumerator.model.algebra;

import java.util.Set;

public interface Delta {
    CompositionDelta combine(Delta other);

    boolean isOpen();

    Set<Reference> boundaries();

    Set<Reference> useRefs();

    Reference defRef();

    Set<Class> classes();
}
