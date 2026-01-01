package org.dflib.jjava.jupyter.instrumentation;

import java.lang.management.ThreadMXBean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * A timer that makes an attempt to capture CPU and user time in addition to the overall elapsed time.
 */
public abstract class WallAndCpuTimer implements EvalTimer {

    private final ThreadMXBean threadMXBean;
    private final AtomicLong totalWallTime;
    private final AtomicLong totalUserTime;
    private final AtomicLong totalCpuTime;

    public static boolean canMeasureCpuTimes(ThreadMXBean threadMXBean) {
        // according to JavaDocs, isThreadCpuTimeEnabled() can throw in some cases
        try {
            return threadMXBean.isCurrentThreadCpuTimeSupported() && threadMXBean.isThreadCpuTimeEnabled();
        } catch (Exception e) {
            return false;
        }
    }

    public WallAndCpuTimer(ThreadMXBean threadMXBean) {
        this.threadMXBean = threadMXBean;
        this.totalWallTime = new AtomicLong();
        this.totalUserTime = new AtomicLong();
        this.totalCpuTime = new AtomicLong();
    }

    @Override
    public void start() {
        totalWallTime.set(0);
        totalUserTime.set(0);
        totalCpuTime.set(0);
    }

    @Override
    public <T> T runAndMeasureStep(Supplier<T> timedAction) {

        long t0 = System.nanoTime();
        long ut0 = threadMXBean.getCurrentThreadUserTime();
        long ct0 = threadMXBean.getCurrentThreadCpuTime();

        try {
            return timedAction.get();
        } finally {

            long ct1 = threadMXBean.getCurrentThreadCpuTime();
            long ut1 = threadMXBean.getCurrentThreadUserTime();
            long t1 = System.nanoTime();

            totalWallTime.addAndGet(t1 - t0);

            // sanity check
            if (ut0 != -1 && ut1 != -1 && ct0 != -1 && ct1 != -1) {
                totalUserTime.addAndGet(ut1 - ut0);
                totalCpuTime.addAndGet(ct1 - ct0);
            } else {
                totalUserTime.set(-1);
                totalCpuTime.set(-1);
            }
        }
    }

    @Override
    public void stop() {
        onStop(totalWallTime.get(), totalUserTime.get(), totalCpuTime.get());
    }

    protected abstract void onStop(
            long wallTimeNanos,
            long userTimeNanos,
            long totalTimeNanos);

}
