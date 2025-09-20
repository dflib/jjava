package org.dflib.jjava.runtime;

import org.dflib.jjava.JJava;
import org.dflib.jjava.jupyter.kernel.display.DisplayData;

import java.util.Objects;
import java.util.UUID;

/**
 * Display-related static methods exposed in each notebook via static imports.
 */
public class Display {

    public static DisplayData render(Object o) {
        return Objects
                .requireNonNull(JJava.getKernelInstance(), "No JJava kernel running")
                .getRenderer()
                .render(o);
    }

    public static DisplayData render(Object o, String... as) {
        return Objects
                .requireNonNull(JJava.getKernelInstance(), "No JJava kernel running")
                .getRenderer()
                .renderAs(o, as);
    }

    public static String display(Object o) {

        DisplayData data = render(o);

        String id = data.getDisplayId();
        if (id == null) {
            id = UUID.randomUUID().toString();
            data.setDisplayId(id);
        }

        Objects
                .requireNonNull(JJava.getKernelInstance(), "No JJava kernel running")
                .display(data);
        return id;
    }

    public static String display(Object o, String... as) {
        DisplayData data = render(o, as);

        String id = data.getDisplayId();
        if (id == null) {
            id = UUID.randomUUID().toString();
            data.setDisplayId(id);
        }

        Objects
                .requireNonNull(JJava.getKernelInstance(), "No JJava kernel running")
                .display(data);
        return id;
    }

    public static void updateDisplay(String id, Object o) {
        DisplayData data = render(o);
        Objects.requireNonNull(JJava.getKernelInstance(), "No JJava kernel running")
                .getIO()
                .display.updateDisplay(id, data);
    }

    public static void updateDisplay(String id, Object o, String... as) {
        DisplayData data = render(o, as);
        Objects.requireNonNull(JJava.getKernelInstance(), "No JJava kernel running")
                .getIO()
                .display.updateDisplay(id, data);
    }
}
