package org.dflib.jjava.kernel;

import org.dflib.jjava.jupyter.kernel.CapturingShellEnvironment;
import org.dflib.jjava.jupyter.messages.publish.PublishDisplayData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Non-portable payloads are rejected in the executor before any delivery: nothing is published, and the next cell
 * still evaluates. The stalled-peer case is not covered yet (needs a blocking endpoint handler seam).
 */
class JavaKernelDisplayFailureTest {

    private static final String IMPORT = "import static org.dflib.jjava.jupyter.kernel.ExecutorNotebookStatics.*;\n";

    @Test
    void nonPortablePayloadsAreRejectedWithoutPublishingAndKernelContinues() {
        JavaKernel kernel = JavaKernel.builder().extensionsEnabled(false).build();
        try {
            CapturingShellEnvironment env = CapturingShellEnvironment.attach(kernel.getIO());

            // a cyclic list and a non-finite double, both built in the child
            // Rejected by renderPortable before the protocol check: a raw collection is not a supported display value.
            assertFailsWithMessage(kernel,
                    "java.util.List<Object> cyclic = new java.util.ArrayList<>(); cyclic.add(cyclic); display(cyclic);",
                    "Non-portable display value");
            assertFailsWithMessage(kernel,
                    "display(java.util.List.of(Double.NaN));",
                    "Non-portable display value");

            assertTrue(env.published.stream().noneMatch(m -> m.getContent() instanceof PublishDisplayData),
                    "a rejected payload was published");

            assertEquals("42", kernel.evalBuilder("40 + 2").resolveMagics().eval());
        } finally {
            kernel.onShutdown(false);
        }
    }

    // Pinned current behavior: a bare scalar is rendered to text before the protocol's non-finite check, which applies to containers.
    @Test
    void bareNonFiniteScalarIsPublishedAsTextNaN() {
        JavaKernel kernel = JavaKernel.builder().extensionsEnabled(false).build();
        try {
            CapturingShellEnvironment env = CapturingShellEnvironment.attach(kernel.getIO());

            kernel.evalBuilder(IMPORT + "display(Double.NaN);").resolveMagics().eval();

            PublishDisplayData published = assertInstanceOf(PublishDisplayData.class, env.published.get(0).getContent());
            assertEquals("NaN", published.getData().get("text/plain"));
        } finally {
            kernel.onShutdown(false);
        }
    }

    private static void assertFailsWithMessage(JavaKernel kernel, String code, String fragment) {
        var evaluation = kernel.evalBuilder(IMPORT + code).resolveMagics();
        RuntimeException failure = org.junit.jupiter.api.Assertions.assertThrows(
                RuntimeException.class,
                evaluation::eval);
        assertTrue(String.valueOf(failure.getMessage()).contains(fragment)
                        || causeChainContains(failure, fragment),
                "expected message containing '" + fragment + "' but was: " + failure);
    }

    private static boolean causeChainContains(Throwable t, String fragment) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (String.valueOf(c.getMessage()).contains(fragment)) {
                return true;
            }
        }
        return false;
    }
}
