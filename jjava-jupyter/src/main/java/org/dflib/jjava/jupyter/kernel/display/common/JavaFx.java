package org.dflib.jjava.jupyter.kernel.display.common;

import org.dflib.jjava.jupyter.kernel.display.RenderContext;
import org.dflib.jjava.jupyter.kernel.display.Renderer;
import org.dflib.jjava.jupyter.kernel.display.mime.MIMEType;

import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Renders a JavaFX {@code javafx.scene.Node} as a static image: it snapshots the node on the JavaFX Application Thread
 * and converts the result to a {@link BufferedImage}, which is then encoded by {@link Image}. So {@code display(node)}
 * (or a bare node expression) yields a still PNG that works anywhere a static image does (GitHub, nbviewer, ...).
 *
 * <p>Everything is done reflectively, so jjava-jupyter keeps <strong>no compile- or link-time dependency on
 * JavaFX</strong>: JavaFX is optional and user-supplied (e.g. {@code %maven org.openjfx:javafx-controls:...}). The
 * renderer is registered <strong>by class name</strong> so it matches user nodes regardless of which class loader loaded
 * them.
 *
 * <p>Producing a snapshot starts the JavaFX toolkit, which needs a display; on a headless host run under Xvfb. If the
 * toolkit can't start, rendering degrades to a short text message instead of failing the cell.
 */
public class JavaFx {

    public static final String NODE_CLASS = "javafx.scene.Node";

    private static final long SNAPSHOT_TIMEOUT_SECONDS = 15;

    public static void registerAll(Renderer renderer) {
        renderer.createRegistration(NODE_CLASS)
                .preferring(Image.PNG)
                .supporting(Image.JPEG)
                .register(JavaFx::renderNode);
    }

    public static void renderNode(Object node, RenderContext context) {
        try {
            Image.renderImage(snapshot(node), context);
        } catch (Throwable t) {
            context.renderIfRequested(MIMEType.TEXT_PLAIN,
                    () -> "Could not render JavaFX node as an image: " + rootMessage(t)
                            + " (JavaFX needs a display; on a headless host run the kernel under Xvfb).");
        }
    }

    private static BufferedImage snapshot(Object node) throws Exception {
        ClassLoader cl = node.getClass().getClassLoader();
        Class<?> platformClass = Class.forName("javafx.application.Platform", true, cl);
        Class<?> snapshotParamsClass = Class.forName("javafx.scene.SnapshotParameters", true, cl);
        Class<?> writableImageClass = Class.forName("javafx.scene.image.WritableImage", true, cl);
        Class<?> imageClass = Class.forName("javafx.scene.image.Image", true, cl);
        Class<?> swingFxUtilsClass = Class.forName("javafx.embed.swing.SwingFXUtils", true, cl);

        startToolkit(platformClass);

        Method runLater = platformClass.getMethod("runLater", Runnable.class);
        Method snapshotMethod = node.getClass().getMethod("snapshot", snapshotParamsClass, writableImageClass);
        Method fromFXImage = swingFxUtilsClass.getMethod("fromFXImage", imageClass, BufferedImage.class);

        AtomicReference<BufferedImage> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);

        // snapshot() must run on the JavaFX Application Thread.
        runLater.invoke(null, (Runnable) () -> {
            try {
                Object params = snapshotParamsClass.getConstructor().newInstance();
                Object fxImage = snapshotMethod.invoke(node, params, null);
                result.set((BufferedImage) fromFXImage.invoke(null, fxImage, null));
            } catch (Throwable t) {
                failure.set(t);
            } finally {
                done.countDown();
            }
        });

        if (!done.await(SNAPSHOT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Timed out waiting for the JavaFX snapshot (is a display available?)");
        }
        if (failure.get() != null) {
            throw new RuntimeException(failure.get());
        }
        return result.get();
    }

    private static void startToolkit(Class<?> platformClass) throws Exception {
        try {
            platformClass.getMethod("startup", Runnable.class).invoke(null, (Runnable) () -> { });
        } catch (InvocationTargetException e) {
            // IllegalStateException means the toolkit is already running, which is fine; anything else is fatal.
            if (!(e.getCause() instanceof IllegalStateException)) {
                throw e;
            }
        }
    }

    private static String rootMessage(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
    }
}
