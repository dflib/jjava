package org.dflib.jjava.kernel;

import jdk.jshell.DeclarationSnippet;
import jdk.jshell.EvalException;
import jdk.jshell.JShell;
import jdk.jshell.Snippet;
import jdk.jshell.SnippetEvent;
import jdk.jshell.SourceCodeAnalysis;
import jdk.jshell.UnresolvedReferenceException;
import org.dflib.jjava.jupyter.kernel.BaseKernel;
import org.dflib.jjava.jupyter.kernel.HelpLink;
import org.dflib.jjava.jupyter.kernel.JupyterIO;
import org.dflib.jjava.jupyter.kernel.LanguageInfo;
import org.dflib.jjava.jupyter.kernel.ReplacementOptions;
import org.dflib.jjava.jupyter.kernel.comm.CommManager;
import org.dflib.jjava.jupyter.kernel.display.DisplayData;
import org.dflib.jjava.jupyter.kernel.display.Renderer;
import org.dflib.jjava.jupyter.kernel.history.HistoryManager;
import org.dflib.jjava.jupyter.kernel.magic.MagicsResolver;
import org.dflib.jjava.jupyter.kernel.magic.MagicTranspiler;
import org.dflib.jjava.jupyter.kernel.magic.MagicsRegistry;
import org.dflib.jjava.jupyter.kernel.util.CharPredicate;
import org.dflib.jjava.jupyter.kernel.util.PathsHandler;
import org.dflib.jjava.jupyter.kernel.util.StringStyler;
import org.dflib.jjava.kernel.execution.CodeEvaluator;
import org.dflib.jjava.kernel.execution.CompilationException;
import org.dflib.jjava.kernel.execution.EvaluationInterruptedException;
import org.dflib.jjava.kernel.execution.EvaluationTimeoutException;
import org.dflib.jjava.kernel.execution.IncompleteSourceException;
import org.dflib.jjava.kernel.execution.JJavaExecutionControlProvider;

import java.nio.charset.Charset;
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
    public List<String> formatError(Throwable e) {
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


        String evalExceptionClassName = EvalException.class.getName();
        String actualExceptionName = e.getExceptionClassName();
        super.formatError(e).stream()
                .map(line -> line.replace(evalExceptionClassName, actualExceptionName))
                .forEach(fmt::add);

        return fmt;
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

    /**
     * Same as {@link #eval(String)}, but not applying the renderer to evaluation result.
     */
    @Override
    public Object evalRaw(String source) {
        return evaluator.eval(magicsResolver.resolve(source));
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
        return this.evaluator.isComplete(code);
    }

    @Override
    public void onShutdown(boolean isRestarting) {
        super.onShutdown(isRestarting);
        jShell.close();
    }

    @Override
    public void interrupt() {
        this.evaluator.interrupt();
    }

    /**
     * Returns notebook ClassLoader, which in the case of JavaKernel is a JShell ClassLoader.
     */
    @Override
    protected ClassLoader getClassLoader() {
        return evaluator.getClassLoader();
    }

    /**
     * @return a JShell instance used to evaluate Java code.
     */
    public JShell getJShell() {
        return jShell;
    }

    public static class Builder extends JavaKernelBuilder<Builder, JavaKernel> {
        private Builder() {
        }

        @Override
        public JavaKernel build() {

            String name = buildName();
            Charset jupyterEncoding = buildJupyterIOEncoding();
            JJavaExecutionControlProvider jShellExecutionControlProvider = buildJShellExecControlProvider(name);
            JShell jShell = buildJShell(jShellExecutionControlProvider);
            LanguageInfo langInfo = buildLanguageInfo();
            MagicTranspiler magicTranspiler = buildMagicTranspiler();

            return new JavaKernel(
                    name,
                    buildVersion(),
                    langInfo,
                    buildHelpLinks(),
                    buildHistoryManager(),
                    buildJupyterIO(jupyterEncoding),
                    buildCommManager(),
                    buildRenderer(),
                    buildMagicsResolver(magicTranspiler),
                    buildMagicsRegistry(),
                    buildExtensionsEnabled(),
                    buildErrorStyler(),
                    jShell,
                    buildCodeEvaluator(jShell, jShellExecutionControlProvider)
            );
        }

        protected List<HelpLink> buildHelpLinks() {
            return List.of(
                    new HelpLink("Java tutorials", "https://docs.oracle.com/javase/tutorial/"),
                    new HelpLink("JJava homepage", "https://github.com/dflib/jjava")
            );
        }
    }
}
