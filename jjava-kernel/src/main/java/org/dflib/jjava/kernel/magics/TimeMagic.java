package org.dflib.jjava.kernel.magics;

import org.dflib.jjava.jupyter.instrumentation.WallTimer;
import org.dflib.jjava.jupyter.kernel.BaseKernel;
import org.dflib.jjava.jupyter.kernel.display.DisplayData;
import org.dflib.jjava.jupyter.kernel.magic.CellMagic;
import org.dflib.jjava.jupyter.kernel.magic.LineMagic;
import org.dflib.jjava.kernel.JavaKernel;

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
        System.out.println("TimeMagic thread: " + Thread.currentThread().getName());
        return kernel.evalBuilder(code)
                .resolveMagics()
                .renderResults()
                .timed(new TMWallTimer(kernel))
                .eval();
    }

    private static void displayWallTime(BaseKernel kernel, long wallTimeNanos) {
        String wallTime = String.format("Wall time: %.3f s", wallTimeNanos / NANONS_IN_SEC);
        kernel.display(new DisplayData(wallTime).setDisplayId(UUID.randomUUID().toString()));
    }

    static class TMWallTimer extends WallTimer {
        private final BaseKernel kernel;

        public TMWallTimer(BaseKernel kernel) {
            this.kernel = kernel;
        }

        @Override
        protected void onStop(long wallTimeNanos) {
            displayWallTime(kernel, wallTimeNanos);
        }
    }
}
