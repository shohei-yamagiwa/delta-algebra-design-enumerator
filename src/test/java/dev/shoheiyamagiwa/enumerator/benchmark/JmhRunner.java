package dev.shoheiyamagiwa.enumerator.benchmark;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Entry point for the JMH benchmarks. Running it without arguments runs every benchmark in this
 * package; passing regular expressions restricts the run to the matching ones, e.g.
 * {@code JmhRunner ReferenceSetAlgebra}.
 */
public class JmhRunner {
    public static void main(String[] args) throws RunnerException {
        OptionsBuilder options = new OptionsBuilder();

        if (args.length == 0) {
            options.include(JmhRunner.class.getPackageName() + ".*");
        } else {
            for (String arg : args) {
                options.include(arg);
            }
        }

        new Runner(options.build()).run();
    }
}
