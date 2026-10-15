package org.dflib.jjava.jupyter.kernel.display.protocol;

import org.dflib.jjava.jupyter.kernel.display.DisplayData;
import org.dflib.jjava.jupyter.kernel.display.protocol.uds.UnixDomainDisplayDelivery;
import org.junit.jupiter.api.Test;

import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.dflib.jjava.jupyter.kernel.display.protocol.DisplayDeliveryException.Kind.UNAVAILABLE;
import static org.dflib.jjava.jupyter.kernel.display.protocol.DisplayDeliveryException.Kind.UNCERTAIN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Production bounds: a single 10 s RESPONSE deadline, judged with a 5 s scheduling tolerance (15 s).
 */
public class UnixDomainDisplayDeliveryTimeoutTest {

    private static final long BOUND_MILLIS = 15_000;

    @Test
    public void missingSocketFailsUnavailableWithinConnectBound() {
        Path missing = Path.of(System.getProperty("java.io.tmpdir"), "jjava-missing-" + System.nanoTime() + ".sock");
        UnixDomainDisplayDelivery delivery = new UnixDomainDisplayDelivery(missing);
        DisplayRequest request = request(new DisplayData("x"));
        long start = System.nanoTime();

        DisplayDeliveryException e = assertThrows(DisplayDeliveryException.class, () -> delivery.deliver(request));

        assertEquals(UNAVAILABLE, e.kind());
        assertTrue(elapsedMillis(start) < BOUND_MILLIS);
    }

    @Test
    public void peerThatNeverReadsLargeRequestFailsUncertainWithinBound() throws Exception {
        Path socket = Files.createTempDirectory("uds-timeout").resolve("s.sock");
        try (ServerSocketChannel server = listen(socket)) {
            // Never accepts: connect completes through the listen backlog, then the large write stalls.
            UnixDomainDisplayDelivery delivery = new UnixDomainDisplayDelivery(socket);
            DisplayRequest request = request(new DisplayData("x".repeat(8 << 20)));
            long start = System.nanoTime();

            DisplayDeliveryException e = assertThrows(DisplayDeliveryException.class, () -> delivery.deliver(request));

            // the cause is reported so a platform-specific failure mode is visible in CI output
            assertEquals(UNCERTAIN, e.kind(), () -> "cause: " + e.getCause());
            assertTrue(elapsedMillis(start) < BOUND_MILLIS);
        }
    }

    @Test
    public void peerThatReadsButNeverRepliesFailsUncertainWithinBound() throws Exception {
        Path socket = Files.createTempDirectory("uds-timeout").resolve("s.sock");
        try (ServerSocketChannel server = listen(socket)) {
            Thread peer = new Thread(() -> holdAfterRead(server));
            peer.setDaemon(true);
            peer.start();

            UnixDomainDisplayDelivery delivery = new UnixDomainDisplayDelivery(socket);
            DisplayRequest request = request(new DisplayData("small"));
            long start = System.nanoTime();

            DisplayDeliveryException e = assertThrows(DisplayDeliveryException.class, () -> delivery.deliver(request));

            assertEquals(UNCERTAIN, e.kind());
            assertTrue(elapsedMillis(start) < BOUND_MILLIS);
        }
    }

    // Reads the request, then blocks until the client closes its end at the deadline (read returns -1).
    private static void holdAfterRead(ServerSocketChannel server) {
        try (SocketChannel accepted = server.accept()) {
            accepted.read(ByteBuffer.allocate(1 << 20));
            accepted.read(ByteBuffer.allocate(1));
        } catch (Exception ignored) {
            // the peer thread only exists to stall the client; its own failure has no observer
        }
    }

    private static ServerSocketChannel listen(Path socket) throws Exception {
        ServerSocketChannel server = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
        server.bind(UnixDomainSocketAddress.of(socket));
        return server;
    }

    private static DisplayRequest request(DisplayData data) {
        return DisplayRequest.of(DisplayRequest.Operation.DISPLAY, "id", data);
    }

    private static long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
