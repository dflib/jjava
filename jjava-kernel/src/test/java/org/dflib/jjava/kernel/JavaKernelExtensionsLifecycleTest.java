package org.dflib.jjava.kernel;

import org.dflib.jjava.jupyter.kernel.util.PathsHandler;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JavaKernelExtensionsLifecycleTest {

    @Test
    void defaultExtension() {
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
    void extraClasspathExtension() throws Exception {
        Path extensionJar = TestJarFactory.buildJar(
                "extensions/classpath/",
                "extensions/classpath/org/dflib/jjava/kernel/test/ExtraClasspathExtension.java",
                "extensions/classpath/META-INF/services/org.dflib.jjava.jupyter.Extension"
        );

        String extraClasspath = PathsHandler.joinPaths(List.of(extensionJar));

        String extInstalledProp = "ext.installs:org.dflib.jjava.kernel.test.ExtraClasspathExtension";
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

    @Test
    void evalExtension() throws Exception {
        Path extensionJar = TestJarFactory.buildJar(
                "extensions/eval/",
                "extensions/eval/org/dflib/jjava/kernel/test/EvalExtension.java",
                "extensions/eval/META-INF/services/org.dflib.jjava.jupyter.Extension"
        );

        String extraClasspath = PathsHandler.joinPaths(List.of(extensionJar));

        JavaKernel kernel = JavaKernel
                .builder()
                .name("TestKernel")
                .build();
        try {
            kernel.onStartup();
            kernel.addToClasspath(extraClasspath);

            Object installed = kernel.evalRaw("evalExtensionInstalled");
            assertEquals(true, installed, "EvalExtension should have been installed");

            Object result = kernel.evalRaw("evalValue");
            assertEquals("Test message", result.toString(), "eval() call was not successful");
        } finally {
            kernel.onShutdown(false);
        }
    }

    @Test
    void libraryExtension() throws Exception {
        Path libraryJar = TestJarFactory.buildJar(
                "extensions/library/",
                "extensions/library/org/dflib/jjava/kernel/test/TestLibraryClass.java"
        );
        Path extensionJar = TestJarFactory.buildJar(
                "extensions/library/",
                "extensions/library/org/dflib/jjava/kernel/test/ExternalLibraryExtension.java",
                "extensions/library/META-INF/services/org.dflib.jjava.jupyter.Extension"
        );

        String extraClasspath = PathsHandler.joinPaths(List.of(libraryJar, extensionJar));

        JavaKernel kernel = JavaKernel
                .builder()
                .name("TestKernel")
                .build();
        try {
            kernel.onStartup();
            kernel.addToClasspath(extraClasspath);

            Object installed = kernel.evalRaw("externalLibraryExtensionInstalled");
            assertEquals(true, installed, "ExternalLibraryExtension should have been installed");

            Object result = kernel.evalRaw("externalLibraryValue");
            assertEquals("Test message", result.toString(), "Library class method call was not successful");
        } finally {
            kernel.onShutdown(false);
        }
    }
}
