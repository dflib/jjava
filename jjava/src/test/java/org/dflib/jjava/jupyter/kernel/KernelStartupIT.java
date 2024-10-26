package org.dflib.jjava.jupyter.kernel;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Container;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

class KernelStartupIT extends ContainerizedKernelCase {

    @Test
    void startUp() throws Exception {
        String snippet = "1000d + 1";
        Container.ExecResult snippetResult = executeInKernel(snippet);

        assertThat(snippetResult.getStderr(), not(containsString("|")));
        assertThat(snippetResult.getStdout(), containsString("1001.0"));
    }
}
