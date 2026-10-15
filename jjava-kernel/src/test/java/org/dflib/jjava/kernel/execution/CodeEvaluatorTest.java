package org.dflib.jjava.kernel.execution;

import jdk.jshell.EvalException;
import jdk.jshell.JShell;
import jdk.jshell.execution.JdiExecutionControlProvider;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CodeEvaluatorTest {

    @Test
    public void evaluatesLastExpressionAndLeavesDeclarationsWithoutResults() {
        try (JShell shell = newShell(); CodeEvaluator evaluator = new CodeEvaluator(-1, TimeUnit.SECONDS)) {
            assertEquals("42", evaluator.eval(shell, "int base = 40;\nbase + 2").text());
            assertEquals(EvaluationResult.NONE, evaluator.eval(shell, "int declared = 1;"));
            assertEquals("\"__NO_MAGIC_RETURN\"", evaluator.eval(shell, "\"__NO_MAGIC_RETURN\"").text());
        }
    }

    @Test
    public void userExceptionDoesNotPoisonSubsequentEvaluation() {
        try (JShell shell = newShell(); CodeEvaluator evaluator = new CodeEvaluator(-1, TimeUnit.SECONDS)) {
            RuntimeException failure = assertThrows(
                    RuntimeException.class,
                    () -> evaluator.eval(shell, "throw new IllegalStateException(\"expected\");"));

            assertInstanceOf(EvalException.class, failure.getCause());
            assertEquals("42", evaluator.eval(shell, "40 + 2").text());
        }
    }

    @Test
    public void timeoutDoesNotPoisonSubsequentEvaluation() {
        try (JShell shell = newShell(); CodeEvaluator evaluator = new CodeEvaluator(5, TimeUnit.SECONDS)) {
            assertEquals("2", evaluator.eval(shell, "1 + 1").text());
            assertThrows(
                    EvaluationTimeoutException.class,
                    () -> evaluator.eval(shell, "try { Thread.sleep(30000L); } catch (InterruptedException ignored) {}"));

            assertEquals("42", evaluator.eval(shell, "40 + 2").text());
        }
    }

    private JShell newShell() {
        return JShell.builder()
                .executionEngine(new JdiExecutionControlProvider(), Map.of())
                .build();
    }
}
