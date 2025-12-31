package org.dflib.jjava.jupyter.instrumentation;

import java.util.function.Supplier;

/**
 * Captures code evaluation performance metrics.
 */
public interface EvalTimer {

    void start();

    /**
     * Called by the kernel for each step in an evaluation of a piece of code. The timer would normally capture the
     * time metrics for a single step and add them to the totals being captured since {@link #start()} was called.
     */
    <T> T runAndMeasureStep(Supplier<T> timedAction);

    void stop();

    EvalTimer DO_NOTHING = new EvalTimer() {
        @Override
        public void start() {
        }

        @Override
        public <T> T runAndMeasureStep(Supplier<T> timedAction) {
            return timedAction.get();
        }

        @Override
        public void stop() {
        }
    };
}
