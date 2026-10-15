package org.dflib.jjava.kernel.execution;

import jdk.jshell.EvalException;
import jdk.jshell.JShell;
import jdk.jshell.JShellException;
import jdk.jshell.Snippet;
import jdk.jshell.SnippetEvent;
import jdk.jshell.SourceCodeAnalysis;
import org.dflib.jjava.jupyter.kernel.BaseKernel;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CodeEvaluator implements AutoCloseable {
    private static final Pattern WHITESPACE_PREFIX = Pattern.compile("(?:^|\r?\n)(?<ws>\\s*).*$");
    private static final Pattern LAST_LINE = Pattern.compile("(?:^|\r?\n)(?<last>.*)$");

    private static final String INDENTATION = "  ";
    private static final String EXECUTION_OUT_OF_MEMORY_NAME = "java.lang.OutOfMemoryError";

    private final long timeoutDuration;
    private final TimeUnit timeoutUnit;
    private final AtomicBoolean interrupted = new AtomicBoolean();
    private final ExecutorService evaluationExecutor;
    private volatile ExecutorTerminationException executorFailure;

    public CodeEvaluator(long timeoutDuration, TimeUnit timeoutUnit) {
        this.timeoutDuration = timeoutDuration;
        this.timeoutUnit = timeoutUnit;
        this.evaluationExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "jjava-jshell-evaluation");
            thread.setDaemon(true);
            return thread;
        });
    }

    public EvaluationResult eval(JShell shell, String code) {
        checkExecutorFailure();
        interrupted.set(false);
        long timeoutNanos = timeoutDuration > 0 ? timeoutUnit.toNanos(timeoutDuration) : -1;
        long started = System.nanoTime();
        SourceCodeAnalysis sca = shell.sourceCodeAnalysis();

        EvaluationResult lastResult = EvaluationResult.NONE;
        SourceCodeAnalysis.CompletionInfo info = sca.analyzeCompletion(code);

        while (info.completeness().isComplete()) {
            long remainingNanos = timeoutNanos < 0 ? -1 : timeoutNanos - (System.nanoTime() - started);
            lastResult = evalSingle(shell, info.source(), remainingNanos);
            info = sca.analyzeCompletion(info.remaining());
        }

        if (info.completeness() != SourceCodeAnalysis.Completeness.EMPTY) {
            throw new IncompleteSourceException(info.remaining().trim());
        }

        return lastResult;
    }

    protected EvaluationResult evalSingle(JShell shell, String code, long timeoutNanos) {
        List<SnippetEvent> events = evaluate(shell, code, timeoutNanos);
        String result = null;

        for (SnippetEvent event : events) {
            if (event.causeSnippet() != null) {
                continue;
            }

            JShellException exception = event.exception();
            if (exception != null) {
                if (exception instanceof EvalException) {
                    EvalException evalException = (EvalException) exception;
                    if (EXECUTION_OUT_OF_MEMORY_NAME.equals(evalException.getExceptionClassName())) {
                        throw executorTerminated(shell, exception);
                    }
                    throw new RuntimeException(
                            evalException.getExceptionClassName() + ", " + exception.getMessage(),
                            exception);
                }
                throw new RuntimeException(exception);
            }

            if (event.status() != Snippet.Status.RECOVERABLE_NOT_DEFINED && !event.status().isDefined()) {
                throw new CompilationException(event);
            }

            String value = event.value();
            if (value == null) {
                continue;
            }

            switch (event.snippet().subKind()) {
                case VAR_VALUE_SUBKIND:
                case OTHER_EXPRESSION_SUBKIND:
                case TEMP_VAR_EXPRESSION_SUBKIND:
                    result = value;
                    break;
                default:
                    result = null;
                    break;
            }
        }

        return new EvaluationResult(result);
    }

    private List<SnippetEvent> evaluate(JShell shell, String code, long timeoutNanos) {
        if (timeoutDuration > 0 && timeoutNanos <= 0) {
            throw new EvaluationTimeoutException(timeoutDuration, timeoutUnit, code.trim());
        }

        Future<List<SnippetEvent>> evaluation = evaluationExecutor.submit(() -> shell.eval(code.strip()));

        List<SnippetEvent> events;
        try {
            events = timeoutNanos > 0 ? evaluation.get(timeoutNanos, TimeUnit.NANOSECONDS) : evaluation.get();
        } catch (TimeoutException e) {
            checkExecutorFailure();
            shell.stop();
            evaluation.cancel(true);
            throw new EvaluationTimeoutException(timeoutDuration, timeoutUnit, code.trim());
        } catch (InterruptedException e) {
            shell.stop();
            evaluation.cancel(true);
            Thread.currentThread().interrupt();
            throw new EvaluationInterruptedException(code.trim());
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            checkExecutorFailure();
            if (interrupted.getAndSet(false)) {
                throw new EvaluationInterruptedException(code.trim());
            }
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            if (cause instanceof Error) throw (Error) cause;
            throw new RuntimeException(cause);
        }

        if (interrupted.getAndSet(false)) {
            throw new EvaluationInterruptedException(code.trim());
        }
        checkExecutorFailure();
        return events;
    }

    public void checkExecutorFailure() {
        if (executorFailure != null) throw executorFailure;
    }

    public synchronized void markExecutorTerminated() {
        if (executorFailure == null) {
            executorFailure = new ExecutorTerminationException(
                    "Executor process terminated; session state is lost. Restart the kernel.", null);
        }
    }

    private synchronized ExecutorTerminationException executorTerminated(JShell shell, Throwable cause) {
        if (executorFailure == null) {
            executorFailure = new ExecutorTerminationException(
                    "Executor process terminated; session state is lost. Restart the kernel.", cause);
            shell.close();
        }
        return executorFailure;
    }

    private String computeIndentation(String partialStatement) {
        // Find the indentation of the last line
        Matcher m = WHITESPACE_PREFIX.matcher(partialStatement);
        String currentIndentation = m.find() ? m.group("ws") : "";

        m = LAST_LINE.matcher(partialStatement);
        if (!m.find())
            throw new Error("Pattern broken. Every string should have a last line.");

        // If a brace or paren was opened on the last line and not closed, indent some more.
        String lastLine = m.group("last");
        int newlyOpenedBraces = -1;
        int newlyOpenedParens = -1;
        for (int i = 0; i < lastLine.length(); i++) {
            switch (lastLine.charAt(i)) {
                case '}':
                    // Ignore closing if one has not been opened on this line yet
                    if (newlyOpenedBraces == -1) continue;
                    // Otherwise close an opened one from this line
                    newlyOpenedBraces--;
                    break;
                case ')':
                    // Same as for braces, but with the parens
                    if (newlyOpenedParens == -1) continue;
                    newlyOpenedParens--;
                    break;
                case '{':
                    // A brace was opened on this line!
                    // If the first then get out og the -1 special case with an extra addition
                    if (newlyOpenedBraces == -1) newlyOpenedBraces++;
                    newlyOpenedBraces++;
                    break;
                case '(':
                    if (newlyOpenedParens == -1) newlyOpenedParens++;
                    newlyOpenedParens++;
                    break;
            }
        }

        return newlyOpenedBraces > 0 || newlyOpenedParens > 0
                ? currentIndentation + INDENTATION
                : currentIndentation;
    }

    public String isComplete(SourceCodeAnalysis sourceAnalyzer, String code) {
        SourceCodeAnalysis.CompletionInfo info = sourceAnalyzer.analyzeCompletion(code);
        while (info.completeness().isComplete()) {
            info = sourceAnalyzer.analyzeCompletion(info.remaining());
        }

        switch (info.completeness()) {
            case UNKNOWN:
                // Unknown means "bad code" and the only way to see if is complete is to execute it.
                return BaseKernel.IS_COMPLETE_BAD;
            case COMPLETE:
            case COMPLETE_WITH_SEMI:
            case EMPTY:
                return BaseKernel.IS_COMPLETE_YES;
            case CONSIDERED_INCOMPLETE:
            case DEFINITELY_INCOMPLETE:
                // Compute the indent of the last line and match it
                return computeIndentation(info.remaining());
            default:
                // For completeness, return an "I don't know" if we somehow get down here
                return BaseKernel.IS_COMPLETE_MAYBE;
        }
    }

    public void interrupt(JShell shell) {
        interrupted.set(true);
        shell.stop();
    }

    @Override
    public void close() {
        evaluationExecutor.shutdownNow();
    }
}
