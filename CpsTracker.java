package dev.zxht.client.fx;

import java.util.ArrayDeque;

/** Counts left clicks in the last second (display only). */
public final class CpsTracker {
    private static final ArrayDeque<Long> CLICKS = new ArrayDeque<>();
    private static boolean wasDown = false;

    private CpsTracker() {}

    /** Call once per tick with the current state of the attack key. */
    public static void poll(boolean down) {
        if (down && !wasDown) CLICKS.addLast(System.currentTimeMillis());
        wasDown = down;
    }

    public static int cps() {
        long cutoff = System.currentTimeMillis() - 1000L;
        while (!CLICKS.isEmpty() && CLICKS.peekFirst() < cutoff) CLICKS.removeFirst();
        return CLICKS.size();
    }
}
