package org.dflib.jjava.jupyter.kernel;

import org.dflib.jjava.jupyter.channels.ShellReplyEnvironment;
import org.dflib.jjava.jupyter.kernel.comm.CommManager;
import org.dflib.jjava.jupyter.kernel.display.DisplayData;
import org.dflib.jjava.jupyter.kernel.display.Renderer;
import org.dflib.jjava.jupyter.kernel.display.protocol.DisplayDelivery;
import org.dflib.jjava.jupyter.kernel.display.protocol.DisplayDeliveryBootstrap;
import org.dflib.jjava.jupyter.kernel.display.protocol.DisplayRequest;
import org.dflib.jjava.jupyter.kernel.magic.MagicTranspiler;
import org.dflib.jjava.jupyter.kernel.magic.MagicsRegistry;
import org.dflib.jjava.jupyter.kernel.magic.MagicsResolver;
import org.dflib.jjava.jupyter.kernel.util.StringStyler;
import org.dflib.jjava.jupyter.messages.Message;
import org.dflib.jjava.jupyter.messages.publish.PublishDisplayData;
import org.dflib.jjava.jupyter.messages.publish.PublishUpdateDisplayData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Manager-side helpers: display and update go to the running kernel directly and never through DisplayDelivery.
 */
public class BaseNotebookStaticsDisplayTest {

    private final List<DisplayRequest> delivered = new ArrayList<>();
    private BaseKernel previousKernel;
    private DisplayDelivery previousDelivery;
    private CapturingEnvironment env;

    @BeforeEach
    public void setUp() {
        previousKernel = BaseKernel.notebookKernel;
        previousDelivery = DisplayDeliveryBootstrap.install(delivered::add);

        JupyterIO io = new JupyterIO(StandardCharsets.UTF_8);
        env = new CapturingEnvironment();
        io.setEnv(env);
        BaseKernel.notebookKernel = new StubKernel(io);
    }

    @AfterEach
    void tearDown() {
        BaseKernel.notebookKernel = previousKernel;
        DisplayDeliveryBootstrap.install(previousDelivery);
    }

    @Test
    void displayReturnsTheIdOfThePublishedBundle() {
        DisplayData data = new DisplayData("x");
        data.setDisplayId("display-id");

        String id = BaseNotebookStatics.display(data);

        PublishDisplayData published = assertInstanceOf(PublishDisplayData.class, env.published.get(0).getContent());
        assertEquals(published.getDisplayId(), id);
        assertTrue(delivered.isEmpty());
    }

    @Test
    public void updateDisplayUsesKernelWithTheTargetId() {
        BaseNotebookStatics.updateDisplay("requested-id", new DisplayData("updated"));

        PublishUpdateDisplayData update = assertInstanceOf(
                PublishUpdateDisplayData.class,
                env.published.get(0).getContent());
        assertEquals("requested-id", update.getDisplayId());
        assertTrue(delivered.isEmpty());
    }

    @Test
    public void managerWithoutKernelFailsWithNullPointerAndNeverDelivers() {
        BaseKernel.notebookKernel = null;

        assertThrows(NullPointerException.class, () -> BaseNotebookStatics.display("text"));
        assertTrue(delivered.isEmpty());
    }

    private static class CapturingEnvironment extends ShellReplyEnvironment {
        final List<Message<?>> published = new ArrayList<>();

        CapturingEnvironment() {
            super(null, null, null, null);
        }

        @Override
        public void publish(Message<?> message) {
            published.add(message);
        }
    }

    private static class StubKernel extends BaseKernel {
        StubKernel(JupyterIO io) {
            super(
                    "test", "0",
                    new LanguageInfo.Builder("Java").build(),
                    List.of(), null, io, new CommManager(), new Renderer(),
                    new MagicsResolver("%", "%%", new MagicTranspiler()),
                    new MagicsRegistry(), false, new StringStyler.Builder().build());
        }

        @Override
        protected Object doEval(String source) {
            throw new UnsupportedOperationException();
        }

        @Override
        public DisplayData inspect(String code, int at, boolean extraDetail) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ReplacementOptions complete(String code, int at) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String isComplete(String code) {
            throw new UnsupportedOperationException();
        }
    }
}
