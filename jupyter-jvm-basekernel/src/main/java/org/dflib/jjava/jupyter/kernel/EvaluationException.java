package org.dflib.jjava.jupyter.kernel;

import java.util.Objects;

public class EvaluationException extends RuntimeException {

    public EvaluationException(Throwable cause) {
        super(Objects.requireNonNull(cause));
    }
}
