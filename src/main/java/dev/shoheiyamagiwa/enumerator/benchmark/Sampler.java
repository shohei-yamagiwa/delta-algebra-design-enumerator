package dev.shoheiyamagiwa.enumerator.benchmark;

import dev.shoheiyamagiwa.enumerator.core.DesignEvaluator;
import dev.shoheiyamagiwa.enumerator.core.TriangulationUtils;

/**
 * Zero-allocation, reusable-buffer counterpart of {@link DesignSampler}'s Rémy-tree sampling, used
 * by {@link DeltaParallel} for high-throughput (parallel) benchmarking. Its Rémy-tree construction
 * and ear-counting recursion intentionally duplicate the algorithms in {@link DesignSampler} and
 * {@link TriangulationUtils}: unlike those, this class reuses pre-allocated instance arrays across
 * calls to {@link #eval} (instead of allocating a fresh {@code List}/arrays per sample), which is
 * the whole point of this class on the hot sampling path. The RNG mixing step and
 * the boundary-edge test have no such allocation concern, so they simply delegate
 * to {@link SplitMix64#mix64} and {@link TriangulationUtils#isBoundary} respectively.
 */
public class Sampler {
    private static final long GAMMA_INCREMENT = 0x9E3779B97F4A7C15L;

    private final int[] leftChild;
    private final int[] rightChild;
    private final int[] parent;
    private final int[] childSide;
    private final int[] activeNodes;
    private final int[] leafCounts;
    private final boolean[] internal;
    private int earCount;

    public Sampler(int maxDeltaCount) {
        int maxNodeCount = 2 * maxDeltaCount + 1;

        leftChild = new int[maxNodeCount];
        rightChild = new int[maxNodeCount];
        parent = new int[maxNodeCount];
        childSide = new int[maxNodeCount];
        activeNodes = new int[maxNodeCount];
        leafCounts = new int[maxNodeCount];
        internal = new boolean[maxNodeCount];
    }

    /**
     * violations for sample sampleIndex — no heap allocation. RNG draw order matches DeltaSampler.
     */
    public int eval(int deltaCount, long seed, long sampleIndex) {
        long state = SplitMix64.mix64(seed ^ SplitMix64.mix64(sampleIndex)); // counter-based per-sample state
        int nodeCount = 2 * deltaCount + 1;

        for (int nodeIndex = 0; nodeIndex < nodeCount; nodeIndex++) {
            leftChild[nodeIndex] = -1;
            rightChild[nodeIndex] = -1;
            parent[nodeIndex] = -1;
            childSide[nodeIndex] = -1;
            internal[nodeIndex] = false;
        }

        int activeNodeCount = 0, root = 0;

        activeNodes[activeNodeCount++] = 0;

        for (int step = 1; step <= deltaCount; step++) {
            int internalNode = 2 * step - 1, newLeaf = 2 * step;

            state += GAMMA_INCREMENT;

            long pickRandomBits = SplitMix64.mix64(state);
            int pickedIndex = Math.floorMod(pickRandomBits, 2 * step - 1);

            state += GAMMA_INCREMENT;

            long sideRandomBits = SplitMix64.mix64(state);
            int chosenSide = (int) (sideRandomBits & 1L);
            int pickedNode = activeNodes[pickedIndex];
            int parentNode = parent[pickedNode], parentSide = childSide[pickedNode];

            parent[internalNode] = parentNode;
            childSide[internalNode] = parentSide;
            internal[internalNode] = true;

            if (parentNode == -1) {
                root = internalNode;
            } else {
                if (parentSide == 0) {
                    leftChild[parentNode] = internalNode;
                } else {
                    rightChild[parentNode] = internalNode;
                }
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

        leafCount(root);
        earCount = 0;
        countEars(root, 0, deltaCount + 1, deltaCount + 2);

        int ears = earCount, diagonalCount = deltaCount - 1, setDirectionBits = 0;

        for (int diagonalIndex = 0; diagonalIndex < diagonalCount; diagonalIndex++) {
            state += GAMMA_INCREMENT;

            long directionRandomBits = SplitMix64.mix64(state);

            if ((directionRandomBits & 1L) != 0) {
                setDirectionBits++;
            }
        }

        return DesignEvaluator.violations(setDirectionBits, ears);
    }

    int leafCount(int node) {
        if (node == -1) {
            return 0;
        }

        if (!internal[node]) {
            leafCounts[node] = 1;
            return 1;
        }

        int count = leafCount(leftChild[node]) + leafCount(rightChild[node]);

        leafCounts[node] = count;

        return count;
    }

    /**
     * @param node        the subtree whose triangles are counted
     * @param arcStart    the first vertex of the arc this subtree spans
     * @param arcEnd      the last vertex of the arc this subtree spans
     * @param vertexCount the size of the *whole* polygon (deltaCount+2), not of the local arc: the
     *                    boundary test has to recognise the closing edge (0, vertexCount-1), which a
     *                    local arc cannot tell apart from a diagonal
     */
    void countEars(int node, int arcStart, int arcEnd, int vertexCount) {
        if (node == -1 || !internal[node]) {
            return;
        }

        int arcMid = arcStart + leafCounts[leftChild[node]];
        int boundaryEdgeCount = 0;

        if (TriangulationUtils.isBoundary(arcStart, arcMid, vertexCount)) {
            boundaryEdgeCount++;
        }
        if (TriangulationUtils.isBoundary(arcMid, arcEnd, vertexCount)) {
            boundaryEdgeCount++;
        }
        if (TriangulationUtils.isBoundary(arcStart, arcEnd, vertexCount)) {
            boundaryEdgeCount++;
        }

        if (boundaryEdgeCount >= 2) {
            earCount++;
        }

        countEars(leftChild[node], arcStart, arcMid, vertexCount);
        countEars(rightChild[node], arcMid, arcEnd, vertexCount);
    }
}
