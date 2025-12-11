package org.dflib.jjava.kernel;

import jdk.jshell.spi.ExecutionControl;
import org.dflib.jjava.kernel.execution.JJavaLoaderDelegate;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.*;

public class JJavaLoaderDelegateTest {

    @Test
    public void addToClasspath() throws URISyntaxException, ExecutionControl.InternalException {
        File extraCp = new File(getClass().getResource("cp").toURI());
        assertTrue(extraCp.isDirectory());

        JJavaLoaderDelegate ld = new JJavaLoaderDelegate();

        String cp1 = System.getProperty("java.class.path");

        ld.addToClasspath(extraCp.getAbsolutePath());
        String cp2 = System.getProperty("java.class.path");
        assertEquals(cp1 + System.getProperty("path.separator") + extraCp.getAbsolutePath(), cp2);

        // TODO: test that classes from this location were actually loaded
    }
}
