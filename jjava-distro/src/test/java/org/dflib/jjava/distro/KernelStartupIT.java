package org.dflib.jjava.distro;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Container;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
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
}
