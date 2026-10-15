package org.dflib.jjava.jupyter.kernel;

import org.dflib.jjava.jupyter.kernel.display.protocol.DisplayDelivery;
import org.dflib.jjava.jupyter.kernel.display.protocol.DisplayDeliveryBootstrap;
import org.dflib.jjava.jupyter.kernel.display.protocol.DisplayDeliveryException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ExecutorNotebookStaticsFailureTest {

    private DisplayDelivery previousDelivery;
    private AtomicInteger attempts;

    @BeforeEach
    void setUp() {
        attempts = new AtomicInteger();
        previousDelivery = null;
    }

    @AfterEach
    void tearDown() {
        if (previousDelivery != null) {
            DisplayDeliveryBootstrap.install(previousDelivery);
        }
    }

    @Test
    public void unavailableMapsToRemoteChannelMessage() {
        failingWith(DisplayDeliveryException.Kind.UNAVAILABLE, "Remote display channel is not available");

        RuntimeException e = assertThrows(RuntimeException.class, () -> ExecutorNotebookStatics.display("text"));

        assertEquals("Remote display channel is not available", e.getMessage());
        assertEquals(1, attempts.get());
    }

    @Test
    public void rejectedKeepsDisplayCallbackPrefix() {
        failingWith(DisplayDeliveryException.Kind.REJECTED, "Display callback failed: boom");

        RuntimeException e = assertThrows(RuntimeException.class, () -> ExecutorNotebookStatics.display("text"));

        assertTrue(e.getMessage().startsWith("Display callback failed: "), e.getMessage());
        assertEquals(1, attempts.get());
    }

    @Test
    public void uncertainMapsToFailedToSendPrefixAndIsNotRetried() {
        failingWith(DisplayDeliveryException.Kind.UNCERTAIN, "outcome unknown");

        RuntimeException e = assertThrows(RuntimeException.class, () -> ExecutorNotebookStatics.updateDisplay("id", "text"));

        assertTrue(e.getMessage().startsWith("Failed to send display data: "), e.getMessage());
        assertEquals(1, attempts.get());
    }

    private void failingWith(DisplayDeliveryException.Kind kind, String message) {
        previousDelivery = DisplayDeliveryBootstrap.install(request -> {
            attempts.incrementAndGet();
            throw new DisplayDeliveryException(kind, message, null);
        });
    }
}
