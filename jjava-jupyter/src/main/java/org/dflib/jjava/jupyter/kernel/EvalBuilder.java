package org.dflib.jjava.jupyter.kernel;

import org.dflib.jjava.jupyter.kernel.display.DisplayData;

public interface EvalBuilder<T> {

    EvalBuilder<T> resolveMagics();

    EvalBuilder<DisplayData> renderResults();

    T eval();
}
