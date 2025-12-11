package org.dflib.jjava.kernel.test;

import org.dflib.jjava.jupyter.Extension;
import org.dflib.jjava.jupyter.kernel.BaseKernel;

public class ExternalLibraryExtension implements Extension {

    @Override
    public void install(BaseKernel kernel) {
        Object value;
        try {
            Class<?> libClass = Class.forName("org.dflib.jjava.kernel.test.TestLibraryClass");
            value = libClass.getMethod("getMessage").invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }

        kernel.eval("var externalLibraryValue = \"" + value + "\";");
        kernel.eval("var externalLibraryExtensionInstalled = true;");
    }
}
