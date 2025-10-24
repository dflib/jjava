package org.dflib.jjava.jupyter.channels;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.LongSupplier;
import java.util.function.ToLongFunction;

public class Loop extends Thread {

    private final Logger logger;
    private final LongSupplier loopBody;
    private final Queue<Runnable> runNextQueue;

    private volatile boolean running;
    private volatile Runnable onCloseCb;
    private volatile ToLongFunction<Throwable> onErrorCb;

    public Loop(String name, long sleep, Runnable target) {
        super(name);

        this.loopBody = () -> {
            target.run();
            return sleep;
        };

        this.runNextQueue = new LinkedBlockingQueue<>();
        this.logger = LoggerFactory.getLogger("Loop-" + name);
    }

    public void onClose(Runnable callback) {
        if (onCloseCb != null) {
            Runnable oldCallback = this.onCloseCb;
            onCloseCb = () -> {
                oldCallback.run();
                callback.run();
            };
        } else {
            onCloseCb = callback;
        }
    }

    public void onError(ToLongFunction<Throwable> callback) {
        if (onErrorCb == null) {
            onErrorCb = callback;
            return;
        }

        // Adding a second handler will only be invoked if the
        // previous one throws (or rethrows) the incoming exception.
        // The callback is invoked with the rethrown exception.
        ToLongFunction<Throwable> oldCallback = this.onErrorCb;
        onErrorCb = t -> {
            try {
                return oldCallback.applyAsLong(t);
            } catch (Throwable tPrime) {
                return callback.applyAsLong(tPrime);
            }
        };
    }

    public void doNext(Runnable next) {
        this.runNextQueue.offer(next);
    }

    @Override
    public void run() {
        Runnable next;
        while (running) {
            long sleep;
            try {
                // Run the loop body
                sleep = this.loopBody.getAsLong();

                // Run all queued tasks
                while ((next = this.runNextQueue.poll()) != null) {
                    next.run();
                }
            } catch (Throwable t) {
                if (this.onErrorCb != null) {
                    sleep = this.onErrorCb.applyAsLong(t);
                } else {
                    throw t;
                }
            }

            if (sleep > 0) {
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException e) {
                    logger.debug("Loop interrupted. Stopping...");
                    running = false;
                }
            } else if (sleep < 0) {
                logger.debug("Loop interrupted by a negative sleep request. Stopping...");
                running = false;
            }
        }

        logger.debug("Running loop shutdown callback.");

        if (onCloseCb != null) {
            onCloseCb.run();
            onCloseCb = null;
        }

        logger.info("Loop stopped.");
    }

    @Override
    public synchronized void start() {
        logger.debug("Loop starting...");

        running = true;
        super.start();

        logger.info("Loop started.");
    }

    public void shutdown() {
        running = false;
        logger.debug("Loop shutdown.");
    }
}
