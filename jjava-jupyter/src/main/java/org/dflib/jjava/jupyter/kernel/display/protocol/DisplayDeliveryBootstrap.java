package org.dflib.jjava.jupyter.kernel.display.protocol;

import org.dflib.jjava.jupyter.kernel.display.protocol.uds.UnixDomainDisplayDelivery;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Chooses the executor-side delivery from the {@code jjava.display.socket} system property. Without the property,
 * every delivery fails as UNAVAILABLE.
 */
public final class DisplayDeliveryBootstrap {

    public static final String SOCKET_PROPERTY = "jjava.display.socket";

    private static DisplayDelivery current;

    private DisplayDeliveryBootstrap() {
    }

    public static synchronized DisplayDelivery current() {
        if (current == null) {
            current = fromSocketPath(System.getProperty(SOCKET_PROPERTY));
        }
        return current;
    }

    /**
     * Replaces the delivery and returns the previous one.
     */
    public static synchronized DisplayDelivery install(DisplayDelivery delivery) {
        DisplayDelivery previous = current();
        current = Objects.requireNonNull(delivery);
        return previous;
    }

    static DisplayDelivery fromSocketPath(String socketPath) {
        if (socketPath == null) {
            return request -> {
                throw new DisplayDeliveryException(
                        DisplayDeliveryException.Kind.UNAVAILABLE, "Remote display channel is not available", null);
            };
        }
        return new UnixDomainDisplayDelivery(Path.of(socketPath));
    }
}
