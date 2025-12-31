package org.dflib.jjava.jupyter.instrumentation;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * A simple timers that captures wall clock elapsed time.
 */
public abstract class WallTimer implements EvalTimer {

    private final AtomicLong totalWallTime;

    public WallTimer() {
        this.totalWallTime = new AtomicLong();
    }

    @Override
    public void start() {
        totalWallTime.set(0);
    }

    @Override
    public <T> T runAndMeasureStep(Supplier<T> timedAction) {
        long t0 = System.nanoTime();
        try {
            return timedAction.get();
        } finally {
            totalWallTime.addAndGet(System.nanoTime() - t0);
        }
    }

    @Override
    public void stop() {
        onStop(totalWallTime.get());
    }

    protected abstract void onStop(long wallTimeNanos);
}
