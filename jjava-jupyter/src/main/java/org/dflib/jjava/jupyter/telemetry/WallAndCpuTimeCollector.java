package org.dflib.jjava.jupyter.telemetry;

import java.lang.management.ThreadMXBean;
import java.util.concurrent.atomic.AtomicLong;

public abstract class WallAndCpuTimeCollector implements TelemetryCollector<WallAndCpuTimeCollector.Measure> {

    private final ThreadMXBean threadMXBean;
    private final AtomicLong totalWT;
    private final AtomicLong totalUT;
    private final AtomicLong totalCT;

    public static boolean canMeasureCpuTimes(ThreadMXBean threadMXBean) {
        // according to JavaDocs, isThreadCpuTimeEnabled() can throw in some cases
        try {
            return threadMXBean.isCurrentThreadCpuTimeSupported() && threadMXBean.isThreadCpuTimeEnabled();
        } catch (Exception e) {
            return false;
        }
    }

    public WallAndCpuTimeCollector(ThreadMXBean threadMXBean) {
        this.threadMXBean = threadMXBean;
        this.totalWT = new AtomicLong();
        this.totalUT = new AtomicLong();
        this.totalCT = new AtomicLong();
    }

    @Override
    public Measure measurementStart() {
        return new Measure(
                System.nanoTime(),
                threadMXBean.getCurrentThreadUserTime(),
                threadMXBean.getCurrentThreadCpuTime()
        );
    }

    @Override
    public void measurementEnd(Measure m) {

        long wt = System.nanoTime();
        long ut = threadMXBean.getCurrentThreadUserTime();
        long ct = threadMXBean.getCurrentThreadCpuTime();

        totalWT.addAndGet(wt - m.wt);

        // sanity check
        if (m.ut != -1 && ut != -1 && m.ct != -1 && ct != -1) {
            totalUT.addAndGet(ut - m.ut);
            totalCT.addAndGet(ct - m.ct);
        } else {
            totalUT.set(-1);
            totalCT.set(-1);
        }
    }

    @Override
    public void stop() {
        onStop(totalWT.get(), totalUT.get(), totalCT.get());
    }

    protected abstract void onStop(long wallTimeNanos, long userTimeNanos, long totalTimeNanos);

    public static class Measure {
        final long wt;
        final long ut;
        final long ct;

        public Measure(long wt, long ut, long ct) {
            this.ct = ct;
            this.wt = wt;
            this.ut = ut;
        }
    }
}
