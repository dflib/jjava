package org.dflib.jjava.jupyter.kernel;

import org.dflib.jjava.jupyter.kernel.display.protocol.DisplayDelivery;
import org.dflib.jjava.jupyter.kernel.display.protocol.DisplayDeliveryBootstrap;
import org.dflib.jjava.jupyter.kernel.display.protocol.DisplayRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ExecutorNotebookStaticsRoutingTest {

    private final List<DisplayRequest> delivered = new ArrayList<>();
    private DisplayDelivery previousDelivery;
    private BaseKernel previousKernel;

    @BeforeEach
    void setUp() {
        previousKernel = BaseKernel.notebookKernel;
        BaseKernel.notebookKernel = null;
        previousDelivery = DisplayDeliveryBootstrap.install(delivered::add);
    }

    @AfterEach
    void tearDown() {
        DisplayDeliveryBootstrap.install(previousDelivery);
        BaseKernel.notebookKernel = previousKernel;
    }

    @Test
    public void displayDeliversThroughBootstrapWithoutReadingKernel() {
        String id = ExecutorNotebookStatics.display("text");

        assertEquals(1, delivered.size());
        assertEquals(DisplayRequest.Operation.DISPLAY, delivered.get(0).operation());
        assertEquals(id, delivered.get(0).displayId());
    }

    @Test
    public void updateDisplayDeliversUpdateWithTargetId() {
        ExecutorNotebookStatics.updateDisplay("target", "text");

        assertEquals(DisplayRequest.Operation.UPDATE, delivered.get(0).operation());
        assertEquals("target", delivered.get(0).displayId());
    }

    @Test
    public void updateDisplayWithNullIdFailsBeforeDelivering() {
        assertThrows(IllegalArgumentException.class, () -> ExecutorNotebookStatics.updateDisplay(null, "text"));
        assertTrue(delivered.isEmpty());
    }

    @Test
    public void unsupportedHelpersFailWithExistingMessages() {
        assertEquals(
                "eval() cannot be called from executor code in JDI mode",
                assertThrows(UnsupportedOperationException.class, () -> ExecutorNotebookStatics.eval("1")).getMessage());
        assertEquals(
                "Magics cannot be called programmatically from executor code in JDI mode; use %magic syntax",
                assertThrows(UnsupportedOperationException.class,
                        () -> ExecutorNotebookStatics.lineMagic("m", List.of())).getMessage());
        assertEquals(
                "Magics cannot be called programmatically from executor code in JDI mode; use %%magic syntax",
                assertThrows(UnsupportedOperationException.class,
                        () -> ExecutorNotebookStatics.cellMagic("m", List.of(), "body")).getMessage());
        assertTrue(delivered.isEmpty());
    }

    @Test
    public void printfWritesStandardOutput() {
        PrintStream original = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
        try {
            ExecutorNotebookStatics.printf("x=%d", 7);
        } finally {
            System.setOut(original);
        }
        assertEquals("x=7", captured.toString(StandardCharsets.UTF_8));
    }
}
