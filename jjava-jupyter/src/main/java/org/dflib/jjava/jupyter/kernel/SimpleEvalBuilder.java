package org.dflib.jjava.jupyter.kernel;

import org.dflib.jjava.jupyter.kernel.display.DisplayData;

public class SimpleEvalBuilder<T> implements EvalBuilder<T> {

    private final BaseKernel kernel;
    private final String source;

    protected SimpleEvalBuilder(BaseKernel kernel, String source) {
        this.kernel = kernel;
        this.source = source;
    }

    @Override
    public EvalBuilder<T> resolveMagics() {
        return new SimpleEvalBuilder<>(kernel, kernel.getMagicsResolver().resolve(source));
    }

    @Override
    public EvalBuilder<DisplayData> renderResults() {
        return new RenderedEvalBuilder(kernel, this);
    }

    @Override
    public T eval() {
        return (T) kernel.doEval(source);
    }
}
