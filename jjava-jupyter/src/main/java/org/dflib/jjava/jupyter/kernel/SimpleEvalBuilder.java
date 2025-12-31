package org.dflib.jjava.jupyter.kernel;

import org.dflib.jjava.jupyter.instrumentation.EvalTimer;
import org.dflib.jjava.jupyter.kernel.display.DisplayData;

public class SimpleEvalBuilder<T> implements EvalBuilder<T> {

    private final BaseKernel kernel;
    private final String source;
    private final EvalTimer timer;

    protected SimpleEvalBuilder(BaseKernel kernel, String source, EvalTimer timer) {
        this.kernel = kernel;
        this.source = source;
        this.timer = timer;
    }

    @Override
    public EvalBuilder<T> timed(EvalTimer timer) {
        return new SimpleEvalBuilder<>(kernel, source, timer);
    }

    @Override
    public EvalBuilder<T> resolveMagics() {
        return new SimpleEvalBuilder<>(kernel, kernel.getMagicsResolver().resolve(source), timer);
    }

    @Override
    public EvalBuilder<DisplayData> renderResults() {
        return new RenderedEvalBuilder(kernel, this);
    }

    @Override
    public T eval() {
        timer.start();
        try {
            return (T) kernel.doEval(source, timer);
        } finally {
            timer.stop();
        }
    }
}
