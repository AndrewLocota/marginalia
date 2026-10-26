package app.marginalia;

/** A wall-clock based sequence: no repeat until every item has appeared. */
public final class Rotation {
    private Rotation() {}
    public static long elapsedSlots(long now, long anchor, int hours) {
        return Math.max(0L, now-anchor) / (hours * 3_600_000L);
    }
    public static int index(long now, long anchor, int hours, int start, int count) {
        if (count < 1) throw new IllegalArgumentException("Empty rotation");
        return (int)Math.floorMod(start + elapsedSlots(now,anchor,hours), count);
    }
    public static long next(long now, long anchor, int hours) {
        return anchor + (elapsedSlots(now,anchor,hours)+1) * hours * 3_600_000L;
    }
}
