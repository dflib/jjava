package org.dflib.jjava.execution;

import java.util.concurrent.TimeUnit;

public class EvaluationTimeoutException extends RuntimeException {
    private final long duration;
    private final TimeUnit unit;
    private final String source;

    public EvaluationTimeoutException(long duration, TimeUnit unit, String source) {
        this.duration = duration;
        this.unit = unit;
        this.source = source;
    }

    public long getDuration() {
        return duration;
    }

    public TimeUnit getUnit() {
        return unit;
    }

    public String getSource() {
        return source;
    }

    @Override
    public String getMessage() {
        return String.format("Evaluator timed out after %d %s while executing: '%s'",
                this.duration,
                this.unit.name().toLowerCase(),
                this.source);
    }
}
