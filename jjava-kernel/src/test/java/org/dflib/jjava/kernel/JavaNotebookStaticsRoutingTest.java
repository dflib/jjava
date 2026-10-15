package org.dflib.jjava.kernel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JavaNotebookStaticsRoutingTest {

    @Test
    void executorKernelAlwaysFailsWithoutReadingKernel() {
        assertEquals(
                "JavaKernel.kernel() cannot expose the managing JVM from executor code",
                assertThrows(UnsupportedOperationException.class, ExecutorJavaNotebookStatics::kernel).getMessage());
    }

    @SuppressWarnings("removal")
    @Test
    void executorGetKernelInstanceAlwaysFails() {
        assertThrows(UnsupportedOperationException.class, ExecutorJavaNotebookStatics::getKernelInstance);
    }

    @Test
    void managerKernelWithoutRunningKernelFailsWithNullPointerFromKernelAccess() {
        // Accepted outcome (spec Story 4 #8): no lifecycle message; the kernel access itself fails.
        assertThrows(NullPointerException.class, JavaNotebookStatics::kernel);
    }
}
