package org.dflib.jjava.jupyter.kernel.display.protocol.uds;

import org.dflib.jjava.jupyter.kernel.display.protocol.DisplayDelivery;
import org.dflib.jjava.jupyter.kernel.display.protocol.DisplayDeliveryException;
import org.dflib.jjava.jupyter.kernel.display.protocol.DisplayProtocol;
import org.dflib.jjava.jupyter.kernel.display.protocol.DisplayRequest;

import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.dflib.jjava.jupyter.kernel.display.protocol.DisplayDeliveryException.Kind.UNAVAILABLE;
import static org.dflib.jjava.jupyter.kernel.display.protocol.DisplayDeliveryException.Kind.UNCERTAIN;
import static org.dflib.jjava.jupyter.kernel.display.protocol.uds.UnixDomainDisplayEndpoint.DEADLINES;
import static org.dflib.jjava.jupyter.kernel.display.protocol.uds.UnixDomainDisplayEndpoint.DEADLINE_SECONDS;
import static org.dflib.jjava.jupyter.kernel.display.protocol.uds.UnixDomainDisplayEndpoint.closeQuietly;
import static org.dflib.jjava.jupyter.kernel.display.protocol.uds.UnixDomainDisplayEndpoint.readLine;

/**
 * Executor-side delivery over a Unix-domain socket. One deadline covers connect, write, and the response read. A
 * failure before any payload byte is written is UNAVAILABLE; a failure after is UNCERTAIN. Nothing is retried.
 */
public final class UnixDomainDisplayDelivery implements DisplayDelivery {

    private static final String UNAVAILABLE_MESSAGE = "Remote display channel is not available";
    private static final String UNCERTAIN_MESSAGE = "Display delivery outcome is uncertain; nothing was retried";

    private final Path socketPath;

    public UnixDomainDisplayDelivery(Path socketPath) {
        this.socketPath = socketPath;
    }

    @Override
    public void deliver(DisplayRequest request) {
        ByteBuffer out = ByteBuffer.wrap((DisplayProtocol.encode(request) + "\n").getBytes(StandardCharsets.UTF_8));

        SocketChannel channel;
        try {
            channel = SocketChannel.open(StandardProtocolFamily.UNIX);
        } catch (IOException e) {
            throw new DisplayDeliveryException(UNAVAILABLE, UNAVAILABLE_MESSAGE, e);
        }

        ScheduledFuture<?> deadline = DEADLINES.schedule(() -> closeQuietly(channel), DEADLINE_SECONDS, TimeUnit.SECONDS);
        // Once connected, a write may have partly reached the peer even if the call never returned (a deadline close
        // interrupts it, and the byte count only advances on return). So UNAVAILABLE is reserved for failures before connect.
        boolean connected = false;
        try (channel) {
            channel.connect(UnixDomainSocketAddress.of(socketPath));
            connected = true;
            while (out.hasRemaining()) {
                channel.write(out);
            }
            DisplayProtocol.checkResponse(readLine(channel));
        } catch (IOException e) {
            throw new DisplayDeliveryException(
                    connected ? UNCERTAIN : UNAVAILABLE,
                    connected ? UNCERTAIN_MESSAGE : UNAVAILABLE_MESSAGE,
                    e);
        } finally {
            deadline.cancel(false);
        }
    }
}
