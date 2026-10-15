package org.dflib.jjava.kernel.execution;

import org.dflib.jjava.kernel.JavaKernel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class JdiExecutionControlTest {

    @Test
    public void usesRemoteExecutorForPersistentStateImportsAndOptions() {
        JavaKernel kernel = JavaKernel.builder()
                .extensionsEnabled(false)
                .remoteVMOptions(List.of("-Djjava.test.executor=remote"))
                .build();
        try {
            long executorPid = Long.parseLong(kernel.<String>evalBuilder("ProcessHandle.current().pid()").eval());
            assertNotEquals(ProcessHandle.current().pid(), executorPid);

            kernel.evalBuilder("int saved = 40;").eval();
            kernel.evalBuilder("import java.time.LocalDate;").eval();
            assertEquals("42", kernel.evalBuilder("saved + 2").eval());
            assertEquals(Long.toString(executorPid), kernel.evalBuilder("ProcessHandle.current().pid()").eval());
            assertEquals("\"remote\"", kernel.evalBuilder("System.getProperty(\"jjava.test.executor\")").eval());
        } finally {
            kernel.onShutdown(false);
        }
    }
}
