package org.dflib.jjava.kernel.test;

import org.dflib.jjava.jupyter.Extension;
import org.dflib.jjava.jupyter.kernel.BaseKernel;

public class EvalExtension implements Extension {

    @Override
    public void install(BaseKernel kernel) {
        kernel.eval("var evalValue = \"Test message\";");
        kernel.eval("var evalExtensionInstalled = true;");
    }
}
