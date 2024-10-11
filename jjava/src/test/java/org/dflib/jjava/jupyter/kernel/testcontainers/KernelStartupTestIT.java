package org.dflib.jjava.jupyter.kernel.testcontainers;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Container;

class KernelStartupTestIT extends KernelIT {

    @Test
    void startUp() throws Exception {
        Container.ExecResult snippetResult = executeInKernel("System.out.println(\"Hello world!\");");

        Assertions.assertEquals(0, snippetResult.getExitCode(), snippetResult.getStderr());
        MatcherAssert.assertThat(snippetResult.getStdout(), CoreMatchers.containsString("Hello world!"));
    }
}
