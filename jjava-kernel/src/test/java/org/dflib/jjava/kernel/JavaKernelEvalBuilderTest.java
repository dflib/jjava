package org.dflib.jjava.kernel;

import org.dflib.jjava.jupyter.kernel.display.DisplayData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;

public class JavaKernelEvalBuilderTest {

    private JavaKernel kernel;

    @BeforeEach
    void setUp() {
        kernel = JavaKernel.builder().extensionsEnabled(false).build();
    }

    @AfterEach
    void tearDown() {
        kernel.onShutdown(false);
    }

    @Test
    void rendersManagerLineAndCellMagicsRegardlessOfBuilderOrder() {
        AtomicInteger calls = new AtomicInteger();
        kernel.getMagicsRegistry().registerLineMagic("probe", (k, args) -> {
            assertSame(kernel, k);
            assertEquals(List.of("first", "second"), args);
            return "magic-" + calls.incrementAndGet();
        });
        kernel.getMagicsRegistry().registerCellMagic("probeCell", (k, args, body) -> {
            assertSame(kernel, k);
            assertEquals(List.of("first", "second"), args);
            assertEquals("cell body", body);
            return "cell-result";
        });

        DisplayData renderFirst = kernel.evalBuilder("%probe first second").renderResults().resolveMagics().eval();
        DisplayData resolveFirst = kernel.evalBuilder("%probe first second")
                .resolveMagics()
                .renderResults()
                .renderResults()
                .eval();
        DisplayData cellResult = kernel.evalBuilder("%%probeCell first second\ncell body")
                .renderResults()
                .resolveMagics()
                .eval();

        assertEquals("magic-1", renderFirst.getData().get("text/plain"));
        assertEquals("magic-2", resolveFirst.getData().get("text/plain"));
        assertEquals("cell-result", cellResult.getData().get("text/plain"));
    }

    @Test
    void preservesNullAndDisplayDataMagicResults() {
        kernel.getMagicsRegistry().registerLineMagic("nothing", (k, args) -> null);
        assertNull(kernel.evalBuilder("%nothing").renderResults().resolveMagics().eval());

        DisplayData returned = new DisplayData("already rendered");
        kernel.getMagicsRegistry().registerLineMagic("bundle", (k, args) -> returned);
        assertSame(returned, kernel.evalBuilder("%bundle").resolveMagics().renderResults().eval());
    }

    @Test
    void textualNullResultIsPresentWhileAbsentResultIsNot() {
        DisplayData textualNull = kernel.evalBuilder("String nothing = null;\nnothing").renderResults().eval();
        assertEquals("null", textualNull.getData().get("text/plain"));
        assertNull(kernel.evalBuilder("int declared = 1;").renderResults().eval());
    }

    @Test
    void doesNotUseManagerRendererForExecutorResults() {
        kernel.getRenderer().createRegistration(String.class)
                .register((value, context) -> fail("Executor result must not be rendered in the manager"));

        DisplayData executorResult = kernel.evalBuilder("40 + 2").renderResults().eval();
        assertEquals("42", executorResult.getData().get("text/plain"));
    }
}
