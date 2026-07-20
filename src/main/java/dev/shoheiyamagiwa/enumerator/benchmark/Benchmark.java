package dev.shoheiyamagiwa.enumerator.benchmark;

/**
 * Entry point tying together the project's independent verification/benchmark concerns:
 * triangulation enumeration sanity checks, Rémy-sampling benchmarks, zero-allocation parallel
 * sampling benchmarks, and a reference-set-algebra SIMD micro-benchmark. Each concern lives in its
 * own dedicated class; this class only orchestrates their execution.
 */
public class Benchmark {
    static void main() throws Exception {
        TriangulationSanityChecks.run();
//        SamplingBenchmarks.run();
        ParallelBenchmarks.run();
        ReferenceSetAlgebraBenchmarks.run();
    }
}
