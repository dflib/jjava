package org.dflib.jjava.jupyter.telemetry;

/**
 * Aggregates some telemetry number for a sequence of "measurements". Instead of a single method wrapping a measure code
 * invocation lambda, each measurement is done via two separate methods: {@link #measurementStart()} and
 * {@link #measurementEnd(Object)}. This is to make the collector minimally invasive and not adding to the call stack
 * trace.
 */
public interface TelemetryCollector<I> {

    I measurementStart();

    void measurementEnd(I startState);

    void stop();

    TelemetryCollector<Object> DO_NOTHING = new TelemetryCollector<>() {
        @Override
        public void measurementEnd(Object startState) {
        }

        @Override
        public Object measurementStart() {
            return null;
        }

        @Override
        public void stop() {
        }
    };
}
