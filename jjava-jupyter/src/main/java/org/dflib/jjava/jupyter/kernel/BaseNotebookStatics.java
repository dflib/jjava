package org.dflib.jjava.jupyter.kernel;

import org.dflib.jjava.jupyter.kernel.display.DisplayData;
import org.dflib.jjava.jupyter.kernel.display.interactive.FxBridge;
import org.dflib.jjava.jupyter.kernel.display.interactive.InteractiveSwing;
import org.dflib.jjava.jupyter.kernel.magic.UndefinedMagicException;

import java.awt.Component;
import java.util.List;
import java.util.UUID;

/**
 * A collection of static methods for notebook code to interact with the kernel. The methods are automatically exposed
 * in notebooks via a static import on bootstrap.
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

        DisplayData data = render(o);

        String id = data.getDisplayId();
        if (id == null) {
            id = UUID.randomUUID().toString();
            data.setDisplayId(id);
        }

        BaseKernel.notebookKernel().display(data);
        return id;
    }

    public static String display(Object o, String... as) {
        DisplayData data = render(o, as);

        String id = data.getDisplayId();
        if (id == null) {
            id = UUID.randomUUID().toString();
            data.setDisplayId(id);
        }

        BaseKernel.notebookKernel().display(data);
        return id;
    }

    /**
     * Display an AWT/Swing component or a JavaFX node as an interactive widget: it is streamed to the browser as a live
     * image and mouse/keyboard events from the browser are dispatched back into it. Requires the JJava JupyterLab
     * extension for interactivity; without it the output falls back to a static image snapshot.
     *
     * <p>A {@code javafx.scene.Node} is wrapped in a {@code JFXPanel} so it rides the same pipeline (JavaFX must be on
     * the classpath — it is an optional, user-supplied dependency).
     */
    public static void displayInteractive(Object component) {
        BaseKernel kernel = BaseKernel.notebookKernel();
        if (component instanceof Component) {
            InteractiveSwing.show(kernel, (Component) component);
        } else if (FxBridge.isNode(component)) {
            try {
                InteractiveSwing.show(kernel, FxBridge.wrap(component));
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("Failed to embed JavaFX node (is JavaFX on the classpath?)", e);
            }
        } else {
            throw new IllegalArgumentException(
                    "displayInteractive expects a java.awt.Component or a javafx.scene.Node, got: "
                            + (component == null ? "null" : component.getClass().getName()));
        }
    }

    public static void updateDisplay(String id, Object o) {
        DisplayData data = render(o);
        BaseKernel.notebookKernel().getIO().display.updateDisplay(id, data);
    }

    public static void updateDisplay(String id, Object o, String... as) {
        DisplayData data = render(o, as);
        BaseKernel.notebookKernel().getIO().display.updateDisplay(id, data);
    }
}
