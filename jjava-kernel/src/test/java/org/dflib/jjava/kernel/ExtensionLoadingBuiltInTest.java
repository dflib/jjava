package org.dflib.jjava.kernel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ExtensionLoadingBuiltInTest {

    @Test
    void loadsExtensionsFromInitialClasspath() {
        JavaKernel.builder().name("TestKernel").build();
        assertNotNull(JavaNotebookStatics.kernel(), "Built-in extension should be installed from initial classpath");
    }
}
