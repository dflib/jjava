package org.dflib.jjava.jupyter.kernel.display.common;

import org.dflib.jjava.jupyter.kernel.display.DisplayData;
import org.dflib.jjava.jupyter.kernel.display.Renderer;
import org.dflib.jjava.jupyter.kernel.display.mime.MIMEType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JavaFxTest {

    /** A stand-in for a JavaFX node so the by-name registration dispatches without JavaFX on the test classpath. */
    static class FakeNode {
    }

    @Test
    public void degradesGracefullyWhenJavaFxUnavailable() {
        Renderer renderer = new Renderer();
        // Exercise the JavaFX renderer against the stand-in's name (no JavaFX on the classpath, so it must not throw).
        renderer.createRegistration(FakeNode.class.getName())
                .preferring(MIMEType.IMAGE_PNG)
                .register(JavaFx::renderNode);

        DisplayData data = renderer.render(new FakeNode());

        Object text = data.getData(MIMEType.TEXT_PLAIN);
        assertNotNull(text, "a text/plain fallback should be present");
        assertTrue(text.toString().contains("Could not render JavaFX"),
                "fallback should explain the JavaFX node could not be rendered");
    }
}
