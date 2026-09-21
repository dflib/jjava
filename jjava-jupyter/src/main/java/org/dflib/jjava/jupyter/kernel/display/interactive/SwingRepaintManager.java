package org.dflib.jjava.jupyter.kernel.display.interactive;

import javax.swing.JComponent;
import javax.swing.RepaintManager;
import java.awt.Component;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A {@link RepaintManager} that, in addition to the default behaviour, notices when a component belonging to an
 * {@link InteractiveSwingSession} is repainted and asks that session to stream a fresh frame. This is what lets
 * asynchronous changes (animation timers, model updates, etc.) reach the browser, not just the repaints that follow an
 * input event.
 *
 * <p>It is installed globally (one per JVM/AppContext) but delegates to the superclass for everything, so ordinary Swing
 * components that are not hosted in a session behave exactly as before.
 */
class SwingRepaintManager extends RepaintManager {

    private static volatile boolean installed = false;

    // Maps each session root component to its session. A repainted component is matched to a session by walking up its
    // parent chain until a registered root is found.
    private static final Map<Component, InteractiveSwingSession> ROOTS = new ConcurrentHashMap<>();

    static synchronized void install() {
        if (!installed) {
            RepaintManager.setCurrentManager(new SwingRepaintManager());
            installed = true;
        }
    }

    static void register(Component root, InteractiveSwingSession session) {
        ROOTS.put(root, session);
    }

    static void unregister(Component root) {
        ROOTS.remove(root);
    }

    private static InteractiveSwingSession sessionFor(Component component) {
        if (ROOTS.isEmpty()) {
            return null;
        }
        for (Component current = component; current != null; current = current.getParent()) {
            InteractiveSwingSession session = ROOTS.get(current);
            if (session != null) {
                return session;
            }
        }
        return null;
    }

    @Override
    public void addDirtyRegion(JComponent c, int x, int y, int w, int h) {
        super.addDirtyRegion(c, x, y, w, h);
        InteractiveSwingSession session = sessionFor(c);
        if (session != null) {
            session.requestFrame();
        }
    }

    @Override
    public void addInvalidComponent(JComponent invalidComponent) {
        super.addInvalidComponent(invalidComponent);
        InteractiveSwingSession session = sessionFor(invalidComponent);
        if (session != null) {
            // An invalidated component means the layout may have changed, so the next frame must re-lay-out.
            session.markNeedsLayout();
            session.requestFrame();
        }
    }
}
