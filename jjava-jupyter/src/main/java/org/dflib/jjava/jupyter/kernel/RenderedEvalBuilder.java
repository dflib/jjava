package org.dflib.jjava.jupyter.kernel;

import org.dflib.jjava.jupyter.kernel.display.DisplayData;

public class RenderedEvalBuilder implements EvalBuilder<DisplayData> {

    private final BaseKernel kernel;
    private final EvalBuilder<?> delegate;

    protected RenderedEvalBuilder(BaseKernel kernel, EvalBuilder<?> delegate) {
        this.kernel = kernel;
        this.delegate = delegate;
    }

    @Override
    public EvalBuilder<DisplayData> renderResults() {
        return this;
    }

    @Override
    public RenderedEvalBuilder resolveMagics() {
        return new RenderedEvalBuilder(kernel, delegate.resolveMagics());
    }

    @Override
    public DisplayData eval() {
        Object o = delegate.eval();

        if (o == null) {
            return null;
        }

        return o instanceof DisplayData
                ? (DisplayData) o
                : kernel.getRenderer().render(o);
    }
}
