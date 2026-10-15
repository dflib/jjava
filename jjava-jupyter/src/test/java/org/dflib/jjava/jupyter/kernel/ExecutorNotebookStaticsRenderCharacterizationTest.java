package org.dflib.jjava.jupyter.kernel;

import org.dflib.jjava.jupyter.kernel.display.DisplayData;
import org.dflib.jjava.jupyter.kernel.display.protocol.DisplayDelivery;
import org.dflib.jjava.jupyter.kernel.display.protocol.DisplayDeliveryBootstrap;
import org.dflib.jjava.jupyter.kernel.display.protocol.DisplayRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Characterizes executor render behaviour (Gate 1 for the routing change). Rendering never publishes or delivers, so
 * the recording delivery must stay empty throughout.
 */
public class ExecutorNotebookStaticsRenderCharacterizationTest {

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
    public void renderNullThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ExecutorNotebookStatics.render((Object) null));
    }

    @Test
    public void renderDisplayDataReturnsSameObject() {
        DisplayData data = new DisplayData("x");
        assertSame(data, ExecutorNotebookStatics.render((Object) data));
    }

    @Test
    public void renderScalarsAsTextPlainOnly() {
        assertEquals(Map.of("text/plain", "hi"), ExecutorNotebookStatics.render("hi").getData());
        assertEquals(Map.of("text/plain", "42"), ExecutorNotebookStatics.render(42).getData());
        assertEquals(Map.of("text/plain", "true"), ExecutorNotebookStatics.render(true).getData());
    }

    @Test
    public void renderCharacterThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> ExecutorNotebookStatics.render('c'));
    }

    @Test
    public void renderIgnoresPresentationHints() {
        assertEquals(
                ExecutorNotebookStatics.render("x").getData(),
                ExecutorNotebookStatics.render("x", "text/html").getData());
    }

    @Test
    public void renderNeverDelivers() {
        ExecutorNotebookStatics.render("x");
        ExecutorNotebookStatics.render("x", "text/html");
        assertTrue(delivered.isEmpty());
    }
}
