package org.dflib.jjava.jupyter.kernel;

import org.dflib.jjava.jupyter.Extension;
import org.dflib.jjava.jupyter.kernel.display.DisplayData;
import org.dflib.jjava.jupyter.kernel.magic.UndefinedMagicException;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * A collection of methods exposed in every notebook via static imports. For the methods to work, an instance of
 * NotebookStatics must be loaded as a kernel extension, as this is when the kernel becomes known to the class.
 */
public class NotebookStatics implements Extension {

    private static final String STARTUP_SCRIPT = "import java.util.*;\n" +
            "import java.io.*;\n" +
            "import java.math.*;\n" +
            "import java.net.*;\n" +
            "import java.time.*;\n" +

            "import java.util.concurrent.*;\n" +
            "import java.util.prefs.*;\n" +
            "import java.util.regex.*;\n" +

            "import static org.dflib.jjava.jupyter.kernel.NotebookStatics.*;";

    private static BaseKernel kernel;

    @Override
    public void install(BaseKernel kernel) {

        if (NotebookStatics.kernel != null) {
            throw new IllegalStateException("Already initialized with another kernel: " + NotebookStatics.kernel.getBanner());
        }

        kernel.eval(STARTUP_SCRIPT);
        NotebookStatics.kernel = kernel;
    }

    private static BaseKernel nonNullKernel() {
        return Objects.requireNonNull(
                NotebookStatics.kernel,
                "No kernel running. Likely called outside of the notebook lifecycle");
    }

    public static void printf(String format, Object... args) {
        System.out.printf(format, args);
    }

    public static Object eval(String expr) {
        return nonNullKernel().evalRaw(expr);
    }

    public static <T> T lineMagic(String name, List<String> args) {
        BaseKernel kernel = nonNullKernel();

        try {
            return kernel.getMagicsRegistry().evalLineMagic(kernel, name, args);
        } catch (UndefinedMagicException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(String.format("Exception occurred while running line magic '%s': %s", name, e.getMessage()), e);
        }
    }

    public static <T> T cellMagic(String name, List<String> args, String body) {
        BaseKernel kernel = nonNullKernel();

        try {
            return kernel.getMagicsRegistry().evalCellMagic(kernel, name, args, body);
        } catch (UndefinedMagicException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(String.format("Exception occurred while running cell magic '%s': %s", name, e.getMessage()), e);
        }
    }

    public static DisplayData render(Object o) {
        return nonNullKernel().getRenderer().render(o);
    }

    public static DisplayData render(Object o, String... as) {
        return nonNullKernel().getRenderer().renderAs(o, as);
    }

    public static String display(Object o) {

        DisplayData data = render(o);

        String id = data.getDisplayId();
        if (id == null) {
            id = UUID.randomUUID().toString();
            data.setDisplayId(id);
        }

        nonNullKernel().display(data);
        return id;
    }

    public static String display(Object o, String... as) {
        DisplayData data = render(o, as);

        String id = data.getDisplayId();
        if (id == null) {
            id = UUID.randomUUID().toString();
            data.setDisplayId(id);
        }

        nonNullKernel().display(data);
        return id;
    }

    public static void updateDisplay(String id, Object o) {
        DisplayData data = render(o);
        nonNullKernel().getIO().display.updateDisplay(id, data);
    }

    public static void updateDisplay(String id, Object o, String... as) {
        DisplayData data = render(o, as);
        nonNullKernel().getIO().display.updateDisplay(id, data);
    }
}
