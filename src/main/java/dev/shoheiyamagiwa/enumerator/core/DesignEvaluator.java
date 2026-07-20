package dev.shoheiyamagiwa.enumerator.core;

import dev.shoheiyamagiwa.enumerator.model.Polygon;
import dev.shoheiyamagiwa.enumerator.model.Triangulation;

/**
 * Strategy for counting how many rule violations a given design (a {@link Triangulation} of the
 * polygon, together with an assignment of a direction bit to each of its diagonals) has.
 * Implementations are free to inspect the {@link Polygon} (e.g. its reference graph) to compute a
 * custom violation count; {@link #STANDARD} provides the default, structure-only rule used
 * throughout this project.
 */
public interface DesignEvaluator {
    /**
     * The standard evaluator, based purely on the triangulation's structure: one violation for
     * every diagonal whose direction bit is set, plus {@code ears - 2} (a triangulation of a
     * polygon with at least 3 vertices always has at least two ears, so this term is non-negative).
     */
    DesignEvaluator STANDARD = (polygon, triangulation, directionBits) -> {
        int diagonalCount = triangulation.getDiagonals().length;
        int setDirectionBits = Long.bitCount(maskToDiagonalCount(directionBits, diagonalCount));

        return violations(setDirectionBits, triangulation.getEars());
    };

    /**
     * Computes the number of violations of the given design.
     *
     * @param polygon        the polygon whose design is being evaluated
     * @param triangulation  the triangulation defining the design's diagonals and ear count
     * @param directionBits  bit {@code i} encodes the chosen direction of the {@code i}-th diagonal
     *                        (in {@code triangulation.getDiagonals()} order); only the lowest
     *                        {@code triangulation.getDiagonals().length} bits are meaningful
     * @return the number of violations of this design; {@code 0} means the design is fully valid
     */
    int violations(Polygon polygon, Triangulation triangulation, long directionBits);

    /**
     * Computes the standard violation count from already-extracted parts, without needing a
     * {@link Polygon}/{@link Triangulation} instance. This is the single source of truth for the
     * standard violation formula, reused by {@link #STANDARD} as well as by callers (such as
     * {@code NaiveViolationSampler}) that only have the diagonal-direction popcount and ear count at hand.
     *
     * @param setDirectionBits the number of diagonals whose direction bit is set
     * @param ears             the ear count of the triangulation
     * @return {@code setDirectionBits + (ears - 2)}
     */
    static int violations(int setDirectionBits, int ears) {
        return setDirectionBits + (ears - 2);
    }

    /**
     * Masks {@code directionBits} down to its lowest {@code diagonalCount} bits.
     *
     * @param directionBits the raw direction bits, possibly containing garbage above bit {@code diagonalCount - 1}
     * @param diagonalCount the number of meaningful low-order bits (0..64)
     * @return {@code directionBits} with only its lowest {@code diagonalCount} bits kept
     */
    private static long maskToDiagonalCount(long directionBits, int diagonalCount) {
        long mask = (diagonalCount == 64) ? -1L : ((1L << diagonalCount) - 1);

        return directionBits & mask;
    }
}
