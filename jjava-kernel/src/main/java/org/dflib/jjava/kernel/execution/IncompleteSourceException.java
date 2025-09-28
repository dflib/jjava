package org.dflib.jjava.kernel.execution;

public class IncompleteSourceException extends RuntimeException {
    private final String source;

    public IncompleteSourceException(String source) {
        super(source);
        this.source = source;
    }

    public String getSource() {
        return source;
    }
}
