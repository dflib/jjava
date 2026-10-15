package org.dflib.jjava.kernel.execution;

import org.dflib.jjava.kernel.JavaKernel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JdiExecutionControlRedefinitionTest {

    @Test
    public void classAndMethodRedefinitionKeepsSavedValues() {
        JavaKernel kernel = JavaKernel.builder().extensionsEnabled(false).build();
        try {
            kernel.evalBuilder("class Stateful { int value() { return 1; } }").eval();
            kernel.evalBuilder("Stateful instance = new Stateful();").eval();
            kernel.evalBuilder("int saved = instance.value();").eval();
            assertEquals("1", kernel.evalBuilder("saved").eval());

            kernel.evalBuilder("class Stateful { int value() { return 2; } }").eval();

            assertEquals("1", kernel.evalBuilder("saved").eval());
            assertEquals("2", kernel.evalBuilder("instance.value()").eval());
            assertEquals("2", kernel.evalBuilder("new Stateful().value()").eval());

            kernel.evalBuilder("int currentValue() { return 10; }").eval();
            kernel.evalBuilder("int savedMethodValue = currentValue();").eval();
            kernel.evalBuilder("int currentValue() { return 20; }").eval();
            assertEquals("10", kernel.evalBuilder("savedMethodValue").eval());
            assertEquals("20", kernel.evalBuilder("currentValue()").eval());
        } finally {
            kernel.onShutdown(false);
        }
    }
}
