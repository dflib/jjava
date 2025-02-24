package org.dflib.jjava;

import org.dflib.jjava.jupyter.kernel.EvaluationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JavaKernelTest {

    @Test
    public void eval_throwsRuntimeException() throws Exception {
        JavaKernel kernel = new JavaKernel("0");
        String expr = "invalid expression";

        JavaKernel mockKernel = mock(JavaKernel.class);
        when(mockKernel.evalRaw(expr)).thenThrow(new Exception("Evaluation error"));

        assertThrows(EvaluationException.class, () -> kernel.eval(expr));
    }
}
