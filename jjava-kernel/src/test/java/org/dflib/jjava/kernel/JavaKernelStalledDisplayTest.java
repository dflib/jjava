package org.dflib.jjava.kernel;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A manager that accepts a display request but never answers. The executor must fail within the response bound and
 * report the outcome as uncertain (never retried), and the kernel must still evaluate afterwards.
 */
class JavaKernelStalledDisplayTest {

    private static final String IMPORT = "import static org.dflib.jjava.jupyter.kernel.ExecutorNotebookStatics.*;\n";
    // executor RESPONSE bound is 10 s; allow a scheduling tolerance of 5 s
    private static final long BOUND_MILLIS = 15_000;

    @Test
    void stalledManagerFailsUncertainWithinBoundAndKernelStaysUsable() {
        CountDownLatch release = new CountDownLatch(1);
        JavaKernel kernel = JavaKernel.builder()
                .extensionsEnabled(false)
                .displayHandler(request -> awaitQuietly(release))
                .build();
        try {
            var stalledDisplay = kernel.evalBuilder(IMPORT + "display(\"stalled\")").resolveMagics();
            long start = System.nanoTime();
            RuntimeException failure = assertThrows(RuntimeException.class, stalledDisplay::eval);
            long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

            assertTrue(elapsedMillis < BOUND_MILLIS, "took " + elapsedMillis + " ms");
            assertTrue(chainContains(failure, "Failed to send display data"), String.valueOf(failure));

            release.countDown();
            assertEquals("42", kernel.evalBuilder("40 + 2").resolveMagics().eval());
        } finally {
            release.countDown();
            kernel.onShutdown(false);
        }
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static boolean chainContains(Throwable t, String fragment) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (String.valueOf(c.getMessage()).contains(fragment)) {
                return true;
            }
        }
        return false;
    }
}
