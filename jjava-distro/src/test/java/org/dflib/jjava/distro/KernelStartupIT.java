package org.dflib.jjava.distro;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Container;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

class KernelStartupIT extends ContainerizedKernelCase {

    @Test
    void startUp() throws Exception {
        String snippet = "1000d + 1";
        Container.ExecResult snippetResult = executeInKernel(snippet);

        assertEquals(0, snippetResult.getExitCode(), snippetResult.getStdout());
        assertThat(snippetResult.getStdout(), not(containsString("|")));
        assertThat(snippetResult.getStdout(), containsString("1001.0"));
    }

    @Test
    void startUp_scriptRequiresClasspath() throws Exception {
        Map<String, String> env = Map.of(
                Env.JJAVA_CLASSPATH, TEST_CLASSPATH,
                Env.JJAVA_STARTUP_SCRIPT, "var obj = new org.dflib.jjava.Dummy()"
        );
        String snippet = "\"hash = \" + obj.hashCode()";
        Container.ExecResult snippetResult = executeInKernel(snippet, env);

        assertThat(snippetResult.getStdout(), not(containsString("|")));
        assertThat(snippetResult.getStdout(), matchesPattern("(?s).*hash = -?\\d+.*"));
    }
}
