package dev.shoheiyamagiwa.enumerator.model.algebra;

public class Reference {
    private Class source;
    private Class target;

    private ReferenceType type;

    public Reference(Class source, Class target, ReferenceType type) {
        this.source = source;
        this.target = target;
        this.type = type;
    }

    public void inverse() {
        Class tempSource = source;

        source = target;
        target = tempSource;
    }

    public boolean isField() {
        return type == ReferenceType.FIELD;
    }

    public void updateMethod(final ReferenceType type) {
        if (type == null) {
            throw new IllegalArgumentException();
        }

        this.type = type;
    }

    public Class getSource() {
        return source;
    }

    public Class getTarget() {
        return target;
    }

    public ReferenceType getType() {
        return type;
    }

    @Override
    public String toString() {
        String str = switch (type) {
            case FIELD -> "-field->";
            case LOCAL -> "-local->";
            case METHOD_ARGUMENT -> "-method argument->";
            case TEMPORARY -> "-temp->";
        };
        return source + " " + str + " " + target;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Reference that)) {
            return false;
        }

        return this.source.equals(that.getSource()) && this.target.equals(that.getTarget())
                && this.type.equals(that.getType());
    }
}