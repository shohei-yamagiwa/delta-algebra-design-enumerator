package dev.shoheiyamagiwa.enumerator.benchmark;

import dev.shoheiyamagiwa.enumerator.core.DesignEvaluator;
import dev.shoheiyamagiwa.enumerator.core.TriangulationUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DesignSampler {
    public static List<int[]> remyTriangles(int deltaCount, SplitMix64 random) {
        int nodeCount = 2 * deltaCount + 1;
        int[] leftChild = new int[nodeCount];
        int[] rightChild = new int[nodeCount];
        int[] parent = new int[nodeCount];
        int[] childSide = new int[nodeCount];
        boolean[] internal = new boolean[nodeCount];

        Arrays.fill(leftChild, -1);
        Arrays.fill(rightChild, -1);
        Arrays.fill(parent, -1);
        Arrays.fill(childSide, -1);

        int[] activeNodes = new int[nodeCount];
        int activeNodeCount = 0;
        int root = 0;

        activeNodes[activeNodeCount++] = 0; // start: single leaf (node 0)

        for (int step = 1; step <= deltaCount; step++) {
            int internalNode = 2 * step - 1;
            int newLeaf = 2 * step;
            int pickedNode = activeNodes[random.nextInt(2 * step - 1)]; // uniform among current 2*step-1 nodes
            int chosenSide = random.nextBit() ? 1 : 0;
            int parentNode = parent[pickedNode];
            int parentSide = childSide[pickedNode];

            parent[internalNode] = parentNode;
            childSide[internalNode] = parentSide;
            internal[internalNode] = true;

            if (parentNode == -1) {
                root = internalNode;
            } else {
                if (parentSide == 0) leftChild[parentNode] = internalNode;
                else rightChild[parentNode] = internalNode;
            }

            if (chosenSide == 0) {
                leftChild[internalNode] = pickedNode;
                parent[pickedNode] = internalNode;
                childSide[pickedNode] = 0;
                rightChild[internalNode] = newLeaf;
                parent[newLeaf] = internalNode;
                childSide[newLeaf] = 1;
            } else {
                leftChild[internalNode] = newLeaf;
                parent[newLeaf] = internalNode;
                childSide[newLeaf] = 0;
                rightChild[internalNode] = pickedNode;
                parent[pickedNode] = internalNode;
                childSide[pickedNode] = 1;
            }

            activeNodes[activeNodeCount++] = internalNode;
            activeNodes[activeNodeCount++] = newLeaf;
        }

        int[] leafCounts = new int[nodeCount];

        countLeaves(root, internal, leftChild, rightChild, leafCounts);

        List<int[]> triangles = new ArrayList<>(deltaCount);
        buildTriangles(root, 0, deltaCount + 1, internal, leftChild, rightChild, leafCounts, triangles);

        return triangles;
    }

    private static int countLeaves(int node, boolean[] internal, int[] leftChild, int[] rightChild, int[] leafCounts) {
        if (node == -1) {
            return 0;
        }

        if (!internal[node]) {
            leafCounts[node] = 1;
            return 1;
        }

        int leafCount = countLeaves(leftChild[node], internal, leftChild, rightChild, leafCounts)
                + countLeaves(rightChild[node], internal, leftChild, rightChild, leafCounts);
        leafCounts[node] = leafCount;

        return leafCount;
    }

    private static void buildTriangles(int node, int arcStart, int arcEnd, boolean[] internal, int[] leftChild, int[] rightChild, int[] leafCounts, List<int[]> triangles) {
        if (node == -1 || !internal[node]) {
            return;
        }

        int arcMid = arcStart + leafCounts[leftChild[node]];

        triangles.add(new int[]{arcStart, arcMid, arcEnd});

        buildTriangles(leftChild[node], arcStart, arcMid, internal, leftChild, rightChild, leafCounts, triangles);
        buildTriangles(rightChild[node], arcMid, arcEnd, internal, leftChild, rightChild, leafCounts, triangles);
    }

    public static SplitMix64 rngForSample(long globalSeed, long sampleIndex) {
        return new SplitMix64(SplitMix64.mix64(globalSeed ^ SplitMix64.mix64(sampleIndex)));
    }

    static int violationsFromParts(int setDirectionBits, int ears) {
        return DesignEvaluator.violations(setDirectionBits, ears);
    }

    static int sampleViolations(int deltaCount, long globalSeed, long sampleIndex) {
        SplitMix64 rng = rngForSample(globalSeed, sampleIndex);
        List<int[]> triangles = remyTriangles(deltaCount, rng);
        int[] diagonalCountAndEarCount = TriangulationUtils.diagonalCountAndEars(triangles, deltaCount + 2, null); // {#diagonals, ears}
        int diagonalCount = diagonalCountAndEarCount[0];
        int ears = diagonalCountAndEarCount[1]; // diagonalCount == deltaCount-1
        int setDirectionBits = 0;

        for (int diagonalIndex = 0; diagonalIndex < diagonalCount; diagonalIndex++) {
            if (rng.nextBit()) setDirectionBits++; // directions
        }

        return violationsFromParts(setDirectionBits, ears);
    }

    public static long[] exact(int deltaCount) {
        int vertexCount = deltaCount + 2;
        int[] vertices = new int[vertexCount];

        for (int vertexIndex = 0; vertexIndex < vertexCount; vertexIndex++) {
            vertices[vertexIndex] = vertexIndex;
        }

        List<List<int[]>> allTriangulations = TriangulationUtils.triFill(vertices);
        long directionCombinationCount = 1L << (deltaCount - 1);
        int minViolations = Integer.MAX_VALUE;
        long satisfyingCount = 0;

        for (List<int[]> triangles : allTriangulations) {
            int ears = TriangulationUtils.diagonalCountAndEars(triangles, vertexCount, null)[1];

            for (long directionBits = 0; directionBits < directionCombinationCount; directionBits++) {
                int violations = DesignEvaluator.violations(Long.bitCount(directionBits), ears);

                if (violations < minViolations) {
                    minViolations = violations;
                }
                if (violations == 0) {
                    satisfyingCount++;
                }
            }
        }
        return new long[]{minViolations, satisfyingCount, (long) allTriangulations.size() * directionCombinationCount};
    }
}
