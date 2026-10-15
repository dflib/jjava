package org.dflib.jjava.kernel.execution;

public class ExecutorTerminationException extends RuntimeException {
    public ExecutorTerminationException(String message, Throwable cause) {
        super(message, cause);
    }
}
