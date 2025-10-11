package org.dflib.jjava.kernel;

import org.dflib.jjava.jupyter.kernel.util.PathsHandler;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

// TODO: move this to BaseKernel? There's nothing JavaKernel specific here
public class ExtensionsLifecycleTest {

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

        String extensionClassName = "org.dflib.jjava.kernel.test.TestExtension";
        String installationsProperty = extensionInstallationsProperty(extensionClassName);
        System.clearProperty(installationsProperty);

        JavaKernel kernel = JavaKernel
                .builder()
                .name("TestKernel")
                .extraClasspath(extraClasspath)
                .build();
        try {
            kernel.onStartup();
            assertEquals("1", System.getProperty(installationsProperty), "Custom extension was not installed");
            assertNotNull(JavaNotebookStatics.kernel, "Built-in extension was not installed");

            kernel.addToClasspath(extraClasspath);
            assertEquals("1", System.getProperty(installationsProperty));
        } finally {
            kernel.onShutdown(false);
        }

        assertNull(JavaNotebookStatics.kernel);
        assertNull(System.getProperty(installationsProperty));
    }

    private static String extensionInstallationsProperty(String className) {
        return "ext.installs:" + className;
    }
}
