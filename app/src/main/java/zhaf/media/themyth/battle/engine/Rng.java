package zhaf.media.themyth.battle.engine;

public final class Rng {

    private long state;

    public Rng(long seed) {
        this.state = seed == 0L ? 0x9E3779B97F4A7C15L : seed;
    }

    public long next() {
        state += 0x9E3779B97F4A7C15L;
        long z = state;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    public int nextInt(int bound) {
        if (bound <= 0) {
            return 0;
        }
        return (int) Math.floorMod(next(), (long) bound);
    }

    public int nextBp() {
        return nextInt(10000);
    }

    public boolean roll(int chanceBp) {
        return nextBp() < chanceBp;
    }
}
