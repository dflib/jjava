package org.dflib.jjava.distro;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class KernelStartupIT extends ContainerizedKernelCase {

    @Test
    public void directStartupUsesOneExecutor() throws Exception {
        KernelRun run = executeInKernel(
                "ProcessHandle.current().pid()",
                "ProcessHandle.current().pid()",
                "ProcessHandle.current().parent().get().info().commandLine().orElse(\"\")"
        ).assertNoErrors();

        assertEquals(run.cell(1).result(), run.cell(2).result());
        assertTrue(run.cell(3).result().contains("jjava.jar"), run.cell(3).result());
    }

    @Test
    public void executorTerminationMarksSessionLost() throws Exception {
        KernelRun run = executeInKernel(
                Map.of(Env.JJAVA_TIMEOUT, "3000"),
                "Runtime.getRuntime().halt(17);",
                "1 + 1"
        );

        run.cell(1).assertError("Executor process terminated", "Restart the kernel");
        run.cell(2).assertError("Executor process terminated", "Restart the kernel");
    }

    @Test
    public void displayAfterExecutorTerminationReportsExecutorUnavailable() throws Exception {
        KernelRun run = executeInKernel(
                Map.of(Env.JJAVA_TIMEOUT, "3000"),
                "Runtime.getRuntime().halt(17);",
                "display(\"after termination\")"
        );

        run.cell(1).assertError("Executor process terminated", "Restart the kernel");
        run.cell(2).assertError("Executor process terminated", "Restart the kernel");
    }

    @Test
    public void executorOutOfMemoryMarksSessionLost() throws Exception {
        KernelRun run = executeInKernel(
                Map.of(Env.JJAVA_JVM_OPTS, "-Xmx128m"),
                "new byte[Integer.MAX_VALUE]",
                "1 + 1"
        );

        run.cell(1).assertError("Executor process terminated", "Restart the kernel");
        run.cell(2).assertError("Executor process terminated", "Restart the kernel");
    }

    @Test
    public void notebookInitializerImportsAndHelpers() throws Exception {
        KernelRun run = executeInKernel(
                "LocalDate.of(2025, 1, 2).getYear()",
                "printf(\"helper-%s\", \"ready\");"
        ).assertNoErrors();

        assertEquals("2025", run.cell(1).result());
        assertEquals("helper-ready", run.cell(2).stdout());
    }

    @Test
    public void startUp_scriptRequiresClasspath() throws Exception {
        Map<String, String> env = Map.of(
                Env.JJAVA_CLASSPATH, TEST_CLASSPATH,
                Env.JJAVA_STARTUP_SCRIPT, "var obj = new org.dflib.jjava.Dummy()"
        );
        KernelRun run = executeInKernel(env, "\"hash = \" + obj.hashCode()").assertNoErrors();

        String result = run.cell(1).result();
        assertTrue(result.matches("\\\"hash = -?\\d+\\\""), () -> "Unexpected result: " + result);
    }
}
