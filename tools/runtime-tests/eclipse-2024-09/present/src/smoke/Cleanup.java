package smoke;

import java.util.ArrayDeque;

/** R4_33 probe-owned cleanup, always attempted in reverse allocation order. */
final class Cleanup implements AutoCloseable {
    private final ArrayDeque<Runnable> actions = new ArrayDeque<>();
    void add(Runnable action) { actions.push(action); }
    @Override public void close() {
        Throwable failure = null;
        while (!actions.isEmpty()) {
            try { actions.pop().run(); }
            catch (Throwable e) { if (failure == null) failure = e; else failure.addSuppressed(e); }
        }
        if (failure != null) {
            System.out.println("R1 CONSUMER CLEANUP FAILED");
            throw new AssertionError("Probe resource cleanup failed", failure);
        }
        System.out.println("R1 CONSUMER CLEANUP COMPLETE");
    }
}
