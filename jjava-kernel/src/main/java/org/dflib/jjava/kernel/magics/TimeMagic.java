package org.dflib.jjava.kernel.magics;

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
        long started = System.nanoTime();
        try {
            return kernel.evalBuilder(code)
                    .resolveMagics()
                    .renderResults()
                    .eval();
        } finally {
            displayWallTime(kernel, System.nanoTime() - started);
        }
    }

    private static void displayWallTime(BaseKernel kernel, long wallTimeNanos) {
        String wallTime = String.format("Wall time: %.3f s", wallTimeNanos / NANONS_IN_SEC);
        kernel.display(new DisplayData(wallTime).setDisplayId(UUID.randomUUID().toString()));
    }
}
