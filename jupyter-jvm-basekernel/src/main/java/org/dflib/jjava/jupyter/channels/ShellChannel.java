package org.dflib.jjava.jupyter.channels;

import org.dflib.jjava.jupyter.kernel.KernelConnectionProperties;
import org.dflib.jjava.jupyter.messages.HMACGenerator;
import org.dflib.jjava.jupyter.messages.Message;
import org.zeromq.SocketType;
import org.zeromq.ZMQ;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ShellChannel extends JupyterSocket {
    private static final long SHELL_DEFAULT_LOOP_SLEEP_MS = 50;
    private static final AtomicInteger SHELL_ID = new AtomicInteger();

    private volatile Loop ioloop;

    private final boolean isControl;
    private final JupyterConnection connection;
    private final long sleep;

    public ShellChannel(ZMQ.Context context, HMACGenerator hmacGenerator, boolean isControl, JupyterConnection connection, long sleep) {
        super(context, SocketType.ROUTER, hmacGenerator, Logger.getLogger(isControl ? "ControlChannel" : "ShellChannel"));
        this.isControl = isControl;
        this.connection = connection;
        this.sleep = sleep;
    }

    public ShellChannel(ZMQ.Context context, HMACGenerator hmacGenerator, boolean isControl, JupyterConnection connection) {
        this(context, hmacGenerator, isControl, connection, SHELL_DEFAULT_LOOP_SLEEP_MS);
    }

    private boolean isBound() {
        return this.ioloop != null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void bind(KernelConnectionProperties connProps) {
        if (this.isBound())
            throw new IllegalStateException("Shell channel already bound");

        String channelThreadName = "Shell-" + SHELL_ID.getAndIncrement();
        String addr = JupyterSocket.formatAddress(connProps.getTransport(), connProps.getIp(),
                isControl ? connProps.getControlPort() : connProps.getShellPort());

        logger.log(Level.INFO, String.format("Binding %s to %s.", channelThreadName, addr));
        super.bind(addr);

        ZMQ.Poller poller = super.ctx.poller(1);
        poller.register(this, ZMQ.Poller.POLLIN);

        this.ioloop = new Loop(channelThreadName, this.sleep, () -> {
            int events = poller.poll(0);
            if (events > 0) {
                Message<?> message = super.readMessage();

                ShellHandler handler = connection.getHandler(message.getHeader().getType());
                if (handler != null) {
                    super.logger.info("Handling message: " + message.getHeader().getType().getName());
                    ShellReplyEnvironment env = connection.prepareReplyEnv(this, message);
                    try {
                        handler.handle(env, message);
                    } catch (Exception e) {
                        super.logger.log(Level.SEVERE, "Unhandled exception handling " + message.getHeader().getType().getName() + ". " + e.getClass().getSimpleName() + " - " + e.getLocalizedMessage());
                    } finally {
                        env.resolveDeferrals();
                    }
                    if (env.isMarkedForShutdown()) {
                        super.logger.info(channelThreadName + " shutting down connection as environment was marked for shutdown.");
                        this.connection.close();
                    }
                } else {
                    super.logger.log(Level.SEVERE, "Unhandled message: " + message.getHeader().getType().getName());
                }
            }
        });

        this.ioloop.onClose(() -> {
            logger.log(Level.INFO, channelThreadName + " shutdown.");
            this.ioloop = null;
        });

        this.ioloop.start();

        logger.log(Level.INFO, "Polling on " + channelThreadName);
    }

    @Override
    public void close() {
        if (this.isBound())
            this.ioloop.shutdown();

        super.close();
    }

    @Override
    public void waitUntilClose() {
        if (this.ioloop != null) {
            try {
                this.ioloop.join();
            } catch (InterruptedException ignored) { }
        }
    }
}
