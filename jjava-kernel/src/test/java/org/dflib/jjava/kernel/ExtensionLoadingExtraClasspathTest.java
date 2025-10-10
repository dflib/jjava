package org.dflib.jjava.kernel;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExtensionLoadingExtraClasspathTest {

    @Test
    void loadsExtensionsFromExtraClasspath() throws Exception {
        Path jar = TestJarFactory.buildJar(
                "java/",
                "java/org/dflib/jjava/kernel/test/TestExtension.java",
                "java/META-INF/services/org.dflib.jjava.jupyter.Extension"
        );

        List<String> extraClasspath = Stream.of(jar)
                .map(Path::toAbsolutePath)
                .map(Path::toString)
                .collect(Collectors.toList());

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
