package org.dflib.jjava.distro;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class KernelExecutionIT extends ContainerizedKernelCase {

    /**
     * The variable and the later import must be in the same cell. Split across cells, this
     * scenario passes even with the bug present.
     */
    @Test
    public void variableSurvivesLaterImports() throws Exception {
        KernelRun run = executeInKernel(
                "%maven com.fasterxml.jackson.core:jackson-databind:2.21.2",
                String.join("\n",
                        "import com.fasterxml.jackson.databind.*;",
                        "var om = new ObjectMapper().findAndRegisterModules();",
                        "import com.fasterxml.jackson.databind.node.*; import com.fasterxml.jackson.databind.type.*;",
                        "om.getClass().getName()"
                )
        ).assertNoErrors();

        assertEquals("\"com.fasterxml.jackson.databind.ObjectMapper\"", run.cell(2).result());
    }

    @Test
    public void variableRedeclarationUsesLatestValue() throws Exception {
        KernelRun run = executeInKernel(
                "int v = 1;",
                "v",
                "float v = 2.4f;",
                "v"
        ).assertNoErrors();

        assertEquals("1", run.cell(2).result());
        assertEquals("2.4", run.cell(4).result());
    }

    @Test
    public void variableRedeclarationInSameCell() throws Exception {
        KernelRun run = executeInKernel(
                "var v = \"a\";\nv",
                "var v = \"b\";\nv"
        ).assertNoErrors();

        assertEquals("\"a\"", run.cell(1).result());
        assertEquals("\"b\"", run.cell(2).result());
    }

    @Test
    public void textPlainStringsAndCustomObjects() throws Exception {
        KernelRun run = executeInKernel(
                "\"quoted\"",
                "\"line1\\nline2\"",
                "class DisplayValue { public String toString() { return \"custom-value\"; } }",
                "new DisplayValue()"
        ).assertNoErrors();

        assertEquals("\"quoted\"", run.cell(1).result());
        assertEquals("\"line1\\nline2\"", run.cell(2).result());
        assertEquals("custom-value", run.cell(4).result());
    }

    @Test
    public void methodRedefinitionKeepsSavedValues() throws Exception {
        KernelRun run = executeInKernel(
                "int calculate() { return 1; }",
                "int saved = calculate();",
                "int calculate() { return 2; }",
                "saved",
                "calculate()"
        ).assertNoErrors();

        assertEquals("1", run.cell(4).result());
        assertEquals("2", run.cell(5).result());
    }

    @Test
    public void classRedefinitionKeepsSavedValuesAndInstances() throws Exception {
        KernelRun run = executeInKernel(
                "class Reloadable { int value() { return 1; } }",
                "Reloadable instance = new Reloadable();",
                "int saved = instance.value();",
                "class Reloadable { int value() { return 2; } }",
                "saved",
                "instance.value()",
                "new Reloadable().value()"
        ).assertNoErrors();

        assertEquals("1", run.cell(5).result());
        assertEquals("2", run.cell(6).result());
        assertEquals("2", run.cell(7).result());
    }

    @Test
    public void interruptStopsBusyLoopAndKeepsExecutor() throws Exception {
        KernelRun run = executeInKernel(
                "@@INTERRUPT_AFTER_MS=500@@while (true) {}",
                "1 + 1"
        );

        run.cell(1).assertError("Evaluation interrupted.");
        assertTrue(Long.parseLong(run.cell(1).field("interruptLatencyMs")) <= 2000, "interrupt was not handled within two seconds");
        assertEquals("2", run.cell(2).result());
    }

    @Test
    public void executorStreamsReachNotebook() throws Exception {
        KernelRun run = executeInKernel(
                "System.out.print(\"stdout-1\"); System.err.print(\"stderr-1\");",
                "System.out.print(\"stdout-2\"); System.err.print(\"stderr-2\");"
        ).assertNoErrors();

        assertEquals("stdout-1", run.cell(1).stdout());
        assertEquals("stderr-1", run.cell(1).stderr());
        assertEquals("stdout-2", run.cell(2).stdout());
        assertEquals("stderr-2", run.cell(2).stderr());
    }

    @Test
    public void userExceptionDoesNotLoseExecutorState() throws Exception {
        KernelRun run = executeInKernel(
                "int saved = 7;",
                "throw new IllegalStateException(\"outer\", new IllegalArgumentException(\"inner\"));",
                "saved + 1"
        );

        run.cell(2).assertError("IllegalStateException", "outer", "IllegalArgumentException", "inner", "at .(#");
        assertEquals("8", run.cell(3).result());
    }
}
