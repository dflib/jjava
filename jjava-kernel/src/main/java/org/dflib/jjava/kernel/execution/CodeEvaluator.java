package org.dflib.jjava.kernel.execution;

import jdk.jshell.EvalException;
import jdk.jshell.JShell;
import jdk.jshell.JShellException;
import jdk.jshell.Snippet;
import jdk.jshell.SnippetEvent;
import jdk.jshell.SourceCodeAnalysis;
import org.dflib.jjava.jupyter.kernel.BaseKernel;
import org.dflib.jjava.jupyter.instrumentation.EvalTimer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CodeEvaluator {
    private static final Pattern WHITESPACE_PREFIX = Pattern.compile("(?:^|\r?\n)(?<ws>\\s*).*$");
    private static final Pattern LAST_LINE = Pattern.compile("(?:^|\r?\n)(?<last>.*)$");

    private static final String NO_MAGIC_RETURN = "\"__NO_MAGIC_RETURN\"";

    private static final Method SNIPPET_CLASS_NAME_METHOD;

    static {
        try {
            SNIPPET_CLASS_NAME_METHOD = Snippet.class.getDeclaredMethod("classFullName");
            SNIPPET_CLASS_NAME_METHOD.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Unable to access jdk.jshell.Snippet.classFullName() method.", e);
        }
    }

    private static final String INDENTATION = "  ";

    private final JShell shell;
    private final JJavaExecutionControlProvider execControlProvider;
    private final String execControlID;
    private final SourceCodeAnalysis sourceAnalyzer;

    public CodeEvaluator(
            JShell shell,
            JJavaExecutionControlProvider execControlProvider,
            String execControlID) {

        this.shell = shell;
        this.execControlProvider = execControlProvider;
        this.execControlID = execControlID;
        this.sourceAnalyzer = shell.sourceCodeAnalysis();
    }

    public Object eval(String code, EvalTimer timer) {

        Object lastResult = null;
        SourceCodeAnalysis.CompletionInfo info = this.sourceAnalyzer.analyzeCompletion(code);

        while (info.completeness().isComplete()) {

            lastResult = evalSingle(info.source(), timer);
            info = sourceAnalyzer.analyzeCompletion(info.remaining());
        }

        if (info.completeness() != SourceCodeAnalysis.Completeness.EMPTY) {
            throw new IncompleteSourceException(info.remaining().trim());
        }

        return lastResult;
    }

    protected Object evalSingle(String code, EvalTimer timer) {

        JJavaExecutionControl execControl = execControlProvider.getRegisteredControlByID(execControlID);
        List<SnippetEvent> events = timer.runAndMeasureStep(() -> shell.eval(code));

        Object result = null;

        // We iterate twice to make sure throwing an early exception doesn't leak the memory
        // and we `takeResult` everything.
        for (SnippetEvent event : events) {
            if (event.status() == Snippet.Status.OVERWRITTEN) {
                // if a new snippet changed some other definition, drop the older one
                dropSnippet(event.snippet());
                continue;
            }

            String key = event.value();
            if (key == null) {
                continue;
            }

            Snippet.SubKind subKind = event.snippet().subKind();

            // Only executable snippets make their way through the machinery we have setup in the
            // JJavaExecutionControl. Declarations for example simply take their default value without
            // being executed.
            Object value = subKind.isExecutable()
                    ? execControl.takeResult(key)
                    : event.value();

            switch (subKind) {
                case VAR_VALUE_SUBKIND:
                case OTHER_EXPRESSION_SUBKIND:
                case TEMP_VAR_EXPRESSION_SUBKIND:
                    result = NO_MAGIC_RETURN.equals(value) ? null : value;
                    break;
                default:
                    result = null;
                    break;
            }
        }

        for (SnippetEvent event : events) {
            // If fresh snippet
            if (event.causeSnippet() == null) {
                JShellException e = event.exception();
                if (e != null) {

                    if (e instanceof EvalException) {
                        EvalException ee = (EvalException) e;
                        switch (ee.getExceptionClassName()) {
                            case JJavaExecutionControl.EXECUTION_TIMEOUT_NAME:
                                throw new EvaluationTimeoutException(execControl.getTimeoutDuration(), execControl.getTimeoutUnit(), code.trim());
                            case JJavaExecutionControl.EXECUTION_INTERRUPTED_NAME:
                                throw new EvaluationInterruptedException(code.trim());
                            default:
                                throw new RuntimeException(ee.getExceptionClassName() + ", " + e.getMessage(), e);
                        }
                    }

                    throw new RuntimeException(e);
                }

                // Undefined snippets are generally bad, unless we can still recover from them. E.g.,
                // "Unresolved dependencies" errors are recoverable when those dependencies are defined in the later
                // snippets.
                if (event.status() != Snippet.Status.RECOVERABLE_NOT_DEFINED && !event.status().isDefined()) {
                    throw new CompilationException(event);
                }
            }
        }

        return result;
    }

    /**
     * Try to clean up information linked to a code snippet and the snippet itself
     */
    private void dropSnippet(Snippet snippet) {
        JJavaExecutionControl execControl = execControlProvider.getRegisteredControlByID(execControlID);
        shell.drop(snippet);
        // snippet.classFullName() returns name of a wrapper class created for a snippet
        String className = snippetClassName(snippet);
        // check that this class is not used by other snippets
        if (shell.snippets()
                .map(this::snippetClassName)
                .noneMatch(className::equals)) {
            execControl.unloadClass(className);
        }
    }

    private String snippetClassName(Snippet snippet) {
        try {
            return SNIPPET_CLASS_NAME_METHOD.invoke(snippet).toString();
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
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

    public String isComplete(String code) {
        SourceCodeAnalysis.CompletionInfo info = this.sourceAnalyzer.analyzeCompletion(code);
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
                return this.computeIndentation(info.remaining());
            default:
                // For completeness, return an "I don't know" if we somehow get down here
                return BaseKernel.IS_COMPLETE_MAYBE;
        }
    }

    public void interrupt() {
        JJavaExecutionControl execControl = execControlProvider.getRegisteredControlByID(execControlID);

        if (execControl != null) {
            execControl.interrupt();
        }
    }

    public ClassLoader getClassLoader() {
        JJavaExecutionControl execControl = execControlProvider.getRegisteredControlByID(execControlID);
        return execControl != null ? execControl.getClassLoader() : null;
    }
}
