package org.dflib.jjava.kernel;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaKernelEndpointLifecycleTest {

    @Test
    void endpointSocketIsRemovedOnShutdown() throws Exception {
        Path socket = Files.createTempDirectory("uds-lifecycle").resolve("display.sock");

        JavaKernel kernel = JavaKernel.builder().extensionsEnabled(false).displayEndpointPath(socket).build();
        assertTrue(Files.exists(socket));

        kernel.onShutdown(false);
        assertFalse(Files.exists(socket));
    }

    @Test
    void buildFailureAfterEndpointCreationRemovesEndpoint() throws Exception {
        Path socket = Files.createTempDirectory("uds-lifecycle").resolve("display.sock");
        AtomicBoolean hookRan = new AtomicBoolean();

        assertThrows(IllegalStateException.class, () -> JavaKernel.builder()
                .extensionsEnabled(false)
                .displayEndpointPath(socket)
                .afterDisplayEndpointCreated(() -> {
                    hookRan.set(true);
                    throw new IllegalStateException("boom");
                })
                .build());

        assertTrue(hookRan.get());
        assertFalse(Files.exists(socket));
    }
}
