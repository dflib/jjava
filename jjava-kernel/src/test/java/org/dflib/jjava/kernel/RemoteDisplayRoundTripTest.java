package org.dflib.jjava.kernel;

import org.dflib.jjava.jupyter.kernel.CapturingShellEnvironment;
import org.dflib.jjava.jupyter.messages.publish.PublishDisplayData;
import org.dflib.jjava.jupyter.messages.publish.PublishUpdateDisplayData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * A display and an update made by user code in the executor JVM are published by the manager.
 */
public class RemoteDisplayRoundTripTest {

    private static final String IMPORT = "import static org.dflib.jjava.jupyter.kernel.ExecutorNotebookStatics.*;\n";

    @Test
    public void executorDisplayAndUpdatePublishOnManager() {
        JavaKernel kernel = JavaKernel.builder().extensionsEnabled(false).build();
        try {
            CapturingShellEnvironment env = CapturingShellEnvironment.attach(kernel.getIO());

            // eval() returns the JShell text of the String result, so the id arrives with its quotes
            String id = ((String) kernel.evalBuilder(IMPORT + "display(\"hello\")").resolveMagics().eval()).replace("\"", "");

            PublishDisplayData display = assertInstanceOf(PublishDisplayData.class, env.published.get(0).getContent());
            assertEquals(id, display.getDisplayId());
            assertEquals("hello", display.getData().get("text/plain"));

            kernel.evalBuilder(IMPORT + "updateDisplay(\"" + id + "\", \"updated\")").resolveMagics().eval();

            PublishUpdateDisplayData update = assertInstanceOf(
                    PublishUpdateDisplayData.class,
                    env.published.get(1).getContent());
            assertEquals(id, update.getDisplayId());
        } finally {
            kernel.onShutdown(false);
        }
    }
}
