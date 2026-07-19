package dev.shoheiyamagiwa.enumerator.benchmark;

import dev.shoheiyamagiwa.enumerator.model.Polygon;
import dev.shoheiyamagiwa.enumerator.model.Triangulation;

public interface DesignEvaluator {
    int violations(Polygon polygon, Triangulation triangulation, long directionBits);
}
