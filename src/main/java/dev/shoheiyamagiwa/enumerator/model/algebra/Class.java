package dev.shoheiyamagiwa.enumerator.model.algebra;

public class Class {
    private final String name;

    public Class(final String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Class name cannot be null or blank!");
        }

        this.name = name;
    }

    public Class updateName(final String name) {
        return new Class(name);
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Class that)) {
            return false;
        }

        return this.name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}