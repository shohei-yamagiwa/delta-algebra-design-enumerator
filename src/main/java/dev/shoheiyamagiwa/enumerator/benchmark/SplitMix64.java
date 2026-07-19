package dev.shoheiyamagiwa.enumerator.benchmark;

public class SplitMix64 {
    private long seed;

    public SplitMix64(long seed) {
        this.seed = seed;
    }

    public long nextLong() {
        seed += 0x9E3779B97F4A7C15L;
        return mix64(seed);
    }

    public int nextInt(int bound) {
        return Math.floorMod(nextLong(), bound);
    }

    public boolean nextBit() {
        return (nextLong() & 1L) != 0;
    }

    public static long mix64(long z) {
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }
}
