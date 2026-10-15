package org.dflib.jjava.distro;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotebookInitializerImportsTest {

    @Test
    public void startupScriptImportsOnlyExecutorHelpers() throws Exception {
        Field field = NotebookInitializer.class.getDeclaredField("STARTUP_SCRIPT");
        field.setAccessible(true);
        String script = (String) field.get(null);

        assertTrue(script.contains("import static org.dflib.jjava.jupyter.kernel.ExecutorNotebookStatics.*;"), script);
        assertTrue(script.contains("import static org.dflib.jjava.kernel.ExecutorJavaNotebookStatics.*;"), script);
        assertFalse(script.contains("BaseNotebookStatics"), script);
        assertFalse(script.contains("org.dflib.jjava.kernel.JavaNotebookStatics"), script);
    }
}
