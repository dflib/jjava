package org.dflib.jjava.kernel.execution;

/**
 * The ordinary evaluation outcome of a cell: the JShell text of its last value or expression. A null text means the
 * result is absent. It carries no Jupyter display, message, channel, or protocol types.
 */
public record EvaluationResult(String text) {

    public static final EvaluationResult NONE = new EvaluationResult(null);
}
