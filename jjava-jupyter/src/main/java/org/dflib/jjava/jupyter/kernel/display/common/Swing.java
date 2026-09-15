package org.dflib.jjava.jupyter.kernel.display.common;

import org.dflib.jjava.jupyter.kernel.display.RenderContext;
import org.dflib.jjava.jupyter.kernel.display.Renderer;
import org.dflib.jjava.jupyter.kernel.display.mime.MIMEType;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * Renders an {@link java.awt.Component AWT/Swing component} as an image by painting it off-screen into a
 * {@link BufferedImage}, which is then encoded by {@link Image}. This works without a display (headless) since no native
 * window is ever created.
 */
public class Swing {
    public static final MIMEType PNG = MIMEType.IMAGE_PNG;
    public static final MIMEType JPEG = MIMEType.IMAGE_JPEG;
    public static final MIMEType GIF = MIMEType.IMAGE_GIF;

    public static void registerAll(Renderer renderer) {
        renderer.createRegistration(Component.class)
                .preferring(PNG)
                .supporting(JPEG, GIF)
                .register(Swing::renderComponent);
    }

    public static void renderComponent(Component component, RenderContext context) {
        Image.renderImage(snapshot(component), context);
    }

    /**
     * Paint a component using its current size, falling back to its {@link Component#getPreferredSize() preferred size}
     * when it has not been laid out yet (which is the usual case for a component that was never added to a window).
     */
    public static BufferedImage snapshot(Component component) {
        Dimension size = component.getSize();
        if (size.width <= 0 || size.height <= 0) {
            size = component.getPreferredSize();
        }
        return snapshot(component, size.width, size.height);
    }

    public static BufferedImage snapshot(Component component, int width, int height) {
        width = Math.max(width, 1);
        height = Math.max(height, 1);

        component.setSize(width, height);
        layoutTree(component);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            // print() rather than paint(): it bypasses Swing's double-buffering, which is the right behaviour for
            // off-screen rendering of a component that has no native peer.
            component.print(g);
        } finally {
            g.dispose();
        }
        return image;
    }

    /**
     * A component that was never realized in a window is not laid out, so its children have zero bounds and would not
     * paint (nor hit-test). Forcing a layout pass over the whole subtree gives every child a position and size.
     */
    public static void layoutTree(Component component) {
        synchronized (component.getTreeLock()) {
            component.doLayout();
            if (component instanceof Container) {
                for (Component child : ((Container) component).getComponents()) {
                    layoutTree(child);
                }
            }
        }
    }
}
