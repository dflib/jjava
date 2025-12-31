package org.dflib.jjava.kernel.test;

import org.dflib.jjava.jupyter.Extension;
import org.dflib.jjava.jupyter.kernel.BaseKernel;

public class EvalExtension implements Extension {

    @Override
    public void install(BaseKernel kernel) {
        kernel.evalBuilder("var evalValue = \"Test message\";").eval();
        kernel.evalBuilder("var evalExtensionInstalled = true;").eval();
    }
}
