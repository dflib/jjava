package org.dflib.jjava.jupyter.kernel;

import org.dflib.jjava.Env;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Container;

import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

class KernelEnvIT extends ContainerizedKernelCase {

    @Test
    void compilerOpts() throws Exception {
        Map<String, String> env = Map.of(Env.JJAVA_COMPILER_OPTS, "-source 9");
        String snippet = "var value = 1";
        Container.ExecResult snippetResult = executeInKernel(snippet, env);

        assertThat(snippetResult.getStderr(), CoreMatchers.allOf(
                containsString("|   var value = 1;"),
                containsString("'var' is a restricted local variable type")
        ));
    }

    @Test
    void timeout() throws Exception {
        Map<String, String> env = Map.of(Env.JJAVA_TIMEOUT, "3000");
        String snippet = "Thread.sleep(5000);";
        Container.ExecResult snippetResult = executeInKernel(snippet, env);

        assertThat(snippetResult.getStderr(), CoreMatchers.allOf(
                containsString("|   " + snippet),
                containsString("Evaluation timed out after 3000 milliseconds.")
        ));
    }

    @Test
    void classpath() throws Exception {
        Map<String, String> env = Map.of(Env.JJAVA_CLASSPATH, TEST_CLASSPATH);
        String snippet = String.join("\n",
                "import org.dflib.jjava.Dummy;",
                "Dummy.class.getName()"
        );
        Container.ExecResult snippetResult = executeInKernel(snippet, env);

        assertThat(snippetResult.getStderr(), not(containsString("|")));
        assertThat(snippetResult.getStdout(), containsString("org.dflib.jjava.Dummy"));
    }

    @Test
    void startUpScriptsPath() throws Exception {
        Map<String, String> env = Map.of(Env.JJAVA_STARTUP_SCRIPTS_PATH,  CONTAINER_RESOURCES + "/test-init.jshell");
        String snippet = "ping()";
        Container.ExecResult snippetResult = executeInKernel(snippet, env);

        assertThat(snippetResult.getStderr(), not(containsString("|")));
        assertThat(snippetResult.getStdout(), containsString("pong!"));
    }

    @Test
    void startUpScript() throws Exception {
        Map<String, String> env = Map.of(Env.JJAVA_STARTUP_SCRIPT, "public String ping() { return \"pong!\"; }");
        String snippet = "ping()";
        Container.ExecResult snippetResult = executeInKernel(snippet, env);

        assertThat(snippetResult.getStderr(), not(containsString("|")));
        assertThat(snippetResult.getStdout(), containsString("pong!"));
    }

    @Test
    void loadExtensions_Default() throws Exception {
        String snippet = "printf(\"Hello, %s!\", \"world\");";
        Container.ExecResult snippetResult = executeInKernel(snippet);

        assertThat(snippetResult.getStderr(), not(containsString("|")));
        assertThat(snippetResult.getStdout(), containsString("Hello, world!"));
    }

    @Test
    void loadExtensions_Disable() throws Exception {
        Map<String, String> env = Map.of(Env.JJAVA_LOAD_EXTENSIONS, "0");
        String snippet = "printf(\"Hello, %s!\", \"world\");";
        Container.ExecResult snippetResult = executeInKernel(snippet, env);

        assertThat(snippetResult.getStderr(), CoreMatchers.allOf(
                containsString("|   " + snippet),
                containsString("cannot find symbol")
        ));
    }

    @Test
    void jvmOpts() throws Exception {
        Map<String, String> env = Map.of(Env.JJAVA_JVM_OPTS, "-Xmx300m");
        String snippet = "Runtime.getRuntime().maxMemory()";
        Container.ExecResult snippetResult = executeInKernel(snippet, env);

        assertThat(snippetResult.getStderr(), not(containsString("|")));
        assertThat(snippetResult.getStdout(), containsString(String.valueOf(300 * (int) Math.pow(1024, 2))));
    }
}
