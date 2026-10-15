package org.dflib.jjava.jupyter.kernel.display.protocol;

public class DisplayDeliveryException extends IllegalStateException {

    public enum Kind {
        UNAVAILABLE,
        REJECTED,
        UNCERTAIN
    }

    private final Kind kind;

    public DisplayDeliveryException(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }
}
