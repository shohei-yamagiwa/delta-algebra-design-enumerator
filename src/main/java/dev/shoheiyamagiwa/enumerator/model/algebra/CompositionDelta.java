package dev.shoheiyamagiwa.enumerator.model.algebra;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.SequencedSet;
import java.util.Set;

public class CompositionDelta implements Delta {
    private final SequencedSet<Reference> useRefs;
    private final Reference defRef;
    private final SequencedSet<Reference> sharedRefs;

    private final SequencedSet<PrimitiveDelta> deltas = new LinkedHashSet<>();

    public CompositionDelta(SequencedSet<Reference> useRefs, Reference defRef) {
        this(useRefs, defRef, new LinkedHashSet<>());
    }

    public CompositionDelta(SequencedSet<Reference> useRefs, Reference defRef, SequencedSet<Reference> sharedRefs) {
        this.useRefs = useRefs;
        this.defRef = defRef;
        this.sharedRefs = sharedRefs;
    }

    @Override
    public CompositionDelta combine(final Delta that) {
        if (this.defRef().isField()) {
            throw new IllegalStateException("This Delta cannot be composed with an another Delta!");
        }

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

        // Use ref
        LinkedHashSet<Reference> newUseRefs = new LinkedHashSet<>();

        for (Reference useRef : this.useRefs()) {
            newUseRefs.addLast(useRef);
        }

        for (Reference useRef : that.useRefs()) {
            if (useRef.equals(sharedRef)) {
                continue;
            }

            newUseRefs.addLast(useRef);
        }

        // Shared ref
        LinkedHashSet<Reference> newSharedRefs = new LinkedHashSet<>();

        for (Reference useRef : this.sharedRefs) {
            newSharedRefs.addLast(useRef);
        }

        newSharedRefs.addLast(sharedRef);

        // Definition ref
        Reference newDefRef = that.defRef();

        CompositionDelta composedDelta = new CompositionDelta(newUseRefs, newDefRef, newSharedRefs);
        composedDelta.deltas().addAll(this.deltas());
        composedDelta.deltas().add(that);

        return composedDelta;
    }

    private CompositionDelta combine(CompositionDelta that) {
        // Checking if the given Delta can be composed with this Delta
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

        // Use ref
        LinkedHashSet<Reference> newUseRefs = new LinkedHashSet<>();

        for (Reference useRef : this.useRefs()) {
            newUseRefs.addLast(useRef);
        }

        for (Reference useRef : that.useRefs()) {
            if (useRef.equals(sharedRef)) {
                continue;
            }

            newUseRefs.addLast(useRef);
        }

        // Shared refs
        LinkedHashSet<Reference> newSharedRefs = new LinkedHashSet<>();
        for (Reference useRef : this.sharedRefs) {
            newSharedRefs.addLast(useRef);
        }

        newSharedRefs.addLast(sharedRef);

        for (Reference useRef : that.sharedRefs()) {
            newSharedRefs.addLast(useRef);
        }

        // Definition ref
        Reference newDefRef = that.defRef();

        CompositionDelta composedDelta = new CompositionDelta(newUseRefs, newDefRef, newSharedRefs);
        composedDelta.deltas().addAll(this.deltas());
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
        Set<Reference> boundaries = new HashSet<>();

        boundaries.addAll(useRefs);
        boundaries.add(defRef);

        return boundaries;
    }

    public Set<Class> getClasses() {
        Set<Class> classes = new HashSet<>();

        for (Reference useRef : useRefs) {
            classes.add(useRef.getSource());
            classes.add(useRef.getTarget());
        }

        classes.add(defRef.getSource());
        classes.add(defRef.getTarget());

        return classes;
    }

    public Set<Reference> getRefsBySourceClass(Class source) {
        Set<Reference> refs = new HashSet<>();

        for (Reference useRef : useRefs) {
            if (useRef.getSource().equals(source)) {
                refs.add(useRef);
            }
        }

        for (Reference sharedRef : sharedRefs) {
            if (sharedRef.getSource().equals(source)) {
                refs.add(sharedRef);
            }
        }

        if (defRef.getSource().equals(source)) {
            refs.add(defRef);
        }

        return refs;
    }

    public Set<Reference> getRefsByTargetClass(Class target) {
        Set<Reference> refs = new HashSet<>();

        for (Reference useRef : useRefs) {
            if (useRef.getTarget().equals(target)) {
                refs.add(useRef);
            }
        }

        for (Reference sharedRef : sharedRefs) {
            if (sharedRef.getTarget().equals(target)) {
                refs.add(sharedRef);
            }
        }

        if (defRef.getTarget().equals(target)) {
            refs.add(defRef);
        }

        return refs;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();

        s.append("UsedRef = [");
        for (Reference useRef : useRefs) {
            s.append(useRef).append(", ");
        }
        s.append("]").append(System.lineSeparator());

        s.append("SharedRef = [");
        for (Reference sharedRef : sharedRefs) {
            s.append(sharedRef).append(", ");
        }
        s.append("]").append(System.lineSeparator());

        s.append("DefinedRef = ");
        s.append(defRef);
        s.append(System.lineSeparator());

        return s.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CompositionDelta that)) {
            return false;
        }

        boolean sameUseRefs = this.useRefs.equals(that.useRefs);
        boolean sameSharedRefs = this.sharedRefs.equals(that.sharedRefs);
        boolean sameDefRef = this.defRef.equals(that.defRef);

        return sameUseRefs && sameSharedRefs && sameDefRef;
    }

    public SequencedSet<PrimitiveDelta> deltas() {
        return deltas;
    }

    @Override
    public SequencedSet<Reference> useRefs() {
        return useRefs;
    }

    public SequencedSet<Reference> sharedRefs() {
        return sharedRefs;
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