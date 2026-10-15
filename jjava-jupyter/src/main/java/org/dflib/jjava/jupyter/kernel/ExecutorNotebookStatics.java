package org.dflib.jjava.jupyter.kernel;

import org.dflib.jjava.jupyter.kernel.display.DisplayData;
import org.dflib.jjava.jupyter.kernel.display.protocol.DisplayDeliveryBootstrap;
import org.dflib.jjava.jupyter.kernel.display.protocol.DisplayDeliveryException;
import org.dflib.jjava.jupyter.kernel.display.protocol.DisplayRequest;

import java.util.List;
import java.util.UUID;

/**
 * Notebook helpers for executor (user cell) code. Fixed by import at wiring time: display requests go through
 * {@link DisplayDeliveryBootstrap}, rendering is local, and the kernel is never read.
 */
public class ExecutorNotebookStatics {

    private ExecutorNotebookStatics() {
    }

    public static void printf(String format, Object... args) {
        System.out.printf(format, args);
    }

    public static Object eval(String source) {
        throw new UnsupportedOperationException("eval() cannot be called from executor code in JDI mode");
    }

    public static <T> T lineMagic(String name, List<String> args) {
        throw new UnsupportedOperationException(
                "Magics cannot be called programmatically from executor code in JDI mode; use %magic syntax");
    }

    public static <T> T cellMagic(String name, List<String> args, String body) {
        throw new UnsupportedOperationException(
                "Magics cannot be called programmatically from executor code in JDI mode; use %%magic syntax");
    }

    public static DisplayData render(Object o) {
        return renderPortable(o);
    }

    @SuppressWarnings("unused")
    public static DisplayData render(Object o, String... as) {
        return renderPortable(o);
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
        deliver(DisplayRequest.of(DisplayRequest.Operation.DISPLAY, id, data));
        return id;
    }

    private static void updateDisplayData(String id, DisplayData data) {
        deliver(DisplayRequest.of(DisplayRequest.Operation.UPDATE, id, data));
    }

    // one attempt, no retry; messages keep the existing prefixes
    private static void deliver(DisplayRequest request) {
        try {
            DisplayDeliveryBootstrap.current().deliver(request);
        } catch (DisplayDeliveryException e) {
            switch (e.kind()) {
                case UNAVAILABLE -> throw new IllegalStateException("Remote display channel is not available", e);
                case REJECTED -> throw new RuntimeException(e.getMessage(), e);
                case UNCERTAIN -> throw new RuntimeException("Failed to send display data: " + e.getMessage(), e);
            }
            throw e;
        }
    }

    private static DisplayData renderPortable(Object value) {
        if (value instanceof DisplayData data) {
            return data;
        }
        if (value instanceof CharSequence || value instanceof Number || value instanceof Boolean) {
            return new DisplayData(String.valueOf(value));
        }
        throw new IllegalArgumentException("Non-portable display value: " + value.getClass().getName());
    }
}
