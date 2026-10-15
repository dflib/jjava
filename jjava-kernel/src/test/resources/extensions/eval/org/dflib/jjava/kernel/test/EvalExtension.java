package org.dflib.jjava.kernel.test;

import org.dflib.jjava.jupyter.Extension;
import org.dflib.jjava.jupyter.kernel.BaseKernel;
import org.dflib.jjava.jupyter.kernel.magic.LineMagic;

public class EvalExtension implements Extension {

    @Override
    public void install(BaseKernel kernel) {
        kernel.evalBuilder("var evalValue = \"Test message\";").eval();
        kernel.evalBuilder("var evalExtensionInstalled = true;").eval();
        LineMagic<String, BaseKernel> probe = (k, args) -> "manager:" + args.get(0);
        kernel.getMagicsRegistry().registerLineMagic("testManagerMagic", probe);
    }
}
