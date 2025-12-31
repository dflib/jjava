package org.dflib.jjava.distro;

import org.dflib.jjava.jupyter.Extension;
import org.dflib.jjava.jupyter.kernel.BaseKernel;
import org.dflib.jjava.jupyter.kernel.BaseNotebookStatics;
import org.dflib.jjava.kernel.JavaNotebookStatics;

/**
 * An automatically-loaded extension that adds common imports to the notebook. This includes static method imports
 * from {@link BaseNotebookStatics} and {@link JavaNotebookStatics}.
 */
public class NotebookInitializer implements Extension {

    private static final String BASE_STATICS_CLASS = BaseNotebookStatics.class.getName();
    private static final String JAVA_STATICS_CLASS = JavaNotebookStatics.class.getName();

    private static final String STARTUP_SCRIPT = "import java.util.*;\n" +
            "import java.io.*;\n" +
            "import java.math.*;\n" +
            "import java.net.*;\n" +
            "import java.time.*;\n" +

            "import java.util.concurrent.*;\n" +
            "import java.util.prefs.*;\n" +
            "import java.util.regex.*;\n" +

            "import static " + BASE_STATICS_CLASS + ".*;\n" +
            "import static " + JAVA_STATICS_CLASS + ".*;";

    @Override
    public void install(BaseKernel kernel) {
        kernel.evalBuilder(STARTUP_SCRIPT).resolveMagics().eval();
    }
}
