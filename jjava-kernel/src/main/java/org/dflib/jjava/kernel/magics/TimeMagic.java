package org.dflib.jjava.kernel.magics;

import org.dflib.jjava.jupyter.kernel.BaseKernel;
import org.dflib.jjava.jupyter.kernel.display.DisplayData;
import org.dflib.jjava.jupyter.kernel.magic.CellMagic;
import org.dflib.jjava.jupyter.kernel.magic.LineMagic;
import org.dflib.jjava.jupyter.telemetry.TelemetryCollector;
import org.dflib.jjava.jupyter.telemetry.WallAndCpuTimeCollector;
import org.dflib.jjava.jupyter.telemetry.WallTimeCollector;
import org.dflib.jjava.kernel.JavaKernel;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.List;
import java.util.UUID;

/**
 * Measures time spent executing some notebook code.
 */
public class TimeMagic implements LineMagic<DisplayData, JavaKernel>, CellMagic<DisplayData, JavaKernel> {

    private static final double NANONS_IN_SEC = 1_000_000_000.;

    @Override
    public DisplayData eval(JavaKernel kernel, List<String> args) throws Exception {
        return args.isEmpty() ? null : timeAndRunCode(kernel, String.join(" ", args));
    }

    @Override
    public DisplayData eval(JavaKernel kernel, List<String> args, String body) throws Exception {
        return timeAndRunCode(kernel, body);
    }

    private DisplayData timeAndRunCode(JavaKernel kernel, String code) {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

        TelemetryCollector<?> collector = WallAndCpuTimeCollector.canMeasureCpuTimes(threadMXBean)
                ? new TMWallAndCpuTimeCollector(kernel, threadMXBean)
                : new TMWallTimeCollector(kernel);

        kernel.getEvaluator().startThreadTelemetryCollection(collector);

        try {
            return kernel.evalBuilder(code)
                    .resolveMagics()
                    .renderResults()
                    .eval();
        } finally {
            kernel.getEvaluator().stopThreadTelemetryCollection();
        }
    }

    private static void displayCpuTime(BaseKernel kernel, long userTimeNanos, long sysTimeNanos, long totalTimeNanos) {
        String cpuTimes = String.format("CPU times: user %.3f s, sys %.3f s, total %.3f s",
                userTimeNanos / NANONS_IN_SEC,
                sysTimeNanos / NANONS_IN_SEC,
                totalTimeNanos / NANONS_IN_SEC);

        kernel.display(new DisplayData(cpuTimes).setDisplayId(UUID.randomUUID().toString()));
    }

    private static void displayWallTime(BaseKernel kernel, long wallTimeNanos) {
        String wallTime = String.format("Wall time: %.3f s", wallTimeNanos / NANONS_IN_SEC);
        kernel.display(new DisplayData(wallTime).setDisplayId(UUID.randomUUID().toString()));
    }

    static class TMWallTimeCollector extends WallTimeCollector {
        private final BaseKernel kernel;

        public TMWallTimeCollector(BaseKernel kernel) {
            this.kernel = kernel;
        }

        @Override
        protected void onStop(long wallTimeNanos) {
            displayWallTime(kernel, wallTimeNanos);
        }
    }


    static class TMWallAndCpuTimeCollector extends WallAndCpuTimeCollector {
        private final BaseKernel kernel;

        public TMWallAndCpuTimeCollector(BaseKernel kernel, ThreadMXBean threadMXBean) {
            super(threadMXBean);
            this.kernel = kernel;
        }

        @Override
        protected void onStop(long wallTimeNanos, long userTimeNanos, long totalTimeNanos) {
            // sanity check: CPU and user times can't be measured sometimes
            if (userTimeNanos != -1 && totalTimeNanos != -1) {
                displayCpuTime(kernel, userTimeNanos, totalTimeNanos - userTimeNanos, totalTimeNanos);
            }

            displayWallTime(kernel, wallTimeNanos);
        }
    }
}
