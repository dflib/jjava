/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 Spencer Park
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.dflib.jjava;

import jdk.jshell.DeclarationSnippet;
import jdk.jshell.EvalException;
import jdk.jshell.JShell;
import jdk.jshell.Snippet;
import jdk.jshell.SnippetEvent;
import jdk.jshell.SourceCodeAnalysis;
import jdk.jshell.UnresolvedReferenceException;
import org.dflib.jjava.execution.CodeEvaluator;
import org.dflib.jjava.execution.CodeEvaluatorBuilder;
import org.dflib.jjava.execution.CompilationException;
import org.dflib.jjava.execution.EvaluationInterruptedException;
import org.dflib.jjava.execution.EvaluationTimeoutException;
import org.dflib.jjava.execution.IncompleteSourceException;
import org.dflib.jjava.execution.MagicsSourceTransformer;
import org.dflib.jjava.jupyter.Extension;
import org.dflib.jjava.jupyter.kernel.BaseKernel;
import org.dflib.jjava.jupyter.kernel.LanguageInfo;
import org.dflib.jjava.jupyter.kernel.ReplacementOptions;
import org.dflib.jjava.jupyter.kernel.display.DisplayData;
import org.dflib.jjava.jupyter.kernel.magic.common.Load;
import org.dflib.jjava.jupyter.kernel.magic.registry.Magics;
import org.dflib.jjava.jupyter.kernel.util.CharPredicate;
import org.dflib.jjava.jupyter.kernel.util.StringStyler;
import org.dflib.jjava.jupyter.kernel.util.TextColor;
import org.dflib.jjava.jupyter.messages.Header;
import org.dflib.jjava.magics.ClasspathMagics;
import org.dflib.jjava.magics.MavenResolver;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class JavaKernel extends BaseKernel {

    public static String completeCodeSignifier() {
        return IS_COMPLETE_YES;
    }

    public static String invalidCodeSignifier() {
        return IS_COMPLETE_BAD;
    }

    public static String maybeCompleteCodeSignifier() {
        return IS_COMPLETE_MAYBE;
    }

    private static final CharPredicate IDENTIFIER_CHAR = CharPredicate.builder()
            .inRange('a', 'z')
            .inRange('A', 'Z')
            .inRange('0', '9')
            .match('_')
            .build();
    private static final CharPredicate WS = CharPredicate.anyOf(" \t\n\r");

    private final String version;
    private final CodeEvaluator evaluator;
    private final MavenResolver mavenResolver;

    private final MagicsSourceTransformer magicsTransformer;
    private final Magics magics;

    private final LanguageInfo languageInfo;
    private final String banner;
    private final List<LanguageInfo.Help> helpLinks;

    private final StringStyler errorStyler;

    @SuppressWarnings("removal")
    public JavaKernel(String version) {
        this.version = version;
        this.evaluator = new CodeEvaluatorBuilder()

                .addClasspathFromString(System.getenv(JJava.CLASSPATH_KEY))
                .addClasspathFromString(System.getenv(Env.JJAVA_CLASSPATH))

                .compilerOptsFromString(System.getenv(JJava.COMPILER_OPTS_KEY))
                .compilerOptsFromString(System.getenv(Env.JJAVA_COMPILER_OPTS))

                .startupScriptFiles(System.getenv(JJava.STARTUP_SCRIPTS_KEY))
                .startupScriptFiles(System.getenv(Env.JJAVA_STARTUP_SCRIPTS_PATH))

                .startupScript(System.getenv(JJava.STARTUP_SCRIPT_KEY))
                .startupScript(System.getenv(Env.JJAVA_STARTUP_SCRIPT))

                // TODO: this property is not additive, so IJAVA_TIMEOUT is overridden by JJAVA_TIMEOUT,
                //  even if the latter is null
                .timeoutFromString(System.getenv(JJava.TIMEOUT_DURATION_KEY))
                .timeoutFromString(System.getenv(Env.JJAVA_TIMEOUT))

                .sysStdout()
                .sysStderr()
                .sysStdin()
                .build();

        this.mavenResolver = buildDependencyResolver();

        this.magicsTransformer = new MagicsSourceTransformer();
        this.magics = new Magics();
        this.magics.registerMagics(this.mavenResolver);
        this.magics.registerMagics(new ClasspathMagics(this::addToClasspath));
        this.magics.registerMagics(new Load(List.of(".jsh", ".jshell", ".java", ".jjava"), this::eval));

        this.languageInfo = new LanguageInfo.Builder("Java")
                .version(Runtime.version().toString())
                .mimetype("text/x-java-source")
                .fileExtension(".jshell")
                .pygments("java")
                .codemirror("java")
                .build();
        this.banner = String.format("Java %s :: JJava kernel %s \nProtocol v%s implementation by %s %s",
                Runtime.version().toString(),
                version,
                Header.PROTOCOL_VERISON,
                KERNEL_META.getOrDefault("project", "UNKNOWN"),
                KERNEL_META.getOrDefault("version", "UNKNOWN")
        );
        this.helpLinks = List.of(
                new LanguageInfo.Help("Java tutorial", "https://docs.oracle.com/javase/tutorial/java/nutsandbolts/index.html"),
                new LanguageInfo.Help("JJava homepage", "https://github.com/dflib/jjava")
        );

        this.errorStyler = new StringStyler.Builder()
                .addPrimaryStyle(TextColor.BOLD_RESET_FG)
                .addSecondaryStyle(TextColor.BOLD_RED_FG)
                .addHighlightStyle(TextColor.BOLD_RESET_FG)
                .addHighlightStyle(TextColor.RED_BG)
                //TODO map snippet ids to code cells and put the proper line number in the margin here
                .withLinePrefix(TextColor.BOLD_RESET_FG + "|   ")
                .build();

        this.mavenResolver.initImplicitExtensions();
    }

    public void addToClasspath(String path) {
        this.evaluator.getShell().addToClasspath(path);
    }

    public void handleExtensionLoading(Extension extension) {
        extension.install(this);
    }

    public MavenResolver getMavenResolver() {
        return this.mavenResolver;
    }

    public Magics getMagics() {
        return this.magics;
    }

    @Override
    public LanguageInfo getLanguageInfo() {
        return this.languageInfo;
    }

    @Override
    public String getBanner() {
        return this.banner;
    }

    @Override
    public List<LanguageInfo.Help> getHelpLinks() {
        return this.helpLinks;
    }

    /**
     * Determines whether auto-loading of extensions is enabled based on the value of the
     * {@code JJAVA_LOAD_EXTENSIONS} environment variable.
     * <br>
     * The feature is considered disabled if this variable is defined and its value is falsy ("", "0", "false").<br>
     * The feature is considered enabled in other cases.
     *
     * @return true if auto-loading of extensions is enabled, false otherwise
     * @since 1.0
     */
    public boolean autoLoadExtensions() {
        String envValue = System.getenv(Env.JJAVA_LOAD_EXTENSIONS);
        if (envValue == null) {
            return true;
        }
        String envValueTrimmed = envValue.trim();
        return !envValueTrimmed.isEmpty()
                && !envValueTrimmed.equals("0")
                && !envValueTrimmed.equalsIgnoreCase("false");
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
            return new LinkedList<>(super.formatError(e));
        }
    }

    private MavenResolver buildDependencyResolver() {
        return new MavenResolver(this::addToClasspath,
                autoLoadExtensions()
                        ? this::handleExtensionLoading
                        : ext -> {});
    }

    private List<String> formatCompilationException(CompilationException e) {
        List<String> fmt = new ArrayList<>();
        SnippetEvent event = e.getBadSnippetCompilation();
        Snippet snippet = event.snippet();
        this.evaluator.getShell().diagnostics(snippet)
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
            List<String> unresolvedDependencies = this.evaluator.getShell().unresolvedDependencies((DeclarationSnippet) snippet)
                    .collect(Collectors.toList());
            if (!unresolvedDependencies.isEmpty()) {
                fmt.addAll(this.errorStyler.primaryLines(snippet.source()));
                fmt.add(this.errorStyler.secondary("Unresolved dependencies:"));
                unresolvedDependencies.forEach(dep ->
                        fmt.add(this.errorStyler.secondary("   - " + dep)));
            }
        }

        return fmt;
    }

    private List<String> formatIncompleteSourceException(IncompleteSourceException e) {
        List<String> fmt = new ArrayList<>();

        String source = e.getSource();
        fmt.add(this.errorStyler.secondary("Incomplete input:"));
        fmt.addAll(this.errorStyler.primaryLines(source));

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

        List<String> unresolvedDependencies = this.evaluator.getShell().unresolvedDependencies(snippet)
                .collect(Collectors.toList());
        if (!unresolvedDependencies.isEmpty()) {
            fmt.addAll(this.errorStyler.primaryLines(snippet.source()));
            fmt.add(this.errorStyler.secondary("Unresolved dependencies:"));
            unresolvedDependencies.forEach(dep ->
                    fmt.add(this.errorStyler.secondary("   - " + dep)));
        }

        return fmt;
    }

    private List<String> formatEvaluationTimeoutException(EvaluationTimeoutException e) {
        List<String> fmt = new ArrayList<>(this.errorStyler.primaryLines(e.getSource()));

        fmt.add(this.errorStyler.secondary(String.format(
                "Evaluation timed out after %d %s.",
                e.getDuration(),
                e.getUnit().name().toLowerCase())
        ));

        return fmt;
    }

    private List<String> formatEvaluationInterruptedException(EvaluationInterruptedException e) {
        List<String> fmt = new ArrayList<>(this.errorStyler.primaryLines(e.getSource()));

        fmt.add(this.errorStyler.secondary("Evaluation interrupted."));

        return fmt;
    }

    public Object evalRaw(String expr) {
        expr = this.magicsTransformer.transformMagics(expr);

        return this.evaluator.eval(expr);
    }

    @Override
    public DisplayData eval(String expr) {
        Object result = this.evalRaw(expr);
        if (result == null) {
            return null;
        }
        return result instanceof DisplayData
                ? (DisplayData) result
                : this.getRenderer().render(result);
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

        List<SourceCodeAnalysis.Documentation> documentations = this.evaluator.getShell().sourceCodeAnalysis().documentation(code, at + 1, true);
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
        List<SourceCodeAnalysis.Suggestion> suggestions = this.evaluator.getShell().sourceCodeAnalysis().completionSuggestions(code, at, replaceStart);
        if (suggestions == null || suggestions.isEmpty()) return null;

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
        this.evaluator.shutdown();
    }

    @Override
    public void interrupt() {
        this.evaluator.interrupt();
    }

    /**
     * @return a JShell instance used to evaluate Java code.
     * @since 1.0-M3
     */
    public JShell getJShell() {
        return evaluator.getShell();
    }

    /**
     * @return a version of the Java kernel
     * @since 1.0-M3
     */
    public String getVersion() {
        return version;
    }
}
