package org.dflib.jjava.jupyter.kernel.display.protocol.uds;

import org.dflib.jjava.jupyter.kernel.display.protocol.DisplayProtocol;
import org.dflib.jjava.jupyter.kernel.display.protocol.DisplayRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Manager-side Unix-domain endpoint. Accepts one request line per connection, serially, and answers with one response
 * line. Each connection is bounded by a 10 s deadline. Closing the endpoint, or an accept failure, releases the
 * channel and removes the socket file.
 */
public final class UnixDomainDisplayEndpoint implements Closeable {

    static final long DEADLINE_SECONDS = 10;
    static final ScheduledExecutorService DEADLINES = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "jjava-display-deadlines");
        thread.setDaemon(true);
        return thread;
    });

    private static final Logger LOGGER = LoggerFactory.getLogger(UnixDomainDisplayEndpoint.class);

    private final ServerSocketChannel server;
    private final Path path;
    private final DisplayRequestHandler handler;

    public UnixDomainDisplayEndpoint(DisplayRequestHandler handler) throws IOException {
        this(null, handler);
    }

    public UnixDomainDisplayEndpoint(Path socketPath, DisplayRequestHandler handler) throws IOException {
        this.handler = handler;
        this.server = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
        Path boundPath = null;
        try {
            this.server.bind(socketPath == null ? null : UnixDomainSocketAddress.of(socketPath));
            boundPath = socketPath;
            boundPath = ((UnixDomainSocketAddress) server.getLocalAddress()).getPath();
            this.path = boundPath;

            Thread acceptor = new Thread(this::acceptLoop, "jjava-display-callback");
            acceptor.setDaemon(true);
            acceptor.start();
        } catch (IOException | RuntimeException | Error e) {
            closeQuietly(server);
            if (boundPath != null) {
                try {
                    Files.deleteIfExists(boundPath);
                } catch (IOException cleanup) {
                    e.addSuppressed(cleanup);
                }
            }
            throw e;
        }
    }

    public Path path() {
        return path;
    }

    private void acceptLoop() {
        while (true) {
            SocketChannel channel;
            try {
                channel = server.accept();
            } catch (IOException e) {
                if (server.isOpen()) {
                    LOGGER.warn("Display endpoint accept failed; closing endpoint", e);
                    closeQuietly(this);
                }
                return;
            }
            try {
                handle(channel);
            } finally {
                closeQuietly(channel);
            }
        }
    }

    private void handle(SocketChannel channel) {
        ScheduledFuture<?> deadline = DEADLINES.schedule(() -> closeQuietly(channel), DEADLINE_SECONDS, TimeUnit.SECONDS);
        try {
            writeLine(channel, respond(channel));
        } catch (IOException e) {
            LOGGER.warn("Display connection failed", e);
        } finally {
            deadline.cancel(false);
        }
    }

    private String respond(SocketChannel channel) throws IOException {
        try {
            handler.handle(DisplayProtocol.decode(readLine(channel)));
            return DisplayProtocol.response(null);
        } catch (RuntimeException e) {
            return DisplayProtocol.response(e);
        }
    }

    @Override
    public void close() throws IOException {
        server.close();
        Files.deleteIfExists(path);
    }

    static String readLine(ReadableByteChannel channel) throws IOException {
        ByteArrayOutputStream line = new ByteArrayOutputStream();
        ByteBuffer buffer = ByteBuffer.allocate(8192);
        while (true) {
            buffer.clear();
            if (channel.read(buffer) < 0) {
                return line.size() == 0 ? null : line.toString(StandardCharsets.UTF_8);
            }
            buffer.flip();
            while (buffer.hasRemaining()) {
                byte b = buffer.get();
                if (b == '\n') {
                    return line.toString(StandardCharsets.UTF_8);
                }
                line.write(b);
            }
        }
    }

    static void writeLine(SocketChannel channel, String text) throws IOException {
        ByteBuffer out = ByteBuffer.wrap((text + "\n").getBytes(StandardCharsets.UTF_8));
        while (out.hasRemaining()) {
            channel.write(out);
        }
    }

    static void closeQuietly(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException ignored) {
            // best-effort close on a failure or deadline path; the caller has no recovery action
        }
    }
}
