package dev.shoheiyamagiwa.enumerator.model.algebra;

import java.util.LinkedHashSet;
import java.util.SequencedSet;

public class Delta {
    private final SequencedSet<Reference> useRefs;
    private final Reference defRef;
    private final SequencedSet<Reference> sharedRefs;

    public Delta(SequencedSet<Reference> useRefs, Reference defRef) {
        this(useRefs, defRef, new LinkedHashSet<>());
    }

    public Delta(SequencedSet<Reference> useRefs, Reference defRef, SequencedSet<Reference> sharedRefs) {
        this.useRefs = useRefs;
        this.defRef = defRef;
        this.sharedRefs = sharedRefs;
    }

    public Delta combine(final Delta that) {
        if (this.getDefRef().isField()) {
            throw new IllegalStateException("This Delta cannot be composed with an another Delta!");
        }

        Reference sharedRef = null;

        for (Reference ref : that.getUseRefs()) {
            if (ref.equals(this.getDefRef())) {
                sharedRef = ref;
                break;
            }
        }

        if (sharedRef == null) {
            throw new IllegalArgumentException("The given Delta cannot be composed with this Delta!");
        }

        LinkedHashSet<Reference> newUseRefs = new LinkedHashSet<>();
        for (Reference useRef : this.getUseRefs()) {
            newUseRefs.addLast(useRef);
        }
        for (Reference useRef : that.getUseRefs()) {
            if (useRef.equals(sharedRef)) {
                continue;
            }
            newUseRefs.addLast(useRef);
        }

        LinkedHashSet<Reference> newSharedRefs = new LinkedHashSet<>();
        for (Reference useRef : this.sharedRefs) {
            newSharedRefs.addLast(useRef);
        }

        newSharedRefs.addLast(sharedRef);

        for (Reference useRef : that.sharedRefs) {
            newSharedRefs.addLast(useRef);
        }

        Reference newDefRef = that.getDefRef();

        return new Delta(newUseRefs, newDefRef, newSharedRefs);
    }

    public boolean isOpen() {
        if (!getDefRef().isField()) {
            return true;
        }

        for (Reference ref : getUseRefs()) {
            if (!ref.isField()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();

        s.append("UsedRef = [");
        for (Reference useRef : getUseRefs()) {
            s.append(useRef).append(", ");
        }
        s.append("]").append(System.lineSeparator());

        s.append("SharedRef = [");
        for (Reference sharedRef : sharedRefs) {
            s.append(sharedRef).append(", ");
        }
        s.append("]").append(System.lineSeparator());

        s.append("DefinedRef = ");
        s.append(getDefRef());
        s.append(System.lineSeparator());

        return s.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Delta that)) {
            return false;
        }

        boolean sameUseRefs = this.getUseRefs().equals(that.getUseRefs());
        boolean sameSharedRefs = this.sharedRefs.equals(that.getSharedRefs());
        boolean sameDefRef = this.getDefRef().equals(that.getDefRef());

        return sameUseRefs && sameSharedRefs && sameDefRef;
    }

    public SequencedSet<Reference> getUseRefs() {
        return useRefs;
    }

    public Reference getDefRef() {
        return defRef;
    }

    public SequencedSet<Reference> getSharedRefs() {
        return sharedRefs;
    }
}