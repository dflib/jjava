package org.dflib.jjava.kernel;

import org.dflib.jjava.jupyter.kernel.util.PathsHandler;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class JavaKernelExtensionsLifecycleTest {

    @Test
    public void defaultExtensions() {
        assertNull(JavaNotebookStatics.kernel);

        JavaKernel kernel = JavaKernel.builder().name("TestKernel").build();
        assertNull(JavaNotebookStatics.kernel, "Kernel should not have loaded any extensions as of creation");

        try {
            kernel.onStartup();
            assertNotNull(JavaNotebookStatics.kernel, "Built-in extension was not installed");
        } finally {
            kernel.onShutdown(false);
        }

        assertNull(JavaNotebookStatics.kernel);
    }

    @Test
    void extraClasspathExtensions() throws Exception {
        Path jar = TestJarFactory.buildJar(
                "java/",
                "java/org/dflib/jjava/kernel/test/TestExtension.java",
                "java/META-INF/services/org.dflib.jjava.jupyter.Extension"
        );

        String extraClasspath = PathsHandler.joinPaths(PathsHandler.resolveGlobs(jar.toAbsolutePath().toString()));

        String extInstalledProp = "ext.installs:org.dflib.jjava.kernel.test.TestExtension";
        System.clearProperty(extInstalledProp);

        JavaKernel kernel = JavaKernel
                .builder()
                .name("TestKernel")
                .build();
        try {
            kernel.onStartup();
            assertNotNull(JavaNotebookStatics.kernel, "Built-in extension was not installed");
            assertNull(System.getProperty(extInstalledProp), "Custom extension should not be installed yet");

            kernel.addToClasspath(extraClasspath);
            assertEquals("1", System.getProperty(extInstalledProp), "Custom extension was not installed");

            kernel.addToClasspath(extraClasspath);
            assertEquals("1", System.getProperty(extInstalledProp), "Custom extension was installed more than once?");
        } finally {
            kernel.onShutdown(false);
        }

        assertNull(JavaNotebookStatics.kernel);
        assertNull(System.getProperty(extInstalledProp));
    }

}
