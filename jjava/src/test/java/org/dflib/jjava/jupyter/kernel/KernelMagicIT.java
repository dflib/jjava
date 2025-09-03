package org.dflib.jjava.jupyter.kernel;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Container;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class KernelMagicIT extends ContainerizedKernelCase {

    @Test
    void jars() throws Exception {
        String jar = CONTAINER_RESOURCES + "/jakarta.annotation-api-3.0.0.jar";
        Container.ExecResult fetchResult = container.execInContainer(
                "curl", "-L", "-s", "-S", "-f",
                "https://repo1.maven.org/maven2/jakarta/annotation/jakarta.annotation-api/3.0.0/jakarta.annotation-api-3.0.0.jar",
                "-o", jar
        );
        assertEquals("", fetchResult.getStderr());

        String snippet = String.join("\n",
                "%jars " + jar,
                "import jakarta.annotation.Nullable;",
                "Nullable.class.getName()"
        );
        Container.ExecResult snippetResult = executeInKernel(snippet);

        assertThat(snippetResult.getStderr(), not(containsString("|")));
        assertThat(snippetResult.getStdout(), containsString("jakarta.annotation.Nullable"));
    }

    @Test
    void classpath() throws Exception {
        String snippet = String.join("\n",
                "%classpath " + TEST_CLASSPATH,
                "import org.dflib.jjava.Dummy;",
                "Dummy.class.getName()"
        );
        Container.ExecResult snippetResult = executeInKernel(snippet);

        assertThat(snippetResult.getStderr(), not(containsString("|")));
        assertThat(snippetResult.getStdout(), containsString("org.dflib.jjava.Dummy"));
    }

    @Test
    void addMavenDependency() throws Exception {
        String snippet = String.join("\n",
                "%maven org.dflib:dflib-jupyter:1.0.0-RC1",
                "System.getProperty(\"java.class.path\")"
        );
        Container.ExecResult snippetResult = executeInKernel(snippet);

        assertThat(snippetResult.getStderr(), not(containsString("|")));
        assertThat(snippetResult.getStdout(), containsString("dflib-jupyter-1.0.0-RC1.jar"));
    }

    @Test
    void addIvyDependency() throws Exception {
        String snippet = String.join("\n",
                "%maven jakarta.annotation#jakarta.annotation-api;3.0.0",
                "System.getProperty(\"java.class.path\")"
        );

        Container.ExecResult snippetResult = executeInKernel(snippet);

        assertThat(snippetResult.getStderr(), not(containsString("|")));
        assertThat(snippetResult.getStdout(), containsString("jakarta.annotation-api-3.0.0.jar"));
    }

    @Test
    void load() throws Exception {
        String script = CONTAINER_RESOURCES + "/test-ping.jshell";
        String snippet = String.join("\n",
                "%load " + script,
                "ping()"
        );
        Container.ExecResult snippetResult = executeInKernel(snippet);

        assertThat(snippetResult.getStderr(), not(containsString("|")));
        assertThat(snippetResult.getStdout(), containsString("pong!"));
    }

    @Test
    void loadFromPOM() throws Exception {
        String pom = CONTAINER_RESOURCES + "/test-pom.xml";
        String snippet = String.join("\n",
                "%loadFromPOM " + pom,
                "import jakarta.annotation.Nullable;",
                "Nullable.class.getName()"
        );
        Container.ExecResult snippetResult = executeInKernel(snippet);

        assertThat(snippetResult.getStderr(), not(containsString("|")));
        assertThat(snippetResult.getStdout(), containsString("jakarta.annotation.Nullable"));
    }

    @Test
    void loadFromJBang() throws Exception {
        String snippet = String.join("\n",
                "%%jbang",
                "//DEPS jakarta.annotation:jakarta.annotation-api:3.0.0",
                "import jakarta.annotation.Nullable;",
                "Nullable.class.getName()"
        );
        Container.ExecResult snippetResult = executeInKernel(snippet);

        assertThat(snippetResult.getStderr(), not(containsString("|")));
        assertThat(snippetResult.getStdout(), containsString("jakarta.annotation.Nullable"));
    }
}
