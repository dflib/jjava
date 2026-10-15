package org.dflib.jjava.jupyter.kernel;

import org.dflib.jjava.jupyter.kernel.display.DisplayData;
import org.dflib.jjava.jupyter.kernel.magic.UndefinedMagicException;

import java.util.List;
import java.util.UUID;

/**
 * Manager-side notebook helpers. They always use the running kernel directly; executor code uses
 * {@link ExecutorNotebookStatics} instead.
 */
public class BaseNotebookStatics {

    public static void printf(String format, Object... args) {
        System.out.printf(format, args);
    }

    public static Object eval(String source) {
        return BaseKernel.notebookKernel().evalBuilder(source).resolveMagics().eval();
    }

    public static <T> T lineMagic(String name, List<String> args) {
        BaseKernel kernel = BaseKernel.notebookKernel();
        try {
            return kernel.getMagicsRegistry().evalLineMagic(kernel, name, args);
        } catch (UndefinedMagicException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(String.format("Exception running line magic '%s': %s", name, e.getMessage()), e);
        }
    }

    public static <T> T cellMagic(String name, List<String> args, String body) {
        BaseKernel kernel = BaseKernel.notebookKernel();
        try {
            return kernel.getMagicsRegistry().evalCellMagic(kernel, name, args, body);
        } catch (UndefinedMagicException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(String.format("Exception running cell magic '%s': %s", name, e.getMessage()), e);
        }
    }

    public static DisplayData render(Object o) {
        return BaseKernel.notebookKernel().getRenderer().render(o);
    }

    public static DisplayData render(Object o, String... as) {
        return BaseKernel.notebookKernel().getRenderer().renderAs(o, as);
    }

    public static String display(Object o) {
        return displayData(render(o));
    }

    public static String display(Object o, String... as) {
        return displayData(render(o, as));
    }

    public static void updateDisplay(String id, Object o) {
        updateDisplayData(id, render(o));
    }

    public static void updateDisplay(String id, Object o, String... as) {
        updateDisplayData(id, render(o, as));
    }

    private static String displayData(DisplayData data) {
        String id = data.getDisplayId();
        if (id == null) {
            id = UUID.randomUUID().toString();
            data.setDisplayId(id);
        }
        BaseKernel.notebookKernel().display(data);
        return id;
    }

    private static void updateDisplayData(String id, DisplayData data) {
        BaseKernel.notebookKernel().getIO().display.updateDisplay(id, data);
    }
}
