package org.dflib.jjava.jupyter.kernel;

import org.dflib.jjava.jupyter.instrumentation.EvalTimer;
import org.dflib.jjava.jupyter.kernel.display.DisplayData;

public interface EvalBuilder<T> {

    EvalBuilder<T> resolveMagics();

    EvalBuilder<DisplayData> renderResults();

    /**
     * Installs an evaluation timer for this builder eval call.
     */
    EvalBuilder<T> timed(EvalTimer timer);

    T eval();
}
