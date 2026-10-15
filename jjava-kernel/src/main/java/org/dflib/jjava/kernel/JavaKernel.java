package org.dflib.jjava.kernel;

import jdk.jshell.DeclarationSnippet;
import jdk.jshell.EvalException;
import jdk.jshell.JShell;
import jdk.jshell.Snippet;
import jdk.jshell.SnippetEvent;
import jdk.jshell.SourceCodeAnalysis;
import jdk.jshell.UnresolvedReferenceException;
import org.dflib.jjava.jupyter.kernel.BaseKernel;
import org.dflib.jjava.jupyter.kernel.EvalBuilder;
import org.dflib.jjava.jupyter.kernel.HelpLink;
import org.dflib.jjava.jupyter.kernel.JupyterIO;
import org.dflib.jjava.jupyter.kernel.LanguageInfo;
import org.dflib.jjava.jupyter.kernel.ReplacementOptions;
import org.dflib.jjava.jupyter.kernel.comm.CommManager;
import org.dflib.jjava.jupyter.kernel.display.DisplayData;
import org.dflib.jjava.jupyter.kernel.display.Renderer;
import org.dflib.jjava.jupyter.kernel.history.HistoryManager;
import org.dflib.jjava.jupyter.kernel.magic.MagicTranspiler;
import org.dflib.jjava.jupyter.kernel.magic.ParsedCellMagic;
import org.dflib.jjava.jupyter.kernel.magic.ParsedLineMagic;
import org.dflib.jjava.jupyter.kernel.magic.UndefinedMagicException;
import org.dflib.jjava.jupyter.kernel.magic.MagicsRegistry;
import org.dflib.jjava.jupyter.kernel.magic.MagicsResolver;
import org.dflib.jjava.jupyter.kernel.util.CharPredicate;
import org.dflib.jjava.jupyter.kernel.util.PathsHandler;
import org.dflib.jjava.jupyter.kernel.util.StringStyler;
import org.dflib.jjava.kernel.execution.CodeEvaluator;
import org.dflib.jjava.kernel.execution.CompilationException;
import org.dflib.jjava.kernel.execution.EvaluationInterruptedException;
import org.dflib.jjava.kernel.execution.ExecutorTerminationException;
import org.dflib.jjava.jupyter.kernel.display.protocol.DisplayRequest;
import org.dflib.jjava.jupyter.kernel.display.protocol.DisplayRequestHandler;
import org.dflib.jjava.jupyter.kernel.display.protocol.uds.UnixDomainDisplayEndpoint;
import org.dflib.jjava.kernel.execution.EvaluationResult;
import org.dflib.jjava.kernel.execution.EvaluationTimeoutException;
import org.dflib.jjava.kernel.execution.IncompleteSourceException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A Jupyter kernel for Java programming language.
 */
public class JavaKernel extends BaseKernel {

    private static final CharPredicate IDENTIFIER_CHAR = CharPredicate.builder()
            .inRange('a', 'z')
            .inRange('A', 'Z')
            .inRange('0', '9')
            .match('_')
            .build();
    private static final CharPredicate WS = CharPredicate.anyOf(" \t\n\r");

    // Match % or %% at start of line, followed by an identifier, and cursor is at end of that identifier
    private static final Pattern MAGIC_PATTERN = Pattern.compile("^(%{1,2})([\\w\\-]*)$");

    /**
     * Starts a builder for a new JJavaKernel.
     */
    public static Builder builder() {
        return new Builder();
    }

    private final JShell jShell;
    private final CodeEvaluator evaluator;
    private volatile boolean shuttingDown;
    private UnixDomainDisplayEndpoint displayEndpoint;

    protected JavaKernel(
            String name,
            String version,
            LanguageInfo languageInfo,
            List<HelpLink> helpLinks,
            HistoryManager historyManager,
            JupyterIO io,
            CommManager commManager,
            Renderer renderer,
            MagicsResolver magicsResolver,
            MagicsRegistry magicsRegistry,
            boolean extensionsEnabled,
            StringStyler errorStyler,
            JShell jShell,
            CodeEvaluator evaluator) {

        super(
                name,
                version,
                languageInfo,
                helpLinks,
                historyManager,
                io,
                commManager,
                renderer,
                magicsResolver,
                magicsRegistry,
                extensionsEnabled,
                errorStyler);

        this.jShell = jShell;
        this.evaluator = evaluator;
        jShell.onShutdown(ignored -> {
            if (!shuttingDown) evaluator.markExecutorTerminated();
        });
    }

    /**
     * Adds a collection of paths to the JShell classpath and triggers extension loading for the extra classpath.
     *
     * @param classpath one or more filesystem paths separated by {@link java.io.File#pathSeparator}.
     */
    public void addToClasspath(String classpath) {
        if (classpath == null || classpath.isBlank()) {
            return;
        }

        String classpathResolved = PathsHandler.joinPaths(PathsHandler.splitAndResolveGlobs(classpath));
        jShell.addToClasspath(classpathResolved);
        if (extensionsEnabled) {
            installExtensions(classpathResolved);
        }
    }

    @Override
    protected List<String> formatError(Throwable e) {
        if (e instanceof CompilationException) {
            return formatCompilationException((CompilationException) e);
        } else if (e instanceof IncompleteSourceException) {
            return formatIncompleteSourceException((IncompleteSourceException) e);
        } else if (e instanceof EvalException) {
            return formatEvalException((EvalException) e);
        } else if (e instanceof UnresolvedReferenceException) {
            return formatUnresolvedReferenceException(((UnresolvedReferenceException) e));
        } else if (e instanceof EvaluationTimeoutException) {
            return formatEvaluationTimeoutException((EvaluationTimeoutException) e);
        } else if (e instanceof EvaluationInterruptedException) {
            return formatEvaluationInterruptedException((EvaluationInterruptedException) e);
        } else if (e instanceof ExecutorTerminationException) {
            return List.of(errorStyler.secondary(e.getMessage()));
        } else if (e instanceof RuntimeException && e.getCause() instanceof EvalException) {
            return formatEvalException((EvalException) e.getCause());
        } else {
            return new ArrayList<>(super.formatError(e));
        }
    }

    private List<String> formatCompilationException(CompilationException e) {
        List<String> fmt = new ArrayList<>();
        SnippetEvent event = e.getBadSnippetCompilation();
        Snippet snippet = event.snippet();
        jShell.diagnostics(snippet)
                .forEach(d -> {
                    // If has line information related, highlight that span
                    if (d.getStartPosition() >= 0 && d.getEndPosition() >= 0)
                        fmt.addAll(this.errorStyler.highlightSubstringLines(snippet.source(),
                                (int) d.getStartPosition(), (int) d.getEndPosition()));
                    else
                        fmt.addAll(this.errorStyler.primaryLines(snippet.source()));

                    // Add the error message
                    for (String line : StringStyler.splitLines(d.getMessage(null))) {
                        // Skip the information about the location of the error as it is highlighted instead
                        if (!line.trim().startsWith("location:"))
                            fmt.add(this.errorStyler.secondary(line));
                    }

                    fmt.add(""); // Add a blank line
                });
        if (snippet instanceof DeclarationSnippet) {
            List<String> unresolvedDependencies = jShell.unresolvedDependencies((DeclarationSnippet) snippet)
                    .collect(Collectors.toList());
            if (!unresolvedDependencies.isEmpty()) {
                fmt.addAll(this.errorStyler.primaryLines(snippet.source()));
                fmt.add(this.errorStyler.secondary("Unresolved dependencies:"));
                unresolvedDependencies.forEach(dep -> fmt.add(this.errorStyler.secondary("   - " + dep)));
            }
        }

        return fmt;
    }

    private List<String> formatIncompleteSourceException(IncompleteSourceException e) {
        List<String> fmt = new ArrayList<>();

        String source = e.getSource();
        fmt.add(errorStyler.secondary("Incomplete input:"));
        fmt.addAll(errorStyler.primaryLines(source));

        return fmt;
    }

    private List<String> formatEvalException(EvalException e) {
        List<String> fmt = new ArrayList<>();

        String actualExceptionName = e.getExceptionClassName();
        fmt.add(errorStyler.secondary(actualExceptionName + ": " + e.getMessage()));
        for (StackTraceElement element : e.getStackTrace()) {
            fmt.add(errorStyler.secondary("\tat " + element));
        }

        formatEvalExceptionCause((EvalException) e.getCause(), fmt);
        return fmt;
    }

    private void formatEvalExceptionCause(EvalException e, List<String> fmt) {
        if (e == null) {
            return;
        }

        String actualExceptionName = e.getExceptionClassName();
        fmt.add(errorStyler.secondary("Caused by: " + actualExceptionName + ": " + e.getMessage()));
        for (StackTraceElement element : e.getStackTrace()) {
            fmt.add(errorStyler.secondary("\tat " + element));
        }
        
        formatEvalExceptionCause((EvalException) e.getCause(), fmt);
    }

    private List<String> formatUnresolvedReferenceException(UnresolvedReferenceException e) {
        List<String> fmt = new ArrayList<>();

        DeclarationSnippet snippet = e.getSnippet();

        List<String> unresolvedDependencies = jShell.unresolvedDependencies(snippet)
                .collect(Collectors.toList());
        if (!unresolvedDependencies.isEmpty()) {
            fmt.addAll(errorStyler.primaryLines(snippet.source()));
            fmt.add(errorStyler.secondary("Unresolved dependencies:"));
            unresolvedDependencies.forEach(dep ->
                    fmt.add(errorStyler.secondary("   - " + dep)));
        }

        return fmt;
    }

    private List<String> formatEvaluationTimeoutException(EvaluationTimeoutException e) {
        List<String> fmt = new ArrayList<>(errorStyler.primaryLines(e.getSource()));

        fmt.add(errorStyler.secondary(String.format(
                "Evaluation timed out after %d %s.",
                e.getDuration(),
                e.getUnit().name().toLowerCase())
        ));

        return fmt;
    }

    private List<String> formatEvaluationInterruptedException(EvaluationInterruptedException e) {
        List<String> fmt = new ArrayList<>(errorStyler.primaryLines(e.getSource()));
        fmt.add(errorStyler.secondary("Evaluation interrupted."));
        return fmt;
    }

    @Override
    public <T> EvalBuilder<T> evalBuilder(String source) {
        return new JavaEvalBuilder<>(this, source, null, null, false);
    }

    @Override
    protected Object doEval(String source) {
        return evaluator.eval(jShell, source);
    }

    /**
     * Maps an evaluation or magic result to its Jupyter presentation: absent evaluation text produces no output,
     * evaluation text becomes text/plain only, and any other magic value is rendered as text.
     */
    static DisplayData toExecuteResult(Object value) {
        if (value instanceof EvaluationResult result) {
            String text = result.text();
            return text == null ? null : new DisplayData(text);
        }
        if (value == null || value instanceof DisplayData) {
            return (DisplayData) value;
        }
        return new DisplayData(String.valueOf(value));
    }

    private static final class JavaEvalBuilder<T> implements EvalBuilder<T> {
        private final JavaKernel kernel;
        private final String source;
        private final ParsedLineMagic lineMagic;
        private final ParsedCellMagic cellMagic;
        private final boolean renderResults;

        private JavaEvalBuilder(
                JavaKernel kernel,
                String source,
                ParsedLineMagic lineMagic,
                ParsedCellMagic cellMagic,
                boolean renderResults) {
            this.kernel = kernel;
            this.source = source;
            this.lineMagic = lineMagic;
            this.cellMagic = cellMagic;
            this.renderResults = renderResults;
        }

        @Override
        public EvalBuilder<T> resolveMagics() {
            if (lineMagic != null || cellMagic != null) {
                return this;
            }

            ParsedCellMagic cell = kernel.getMagicsResolver().parseCellMagic(source);
            if (cell != null) {
                return new JavaEvalBuilder<>(kernel, source, null, cell, renderResults);
            }

            ParsedLineMagic line = kernel.getMagicsResolver().parseLineMagic(source);
            if (line != null && line.magicLinePrefix.isBlank()) {
                return new JavaEvalBuilder<>(kernel, source, line, null, renderResults);
            }

            return new JavaEvalBuilder<>(kernel, kernel.getMagicsResolver().resolve(source), null, null, renderResults);
        }

        @SuppressWarnings("unchecked")
        @Override
        public EvalBuilder<DisplayData> renderResults() {
            return renderResults
                    ? (EvalBuilder<DisplayData>) this
                    : new JavaEvalBuilder<>(kernel, source, lineMagic, cellMagic, true);
        }

        @Override
        @SuppressWarnings("unchecked")
        public T eval() {
            Object value = evalRaw();
            if (!renderResults) {
                return (T) (value instanceof EvaluationResult result ? result.text() : value);
            }
            return (T) toExecuteResult(value);
        }

        private Object evalRaw() {
            if (cellMagic == null && lineMagic == null) {
                return kernel.doEval(source);
            }

            try {
                return cellMagic != null
                        ? kernel.getMagicsRegistry().evalCellMagic(kernel, cellMagic.name, cellMagic.args, cellMagic.cellBodyAfterMagic)
                        : kernel.getMagicsRegistry().evalLineMagic(kernel, lineMagic.name, lineMagic.args);
            } catch (UndefinedMagicException e) {
                throw e;
            } catch (Exception e) {
                String magicName = cellMagic != null ? cellMagic.name : lineMagic.name;
                String kind = cellMagic != null ? "cell" : "line";
                throw new RuntimeException(
                        String.format("Exception running %s magic '%s': %s", kind, magicName, e.getMessage()),
                        e);
            }
        }
    }

    @Override
    public DisplayData inspect(String code, int at, boolean extraDetail) {
        // Move the code position to the end of the identifier to make the inspection work at any
        // point in the identifier. i.e "System.o|ut" or "System.out|" will return the same result.
        while (at + 1 < code.length() && IDENTIFIER_CHAR.test(code.charAt(at + 1))) at++;

        // If the next non-whitespace character is an opening paren '(' then this must be included
        // in the documentation search to ensure it searches for a method call.
        int parenIdx = at;
        while (parenIdx + 1 < code.length() && WS.test(code.charAt(parenIdx + 1))) parenIdx++;
        if (parenIdx + 1 < code.length() && code.charAt(parenIdx + 1) == '(') at = parenIdx + 1;

        List<SourceCodeAnalysis.Documentation> documentations = jShell.sourceCodeAnalysis().documentation(code, at + 1, true);
        if (documentations == null || documentations.isEmpty()) {
            return null;
        }

        DisplayData fmtDocs = new DisplayData(
                documentations.stream()
                        .map(doc -> {
                            String formatted = doc.signature();

                            String javadoc = doc.javadoc();
                            if (javadoc != null) formatted += '\n' + javadoc;

                            return formatted;
                        }).collect(Collectors.joining("\n\n")
                        )
        );

        fmtDocs.putHTML(
                documentations.stream()
                        .map(doc -> {
                            String formatted = doc.signature();

                            // TODO consider compiling the javadoc to html for pretty printing
                            String javadoc = doc.javadoc();
                            if (javadoc != null) formatted += "<br/>" + javadoc;

                            return formatted;
                        }).collect(Collectors.joining("<br/><br/>")
                        )
        );

        return fmtDocs;
    }

    @Override
    public ReplacementOptions complete(String code, int at) {
        int[] replaceStart = new int[1]; // As of now this is always the same as the cursor...
        // Check if the cursor is at the end of a line starting with % or %% (cell/line magic)
        // and if so, offer completions for all magic aliases and names.
        int lineStart = code.lastIndexOf('\n', at - 1) + 1;
        String line = code.substring(lineStart, at);

        // Match % or %% at start of line, followed by an identifier, and cursor is at end of that identifier
        Matcher magicMatcher = MAGIC_PATTERN.matcher(line);
        if (magicMatcher.find()) {
            String percent = magicMatcher.group(1);
            String prefix = magicMatcher.group(2);

            Set<String> magics = percent.equals("%%") ? magicsRegistry.getCellMagicNames() : magicsRegistry.getLineMagicNames();

            // Get all magic names and aliases

            // Filter by prefix if present
            List<String> options = magics.stream()
                    .filter(name -> name.startsWith(prefix))
                    .map(name -> percent + name)
                    .sorted()
                    .collect(Collectors.toList());
            if (!options.isEmpty()) {
                return new ReplacementOptions(options, lineStart, at);
            }
        }

        List<SourceCodeAnalysis.Suggestion> suggestions = jShell
                .sourceCodeAnalysis()
                .completionSuggestions(code, at, replaceStart);

        if (suggestions == null || suggestions.isEmpty()) {
            return null;
        }

        List<String> options = suggestions.stream()
                .sorted((s1, s2) ->
                        s1.matchesType()
                                ? s2.matchesType() ? 0 : -1
                                : s2.matchesType() ? 1 : 0
                )
                .map(SourceCodeAnalysis.Suggestion::continuation)
                .distinct()
                .collect(Collectors.toList());

        return new ReplacementOptions(options, replaceStart[0], at);
    }

    @Override
    public String isComplete(String code) {
        return evaluator.isComplete(jShell.sourceCodeAnalysis(), code);
    }

    @Override
    public void onShutdown(boolean isRestarting) {
        shuttingDown = true;
        try {
            super.onShutdown(isRestarting);
        } finally {
            try {
                jShell.close();
            } finally {
                try {
                    closeDisplayEndpoint();
                } finally {
                    evaluator.close();
                }
            }
        }
    }

    private void closeDisplayEndpoint() {
        if (displayEndpoint == null) {
            return;
        }
        try {
            displayEndpoint.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void interrupt() {
        evaluator.interrupt(jShell);
    }

    /**
     * @return a JShell instance used to evaluate Java code.
     */
    public JShell getJShell() {
        return jShell;
    }

    public CodeEvaluator getEvaluator() {
        return evaluator;
    }

    public static class Builder extends JavaKernelBuilder<Builder, JavaKernel> {
        private Builder() {
        }

        @Override
        public JavaKernel build() {
            JavaKernel[] kernelRef = new JavaKernel[1];
            UnixDomainDisplayEndpoint endpoint = null;
            JShell jShell = null;
            CodeEvaluator evaluator = null;
            try {
                endpoint = openDisplayEndpoint(kernelRef);
                afterDisplayEndpointCreated.run();

                String name = buildName();
                JupyterIO io = buildJupyterIO(buildJupyterIOEncoding());
                evaluator = buildCodeEvaluator();
                jShell = buildJShell(io, List.of("-Djjava.display.socket=" + endpoint.path()));
                MagicTranspiler magicTranspiler = buildMagicTranspiler();

                JavaKernel kernel = new JavaKernel(
                        name,
                        buildVersion(),
                        buildLanguageInfo(),
                        buildHelpLinks(),
                        buildHistoryManager(),
                        io,
                        buildCommManager(),
                        buildRenderer(),
                        buildMagicsResolver(magicTranspiler),
                        buildMagicsRegistry(),
                        buildExtensionsEnabled(),
                        buildErrorStyler(),
                        jShell,
                        evaluator
                );
                kernel.displayEndpoint = endpoint;
                kernelRef[0] = kernel;
                return kernel;
            } catch (RuntimeException | Error e) {
                // Deliberately catches Error too: an OutOfMemoryError or similar during build must still release the
                // JShell, display endpoint, and evaluator it has already created, and then be rethrown unchanged.
                closeAfterFailure(e, jShell, endpoint, evaluator);
                throw e;
            }
        }

        private UnixDomainDisplayEndpoint openDisplayEndpoint(JavaKernel[] kernelRef) {
            try {
                DisplayRequestHandler handler = displayHandler != null
                        ? displayHandler
                        : request -> applyDisplay(kernelRef[0], request);
                return new UnixDomainDisplayEndpoint(displayEndpointPath, handler);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to start display channel", e);
            }
        }

        private static void applyDisplay(JavaKernel kernel, DisplayRequest request) {
            if (request.operation() == DisplayRequest.Operation.UPDATE) {
                kernel.getIO().display.updateDisplay(request.displayId(), request.toDisplayData());
            } else {
                kernel.display(request.toDisplayData());
            }
        }

        private static void closeAfterFailure(Throwable failure, AutoCloseable... resources) {
            for (AutoCloseable resource : resources) {
                if (resource == null) {
                    continue;
                }
                try {
                    resource.close();
                } catch (Exception cleanup) {
                    if (cleanup != failure) failure.addSuppressed(cleanup);
                }
            }
        }

        protected List<HelpLink> buildHelpLinks() {
            return List.of(
                    new HelpLink("Java tutorials", "https://docs.oracle.com/javase/tutorial/"),
                    new HelpLink("JJava homepage", "https://github.com/dflib/jjava")
            );
        }
    }
}
