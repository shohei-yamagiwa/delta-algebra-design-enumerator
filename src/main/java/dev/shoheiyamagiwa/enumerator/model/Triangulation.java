package dev.shoheiyamagiwa.enumerator.model;

public class Triangulation {
    private final int[][] triangles;
    private final int[][] diagonals;

    private final int ears;

    public Triangulation(int[][] triangles, int[][] diagonals, int ears) {
        this.triangles = triangles;
        this.diagonals = diagonals;
        this.ears = ears;
    }

    public int[][] getTriangles() {
        return triangles;
    }

    public int[][] getDiagonals() {
        return diagonals;
    }

    public int getEars() {
        return ears;
    }
}
