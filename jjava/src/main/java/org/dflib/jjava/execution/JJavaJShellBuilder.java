package org.dflib.jjava.execution;

import jdk.jshell.JShell;
import jdk.jshell.spi.ExecutionControlProvider;
import org.dflib.jjava.jupyter.kernel.util.GlobFinder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class JJavaJShellBuilder {

    static final Pattern PATH_SPLITTER = Pattern.compile(File.pathSeparator, Pattern.LITERAL);
    static final Pattern BLANK = Pattern.compile("^\\s*$");

    private final JShell.Builder jshellBuilder;
    private final List<String> compilerOpts;
    private final List<String> classpath;
    private String timeout;

    public static JJavaJShellBuilder builder() {
        return new JJavaJShellBuilder();
    }

    protected JJavaJShellBuilder() {
        this.compilerOpts = new ArrayList<>();
        this.classpath = new ArrayList<>();
        this.jshellBuilder = JShell.builder();
    }

    public JJavaJShellBuilder addClasspathFromString(String classpath) {
        if (classpath == null) {
            return this;
        }

        if (BLANK.matcher(classpath).matches()) {
            return this;
        }

        Collections.addAll(this.classpath, PATH_SPLITTER.split(classpath));

        return this;
    }

    public JJavaJShellBuilder timeoutFromString(String timeout) {
        this.timeout = timeout;
        return this;
    }

    public JJavaJShellBuilder timeout(long timeout, TimeUnit timeoutUnit) {
        return this.timeoutFromString(String.format("%d %s", timeout, timeoutUnit.name()));
    }

    public JJavaJShellBuilder compilerOptsFromString(String opts) {
        if (opts != null) {
            compilerOpts.addAll(split(opts));
        }
        return this;
    }

    public JJavaJShellBuilder compilerOpts(String... opts) {
        Collections.addAll(this.compilerOpts, opts);
        return this;
    }

    public JJavaJShellBuilder stdout(PrintStream out) {
        jshellBuilder.out(out);
        return this;
    }

    public JJavaJShellBuilder stderr(PrintStream err) {
        jshellBuilder.err(err);
        return this;
    }

    public JJavaJShellBuilder stdin(InputStream in) {
        jshellBuilder.in(in);
        return this;
    }

    public JShell build(ExecutionControlProvider execControlProvider, String executionControlID) {

        Map<String, String> execControlParams = new HashMap<>();
        execControlParams.put(JJavaExecutionControlProvider.REGISTRATION_ID_KEY, executionControlID);

        if (timeout != null) {
            execControlParams.put(JJavaExecutionControlProvider.TIMEOUT_KEY, timeout);
        }

        JShell shell = jshellBuilder
                .executionEngine(execControlProvider, execControlParams)
                .compilerOptions(compilerOpts.toArray(new String[0]))
                .build();

        for (String cp : this.classpath) {

            if (BLANK.matcher(cp).matches()) {
                continue;
            }

            GlobFinder resolver = new GlobFinder(cp);
            try {
                for (Path entry : resolver.computeMatchingPaths()) {
                    shell.addToClasspath(entry.toAbsolutePath().toString());
                }
            } catch (IOException e) {
                throw new RuntimeException(String.format("IOException while computing classpath entries for '%s': %s", cp, e.getMessage()), e);
            }
        }

        return shell;
    }

    private static List<String> split(String opts) {
        opts = opts.trim();

        List<String> split = new ArrayList<>();

        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        boolean escape = false;
        for (char c : opts.toCharArray()) {
            switch (c) {
                case ' ':
                case '\t':
                    if (inQuotes) {
                        current.append(c);
                    } else if (current.length() > 0) {
                        // If whitespace is closing the string the add the current and reset
                        split.add(current.toString());
                        current.setLength(0);
                    }
                    break;
                case '\\':
                    if (escape) {
                        current.append("\\\\");
                        escape = false;
                    } else {
                        escape = true;
                    }
                    break;
                case '\"':
                    if (escape) {
                        current.append('"');
                        escape = false;
                    } else {
                        if (current.length() > 0 && inQuotes) {
                            split.add(current.toString());
                            current.setLength(0);
                            inQuotes = false;
                        } else {
                            inQuotes = true;
                        }
                    }
                    break;
                default:
                    current.append(c);
            }
        }

        if (current.length() > 0) {
            split.add(current.toString());
        }

        return split;
    }
}
