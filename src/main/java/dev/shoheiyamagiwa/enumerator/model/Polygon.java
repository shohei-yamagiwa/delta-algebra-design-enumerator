package dev.shoheiyamagiwa.enumerator.model;

public class Polygon {
    private final String[] classNames;

    private final RefEdge[] boundary;

    public Polygon(String[] classNames, RefEdge[] boundary) {
        this.classNames = classNames;
        this.boundary = boundary;
    }

    public int vertices() {
        return classNames.length;
    }

    public int deltas() {
        return vertices() - 2;
    }

    public String[] getClassNames() {
        return classNames;
    }

    public RefEdge[] getBoundary() {
        return boundary;
    }
}
