package org.dflib.jjava;

import org.dflib.jjava.jupyter.Extension;
import org.dflib.jjava.jupyter.kernel.BaseKernel;

public class JJavaExtension implements Extension {

    private static final String STARTUP_SCRIPT = "import java.util.*;\n" +
            "import java.io.*;\n" +
            "import java.math.*;\n" +
            "import java.net.*;\n" +
            "import java.time.*;\n" +

            "import java.util.concurrent.*;\n" +
            "import java.util.prefs.*;\n" +
            "import java.util.regex.*;\n" +

            "import static org.dflib.jjava.runtime.Display.*;\n" +
            "import static org.dflib.jjava.runtime.Kernel.*;\n" +
            "import static org.dflib.jjava.runtime.Magics.*;\n" +

            "public void printf(String format, Object... args) {\n" +
            "    System.out.printf(format, args);\n" +
            "}";

    @Override
    public void install(BaseKernel kernel) {
        kernel.eval(STARTUP_SCRIPT);
    }
}
