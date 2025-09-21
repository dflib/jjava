package org.dflib.jjava;

import jdk.jshell.DeclarationSnippet;
import jdk.jshell.EvalException;
import jdk.jshell.JShell;
import jdk.jshell.Snippet;
import jdk.jshell.SnippetEvent;
import jdk.jshell.SourceCodeAnalysis;
import jdk.jshell.UnresolvedReferenceException;
import org.dflib.jjava.execution.CodeEvaluator;
import org.dflib.jjava.execution.CompilationException;
import org.dflib.jjava.execution.EvaluationInterruptedException;
import org.dflib.jjava.execution.EvaluationTimeoutException;
import org.dflib.jjava.execution.IncompleteSourceException;
import org.dflib.jjava.execution.JJavaExecutionControlProvider;
import org.dflib.jjava.execution.JJavaMagicTranspiler;
import org.dflib.jjava.execution.JJavaJShellBuilder;
import org.dflib.jjava.jupyter.ExtensionLoader;
import org.dflib.jjava.jupyter.kernel.BaseKernel;
import org.dflib.jjava.jupyter.kernel.LanguageInfo;
import org.dflib.jjava.jupyter.kernel.ReplacementOptions;
import org.dflib.jjava.jupyter.kernel.display.DisplayData;
import org.dflib.jjava.jupyter.kernel.magic.CellMagic;
import org.dflib.jjava.jupyter.kernel.magic.LineMagic;
import org.dflib.jjava.jupyter.kernel.magic.MagicParser;
import org.dflib.jjava.jupyter.kernel.magic.MagicsRegistry;
import org.dflib.jjava.jupyter.kernel.util.CharPredicate;
import org.dflib.jjava.jupyter.kernel.util.StringStyler;
import org.dflib.jjava.jupyter.kernel.util.TextColor;
import org.dflib.jjava.jupyter.messages.Header;
import org.dflib.jjava.magics.ClasspathMagic;
import org.dflib.jjava.magics.JarsMagic;
import org.dflib.jjava.magics.LoadCodeMagic;
import org.dflib.jjava.magics.LoadFromPomCellMagic;
import org.dflib.jjava.magics.LoadFromPomLineMagic;
import org.dflib.jjava.magics.MavenMagic;
import org.dflib.jjava.magics.MavenRepoMagic;
import org.dflib.jjava.maven.MavenResolver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

    private final String version;
    private final JShell jShell;
    private final CodeEvaluator evaluator;
    private final ExtensionLoader extensionLoader;
    private final boolean willLoadExtensions;
    private final MavenResolver mavenResolver;
    private final MagicParser magicParser;
    private final MagicsRegistry magics;
    private final LanguageInfo languageInfo;
    private final String banner;
    private final List<LanguageInfo.Help> helpLinks;
    private final StringStyler errorStyler;

    public JavaKernel(String version) {
        this.version = version;

        JJavaExecutionControlProvider execControlProvider = new JJavaExecutionControlProvider();
        String execControlID = UUID.randomUUID().toString();

        this.jShell = JJavaJShellBuilder.builder()
                .addClasspathFromString(System.getenv(Env.JJAVA_CLASSPATH))
                .compilerOptsFromString(System.getenv(Env.JJAVA_COMPILER_OPTS))
                .timeoutFromString(System.getenv(Env.JJAVA_TIMEOUT))
                .stdout(System.out)
                .stderr(System.err)
                .stdin(System.in)
                .build(execControlProvider, execControlID);

        this.evaluator = CodeEvaluator.builder()
                .startupScriptFiles(System.getenv(Env.JJAVA_STARTUP_SCRIPTS_PATH))
                .startupScript(System.getenv(Env.JJAVA_STARTUP_SCRIPT))
                .build(this.jShell, execControlProvider, execControlID);

        this.mavenResolver = new MavenResolver(this);
        this.magics = buildMagicsRegistry(mavenResolver);
        this.magicParser = new MagicParser("(?<=(?:^|=))\\s*%", "%%", new JJavaMagicTranspiler());
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

        this.extensionLoader = new ExtensionLoader();
        this.willLoadExtensions = shouldLoadExtensions();
        if (willLoadExtensions) {
            extensionLoader.loadExtensions().forEach(e -> e.install(this));
        }
    }

    /**
     * Adds multiple classpath entries to the JShell classpath and triggers extension loading for them.
     */
    public void addToClasspath(Iterable<String> paths) {
        paths.forEach(p -> jShell.addToClasspath(p));

        // Need to "addToClasspath" all entries in a collection before we can install any extensions, as an extension
        // may depend on other entries in the collection

        if (willLoadExtensions) {
            extensionLoader.loadExtensions(paths).forEach(e -> e.install(this));
        }
    }

    public MavenResolver getMavenResolver() {
        return mavenResolver;
    }

    public ExtensionLoader getExtensionLoader() {
        return extensionLoader;
    }

    public MagicsRegistry getMagics() {
        return magics;
    }

    @Override
    public LanguageInfo getLanguageInfo() {
        return languageInfo;
    }

    @Override
    public String getBanner() {
        return banner;
    }

    @Override
    public List<LanguageInfo.Help> getHelpLinks() {
        return helpLinks;
    }

    private MagicsRegistry buildMagicsRegistry(MavenResolver mavenResolver) {
        Map<String, LineMagic<?>> lineMagics = new HashMap<>();
        lineMagics.put("classpath", new ClasspathMagic(this));
        lineMagics.put("maven", new MavenMagic(mavenResolver));
        lineMagics.put("mavenRepo", new MavenRepoMagic(mavenResolver));
        lineMagics.put("load", new LoadCodeMagic(this, "", ".jsh", ".jshell", ".java", ".jjava"));
        lineMagics.put("loadFromPOM", new LoadFromPomLineMagic(mavenResolver));
        lineMagics.put("jars", new JarsMagic(this)); // TODO: deprecate redundant "jars" alias; "classpath" is a superset of this
        lineMagics.put("addMavenDependency", new MavenMagic(mavenResolver)); // TODO: deprecate redundant "addMavenDependency" alias

        Map<String, CellMagic<?>> cellMagics = new HashMap<>();
        cellMagics.put("loadFromPOM", new LoadFromPomCellMagic(mavenResolver));

        return new MagicsRegistry(lineMagics, cellMagics);
    }

    private boolean shouldLoadExtensions() {
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

    public Object evalRaw(String source) {
        return evaluator.eval(magicParser.resolveMagics(source));
    }

    @Override
    public DisplayData eval(String source) {
        Object result = evalRaw(source);
        if (result == null) {
            return null;
        }
        return result instanceof DisplayData
                ? (DisplayData) result
                : getRenderer().render(result);
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

            Set<String> magics = percent.equals("%%") ? this.magics.getCellMagicNames() : this.magics.getLineMagicNames();

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
        jShell.close();
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
        return jShell;
    }

    /**
     * @return a version of the Java kernel
     * @since 1.0-M3
     */
    public String getVersion() {
        return version;
    }
}
