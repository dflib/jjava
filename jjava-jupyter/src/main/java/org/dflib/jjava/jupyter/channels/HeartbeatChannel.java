package org.dflib.jjava.jupyter.channels;

import org.dflib.jjava.jupyter.kernel.KernelConnectionProperties;
import org.dflib.jjava.jupyter.messages.HMACGenerator;
import org.zeromq.SocketType;
import org.zeromq.ZMQ;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HeartbeatChannel extends JupyterSocket {
    private static final long HB_DEFAULT_SLEEP_MS = 500;

    private static final AtomicInteger HEARTBEAT_ID = new AtomicInteger();

    private final long sleep;
    private volatile Loop pulse;

    public HeartbeatChannel(ZMQ.Context context, HMACGenerator hmacGenerator, long sleep) {
        super(context, SocketType.REP, hmacGenerator, Logger.getLogger("HeartbeatChannel"));
        this.sleep = sleep;
    }

    public HeartbeatChannel(ZMQ.Context context, HMACGenerator hmacGenerator) {
        this(context, hmacGenerator, HB_DEFAULT_SLEEP_MS);
    }

    private boolean isBound() {
        return this.pulse != null;
    }

    @Override
    public void bind(KernelConnectionProperties connProps) {
        if (this.isBound())
            throw new IllegalStateException("Heartbeat channel already bound");

        String channelThreadName = "Heartbeat-" + HEARTBEAT_ID.getAndIncrement();
        String addr = formatAddress(connProps.getTransport(), connProps.getIp(), connProps.getHbPort());

        logger.log(Level.INFO, String.format("Binding %s to %s.", channelThreadName, addr));
        super.bind(addr);

        ZMQ.Poller poller = super.ctx.poller(1);
        poller.register(this, ZMQ.Poller.POLLIN);

        this.pulse = new Loop(channelThreadName, this.sleep, () -> {
            int events = poller.poll(0);
            if (events > 0) {
                byte[] msg = this.recv();
                if (msg == null) {
                    //Error during receive, just continue
                    super.logger.log(Level.SEVERE, "Poll returned 1 event but could not read the echo string");
                    return;
                }
                if (!this.send(msg)) {
                    super.logger.log(Level.SEVERE, "Could not send heartbeat reply");
                }
                super.logger.log(Level.FINEST, "Heartbeat pulse");
            }
        });
        this.pulse.onClose(() -> {
            logger.log(Level.INFO, channelThreadName + " shutdown.");
            this.pulse = null;
        });
        this.pulse.start();
        logger.log(Level.INFO, "Polling on " + channelThreadName);
    }

    @Override
    public void close() {
        if (this.isBound())
            this.pulse.shutdown();

        super.close();
    }

    @Override
    public void waitUntilClose() {
        if (this.pulse != null) {
            try {
                this.pulse.join();
            } catch (InterruptedException ignored) { }
        }
    }
}
