package dev.shoheiyamagiwa.enumerator.model.algebra;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.SequencedSet;
import java.util.Set;

public class PrimitiveDelta implements Delta {
    private final Reference useRef0;
    private final Reference useRef1;
    private final Reference defRef;

    public PrimitiveDelta(Reference useRef0, Reference useRef1, Reference defRef) {
        this.useRef0 = useRef0;
        this.useRef1 = useRef1;
        this.defRef = defRef;
    }

    @Override
    public CompositionDelta combine(Delta that) {
        if (that instanceof PrimitiveDelta primitiveDelta) {
            return combine(primitiveDelta);
        }

        if (that instanceof CompositionDelta compositionDelta) {
            return combine(compositionDelta);
        }

        throw new IllegalArgumentException("The given Delta cannot be composed with this Delta!");
    }

    private CompositionDelta combine(PrimitiveDelta that) {
        // Checking if the given Delta can be composed with this Delta
        if (this.defRef.isField()) {
            throw new IllegalStateException("This Delta cannot be composed with an another Delta!");
        }

        Reference sharedRef = null;

        for (Reference ref : that.useRefs()) {
            if (ref.equals(this.defRef())) {
                sharedRef = ref;
                break;
            }
        }

        if (sharedRef == null) {
            throw new IllegalArgumentException("The given Delta cannot be composed with this Delta!");
        }

        // Use refs
        SequencedSet<Reference> useRefs = new LinkedHashSet<>();

        for (Reference useRef : this.useRefs()) {
            useRefs.addLast(useRef);
        }

        for (Reference useRef : that.useRefs()) {
            if (useRef.equals(sharedRef)) {
                continue;
            }

            useRefs.addLast(useRef);
        }

        // Shared ref
        SequencedSet<Reference> sharedRefs = new LinkedHashSet<>();
        sharedRefs.add(sharedRef);

        // Definition ref
        Reference defRef = that.defRef();

        CompositionDelta composedDelta = new CompositionDelta(useRefs, defRef, sharedRefs);
        composedDelta.deltas().add(this);
        composedDelta.deltas().add(that);

        return composedDelta;
    }

    private CompositionDelta combine(CompositionDelta that) {
        // Checking if the given Delta can be composed with this Delta
        if (this.defRef.isField()) {
            throw new IllegalStateException("This Delta cannot be composed with an another Delta!");
        }

        Reference sharedRef = null;

        for (Reference ref : that.useRefs()) {
            if (ref.equals(this.defRef())) {
                sharedRef = ref;
                break;
            }
        }

        if (sharedRef == null) {
            throw new IllegalArgumentException("The given Delta cannot be composed with this Delta!");
        }

        // Use refs
        SequencedSet<Reference> useRefs = new LinkedHashSet<>();

        for (Reference useRef : this.useRefs()) {
            useRefs.addLast(useRef);
        }

        for (Reference useRef : that.useRefs()) {
            if (useRef.equals(sharedRef)) {
                continue;
            }

            useRefs.addLast(useRef);
        }

        // Shared ref
        SequencedSet<Reference> sharedRefs = new LinkedHashSet<>();

        sharedRefs.addFirst(sharedRef);

        for (Reference useRef : that.sharedRefs()) {
            sharedRefs.addLast(useRef);
        }

        // Definition ref
        Reference defRef = that.defRef();

        CompositionDelta composedDelta = new CompositionDelta(useRefs, defRef, sharedRefs);
        composedDelta.deltas().add(this);
        composedDelta.deltas().addAll(that.deltas());

        return composedDelta;
    }

    @Override
    public boolean isOpen() {
        for (Reference ref : boundaries()) {
            if (!ref.isField()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Set<Reference> boundaries() {
        return Set.of(useRef0, useRef1, defRef);
    }

    @Override
    public SequencedSet<Reference> useRefs() {
        SequencedSet<Reference> useRefs = new LinkedHashSet<>();

        useRefs.add(useRef0);
        useRefs.add(useRef1);
        return useRefs;
    }

    @Override
    public Reference defRef() {
        return defRef;
    }

    @Override
    public Set<Class> classes() {
        Set<Class> classes = new HashSet<>();

        for (Reference boundaryRef : boundaries()) {
            classes.add(boundaryRef.getSource());
            classes.add(boundaryRef.getTarget());
        }
        return classes;
    }
}
