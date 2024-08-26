package org.dflib.jjava.jupyter.channels;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.LongSupplier;
import java.util.function.ToLongFunction;
import java.util.logging.Logger;

public class Loop extends Thread {
    private final Logger logger;

    private volatile boolean running = false;
    private final LongSupplier loopBody;

    private volatile Runnable onCloseCb;
    private volatile ToLongFunction<Throwable> onErrorCb;
    private final Queue<Runnable> runNextQueue;

    public Loop(String name, long sleep, Runnable target) {
        this(name, () -> {
            target.run();
            return sleep;
        });
    }

    public Loop(String name, LongSupplier target) {
        super(name);

        this.loopBody = target;

        this.runNextQueue = new LinkedBlockingQueue<>();

        this.logger = Logger.getLogger("Loop-" + name);
    }

    public void onClose(Runnable callback) {
        if (this.onCloseCb != null) {
            Runnable oldCallback = this.onCloseCb;
            this.onCloseCb = () -> {
                oldCallback.run();
                callback.run();
            };
        } else {
            this.onCloseCb = callback;
        }
    }

    public void onError(ToLongFunction<Throwable> callback) {
        if (this.onErrorCb == null) {
            this.onErrorCb = callback;
            return;
        }

        // Adding a second handler will only be invoked if the
        // previous one throws (or rethrows) the incoming exception.
        // The callback is invoked with the rethrown exception.
        ToLongFunction<Throwable> oldCallback = this.onErrorCb;
        this.onErrorCb = t -> {
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
        while (this.running) {
            long sleep;
            try {
                // Run the loop body
                sleep = this.loopBody.getAsLong();

                // Run all queued tasks
                while ((next = this.runNextQueue.poll()) != null)
                    next.run();
            } catch (Throwable t) {
                if (this.onErrorCb != null)
                    sleep = this.onErrorCb.applyAsLong(t);
                else
                    throw t;
            }

            if (sleep > 0) {
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException e) {
                    this.logger.info("Loop interrupted. Stopping...");
                    this.running = false;
                }
            } else if (sleep < 0) {
                this.logger.info("Loop interrupted by a negative sleep request. Stopping...");
                this.running = false;
            }
        }

        this.logger.info("Running loop shutdown callback.");

        if (this.onCloseCb != null)
            this.onCloseCb.run();
        this.onCloseCb = null;

        this.logger.info("Loop stopped.");
    }

    @Override
    public synchronized void start() {
        this.logger.info("Loop starting...");

        this.running = true;
        super.start();

        this.logger.info("Loop started.");
    }

    public void shutdown() {
        this.running = false;
        this.logger.info("Loop shutdown.");
    }
}
