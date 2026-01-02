package org.dflib.jjava.jupyter.telemetry;

import java.util.concurrent.atomic.AtomicLong;

public abstract class WallTimeCollector implements TelemetryCollector<Long> {

    private final AtomicLong totalWallTime;

    public WallTimeCollector() {
        this.totalWallTime = new AtomicLong();
    }

    @Override
    public Long measurementStart() {
        return System.nanoTime();
    }

    @Override
    public void measurementEnd(Long startState) {
        long delta = System.nanoTime() - startState;
        totalWallTime.addAndGet(delta);
    }

    @Override
    public void stop() {
        onStop(totalWallTime.get());
    }

    protected abstract void onStop(long wallTimeNanos);
}
