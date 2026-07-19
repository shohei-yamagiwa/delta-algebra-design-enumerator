package dev.shoheiyamagiwa.enumerator.model;

public class RefEdge {
    private final String name;

    private final int from;
    private final int to;

    private final boolean field;

    public RefEdge(String name, int from, int to, boolean field) {
        this.name = name;
        this.from = from;
        this.to = to;
        this.field = field;
    }

    public String getName() {
        return name;
    }

    public int getFrom() {
        return from;
    }

    public int getTo() {
        return to;
    }

    public boolean isField() {
        return field;
    }
}
