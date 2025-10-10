package org.dflib.jjava.kernel;

import org.dflib.jjava.jupyter.kernel.util.PathsHandler;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExtensionLoadingExtraClasspathTest {

    @Test
    void loadsExtensionsFromExtraClasspath() throws Exception {
        Path jar = TestJarFactory.buildJar(
                "java/",
                "java/org/dflib/jjava/kernel/test/TestExtension.java",
                "java/META-INF/services/org.dflib.jjava.jupyter.Extension"
        );

        String extraClasspath = PathsHandler.joinPaths(PathsHandler.resolveGlobs(jar.toAbsolutePath().toString()));

        String extensionClassName = "org.dflib.jjava.kernel.test.TestExtension";
        String installationsProperty = extensionInstallationsProperty(extensionClassName);
        System.clearProperty(installationsProperty);

        JavaKernel kernel = JavaKernel.builder().name("TestKernel").extraClasspath(extraClasspath).build();
        assertEquals("1", System.getProperty(installationsProperty));

        kernel.addToClasspath(extraClasspath);
        assertEquals("1", System.getProperty(installationsProperty));
    }

    private static String extensionInstallationsProperty(String className) {
        return "ext.installs:" + className;
    }
}
