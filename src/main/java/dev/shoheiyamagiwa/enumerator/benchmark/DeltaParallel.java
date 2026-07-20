package dev.shoheiyamagiwa.enumerator.benchmark;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class DeltaParallel {
    public static Acc serial(int n, long seed, long S) {
        Sampler w = new Sampler(n);

        w.gk = n + 2;

        Acc a = new Acc();

        for (long i = 0; i < S; i++) {
            reduce(a, w.eval(n, seed, i));
        }

        return a;
    }

    public static Acc parallel(int n, long seed, long S, int threads) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Future<Acc>> fs = new ArrayList<>();
        long chunk = (S + threads - 1) / threads;

        for (int t = 0; t < threads; t++) {
            final long lo = t * chunk, hi = Math.min(S, lo + chunk);

            fs.add(pool.submit(() -> {
                Sampler w = new Sampler(n);

                w.gk = n + 2;

                Acc a = new Acc();

                for (long i = lo; i < hi; i++) {
                    reduce(a, w.eval(n, seed, i));
                }

                return a;
            }));
        }

        Acc all = new Acc();

        for (Future<Acc> f : fs) {
            merge(all, f.get());
        }

        pool.shutdown();

        return all;
    }

    private static void reduce(Acc a, int v) {
        if (v < a.best) {
            a.best = v;
        }

        if (v == 0) {
            a.sat++;
        }

        a.hist[Math.clamp(v, 0, Acc.HCAP - 1)]++;
    }

    private static void merge(Acc a, Acc b) {
        a.best = Math.min(a.best, b.best);
        a.sat += b.sat;

        for (int j = 0; j < Acc.HCAP; j++) {
            a.hist[j] += b.hist[j];
        }
    }
}
