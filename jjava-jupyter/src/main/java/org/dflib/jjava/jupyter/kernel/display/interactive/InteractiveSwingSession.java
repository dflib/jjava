package org.dflib.jjava.jupyter.kernel.display.interactive;

import com.google.gson.JsonObject;
import org.dflib.jjava.jupyter.kernel.comm.Comm;
import org.dflib.jjava.jupyter.kernel.display.common.Swing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import java.awt.AlphaComposite;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Hosts a single {@link Component} off-screen and bridges it to a browser canvas: it streams PNG frames of the component
 * over a {@link Comm} and dispatches the mouse/keyboard events that arrive back from the frontend into the component's
 * Swing event handling. No native window is ever created, so this works headless.
 *
 * <p>All component manipulation (layout, painting, event dispatch) happens on the AWT event dispatch thread (EDT).
 */
public class InteractiveSwingSession {

    private static final Logger LOGGER = LoggerFactory.getLogger(InteractiveSwingSession.class);

    /** Frames are coalesced to roughly this cadence so a burst of repaints produces one frame. */
    private static final long FRAME_INTERVAL_MS = 33;

    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "jjava-swing-frames");
        thread.setDaemon(true);
        return thread;
    });

    private final String token;
    private final Component component;

    private volatile int width;
    private volatile int height;
    private volatile Comm comm;
    private volatile boolean disposed;

    private final AtomicBoolean framePending = new AtomicBoolean(false);

    // Reused across frames to avoid per-frame allocation; touched only on the EDT during painting.
    private BufferedImage frameImage;
    // Layout is expensive, so only redo it when something actually changed (resize or an invalidated component) rather
    // than on every repaint. Touched only on the EDT.
    private boolean needsLayout = true;
    // Hash of the last frame actually streamed; lets us drop frames whose pixels are unchanged. Scheduler-thread only.
    private long lastFrameHash;

    // Accessed only on the EDT. The component that should receive keyboard events, tracked from the last mouse press.
    private Component focusOwner;

    InteractiveSwingSession(String token, Component component) {
        this.token = token;
        this.component = component;

        Dimension size = component.getSize();
        if (size.width <= 0 || size.height <= 0) {
            size = component.getPreferredSize();
        }
        // Fall back to a usable default when the component has neither a size nor a preferred size (e.g. a bare
        // JFXPanel, which reports 0x0 until its scene drives a size).
        this.width = size.width > 0 ? size.width : 400;
        this.height = size.height > 0 ? size.height : 300;

        // Size and lay out eagerly so the component (and its children) have real bounds for hit-testing the very first
        // events, rather than waiting for the first frame to be rendered.
        component.setSize(width, height);
        Swing.layoutTree(component);

        SwingRepaintManager.install();
        SwingRepaintManager.register(component, this);
    }

    public String getToken() {
        return token;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    /** Bind the comm opened by the frontend and push the first frame. */
    void bind(Comm comm) {
        this.comm = comm;
        requestFrame();
    }

    /** Handle a message coming from the frontend canvas. */
    void handleMessage(JsonObject data) {
        if (data == null || disposed) {
            return;
        }
        String type = optString(data, "type", "");
        switch (type) {
            case "mouse":
                dispatchOnEdt(() -> dispatchMouse(data));
                break;
            case "key":
                dispatchOnEdt(() -> dispatchKey(data));
                break;
            case "resize":
                dispatchOnEdt(() -> resize(data));
                break;
            case "refresh":
                requestFrame();
                break;
            default:
                break;
        }
    }

    /**
     * Run an event-applying action on the EDT, then request a frame. A failing handler (e.g. a toolkit that can't map a
     * synthetic event off-screen) is logged and swallowed so it never kills the EDT or the session.
     */
    private void dispatchOnEdt(Runnable action) {
        SwingUtilities.invokeLater(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                LOGGER.debug("Interactive event dispatch failed", t);
            }
            requestFrame();
        });
    }

    /** Schedule a (coalesced) frame to be rendered and streamed. Safe to call from any thread. */
    void requestFrame() {
        if (disposed) {
            return;
        }
        if (framePending.compareAndSet(false, true)) {
            SCHEDULER.schedule(this::renderAndSend, FRAME_INTERVAL_MS, TimeUnit.MILLISECONDS);
        }
    }

    /** Flag that the component tree must be laid out again before the next frame (e.g. a child was invalidated). */
    void markNeedsLayout() {
        needsLayout = true;
    }

    void dispose() {
        disposed = true;
        SwingRepaintManager.unregister(component);
    }

    // --- frame streaming -------------------------------------------------------------------------------------------

    private void renderAndSend() {
        framePending.set(false);
        Comm target = this.comm;
        if (target == null || target.isClosed() || disposed) {
            return;
        }
        try {
            BufferedImage image = paintFrame();
            long hash = framePixelHash(image);
            if (hash == lastFrameHash) {
                // Nothing changed since the last streamed frame (e.g. a timer called repaint() without altering
                // anything) — skip the PNG encode and the send entirely.
                return;
            }
            lastFrameHash = hash;

            ByteArrayOutputStream out = new ByteArrayOutputStream(32 * 1024);
            ImageIO.write(image, "png", out);

            JsonObject message = new JsonObject();
            message.addProperty("type", "frame");
            message.addProperty("w", image.getWidth());
            message.addProperty("h", image.getHeight());
            target.send(message, null, Collections.singletonList(out.toByteArray()));
        } catch (Exception e) {
            LOGGER.debug("Failed to render/stream interactive Swing frame", e);
        }
    }

    private BufferedImage paintFrame() throws Exception {
        BufferedImage[] holder = new BufferedImage[1];
        Runnable paint = () -> holder[0] = paintOnEdt();
        if (SwingUtilities.isEventDispatchThread()) {
            paint.run();
        } else {
            SwingUtilities.invokeAndWait(paint);
        }
        return holder[0];
    }

    /** Paint the component into the reused buffer, laying out only when needed. Must run on the EDT. */
    private BufferedImage paintOnEdt() {
        int w = width;
        int h = height;
        if (frameImage == null || frameImage.getWidth() != w || frameImage.getHeight() != h) {
            frameImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            needsLayout = true;
        }
        if (needsLayout) {
            component.setSize(w, h);
            Swing.layoutTree(component);
            needsLayout = false;
        }

        Graphics2D g = frameImage.createGraphics();
        try {
            // Clear the reused buffer to fully transparent before repainting.
            g.setComposite(AlphaComposite.Clear);
            g.fillRect(0, 0, w, h);
            g.setComposite(AlphaComposite.SrcOver);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            component.print(g);
        } finally {
            g.dispose();
        }
        return frameImage;
    }

    private static long framePixelHash(BufferedImage image) {
        int[] pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        long hash = 1125899906842597L;
        for (int pixel : pixels) {
            hash = 31 * hash + pixel;
        }
        return hash;
    }

    // --- event dispatch (EDT) --------------------------------------------------------------------------------------

    private void resize(JsonObject data) {
        this.width = Math.max(1, optInt(data, "width", width));
        this.height = Math.max(1, optInt(data, "height", height));
        component.setSize(width, height);
        needsLayout = true;
    }

    private void dispatchMouse(JsonObject data) {
        String action = optString(data, "action", "");
        int x = optInt(data, "x", 0);
        int y = optInt(data, "y", 0);
        int button = optInt(data, "button", 1);
        long when = System.currentTimeMillis();
        int modifiers = mouseModifiers(data, action, button);

        Component target = componentAt(x, y);
        Point p = SwingUtilities.convertPoint(component, x, y, target);

        if ("wheel".equals(action)) {
            int rotation = optInt(data, "deltaY", 0) >= 0 ? 1 : -1;
            MouseWheelEvent wheel = new MouseWheelEvent(target, MouseEvent.MOUSE_WHEEL, when, modifiers,
                    p.x, p.y, 0, false, MouseWheelEvent.WHEEL_UNIT_SCROLL, 3, rotation);
            target.dispatchEvent(wheel);
            return;
        }

        int id;
        switch (action) {
            case "down": id = MouseEvent.MOUSE_PRESSED; break;
            case "up": id = MouseEvent.MOUSE_RELEASED; break;
            case "click": id = MouseEvent.MOUSE_CLICKED; break;
            case "move": id = MouseEvent.MOUSE_MOVED; break;
            case "drag": id = MouseEvent.MOUSE_DRAGGED; break;
            default: return;
        }

        if (id == MouseEvent.MOUSE_PRESSED) {
            focusOn(target);
        }

        boolean buttoned = id != MouseEvent.MOUSE_MOVED && id != MouseEvent.MOUSE_DRAGGED;
        int clickCount = buttoned ? Math.max(1, optInt(data, "clickCount", 1)) : 0;
        int awtButton = buttoned ? awtButton(button) : MouseEvent.NOBUTTON;

        MouseEvent event = new MouseEvent(target, id, when, modifiers, p.x, p.y, clickCount, false, awtButton);
        target.dispatchEvent(event);
    }

    private void dispatchKey(JsonObject data) {
        Component target = focusOwner != null ? focusOwner : component;

        // JavaFX: AWT key forwarding through an off-screen JFXPanel doesn't work (no real keyboard focus), so deliver
        // the key straight into the FX scene's focused node instead.
        if (FxBridge.isJFXPanel(target) && FxBridge.injectKey(target, data)) {
            return;
        }

        String action = optString(data, "action", "");
        long when = System.currentTimeMillis();
        int modifiers = keyboardModifiers(data);

        if ("press".equals(action)) {
            String text = optString(data, "char", "");
            if (text.isEmpty()) {
                return;
            }
            KeyEvent typed = new KeyEvent(target, KeyEvent.KEY_TYPED, when, modifiers,
                    KeyEvent.VK_UNDEFINED, text.charAt(0));
            redispatchKey(target, typed);
            return;
        }

        int id;
        switch (action) {
            case "down": id = KeyEvent.KEY_PRESSED; break;
            case "up": id = KeyEvent.KEY_RELEASED; break;
            default: return;
        }
        int keyCode = awtKeyCode(optInt(data, "keyCode", 0));
        String text = optString(data, "char", "");
        char keyChar = text.isEmpty() ? KeyEvent.CHAR_UNDEFINED : text.charAt(0);
        KeyEvent event = new KeyEvent(target, id, when, modifiers, keyCode, keyChar);
        redispatchKey(target, event);
    }

    /**
     * Deliver a key event via {@link KeyboardFocusManager#redispatchEvent}, which bypasses the focus manager's
     * type-ahead handling. A plain {@code dispatchEvent} would be swallowed here because no Java window holds focus
     * (the browser does), so the key would never reach the component's key bindings.
     */
    private static void redispatchKey(Component target, KeyEvent event) {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().redispatchEvent(target, event);
    }

    /**
     * Move synthetic focus to {@code target}: there is no real keyboard-focus manager off-screen, so we hand-deliver
     * FOCUS_LOST/FOCUS_GAINED. This activates a Swing text caret, and — for a JFXPanel — activates the embedded JavaFX
     * scene so its focused node accepts typing.
     */
    private void focusOn(Component target) {
        if (focusOwner == target) {
            return;
        }
        if (focusOwner != null) {
            focusOwner.dispatchEvent(new FocusEvent(focusOwner, FocusEvent.FOCUS_LOST));
        }
        focusOwner = target;
        target.dispatchEvent(new FocusEvent(target, FocusEvent.FOCUS_GAINED));
    }

    private Component componentAt(int x, int y) {
        Component deepest = SwingUtilities.getDeepestComponentAt(component, x, y);
        return deepest != null ? deepest : component;
    }

    private int mouseModifiers(JsonObject data, String action, int button) {
        int modifiers = keyboardModifiers(data);
        boolean buttonInvolved = "down".equals(action) || "up".equals(action)
                || "drag".equals(action) || "click".equals(action);
        if (buttonInvolved) {
            // Keep the button-down mask set even on release so SwingUtilities.isLeftMouseButton(e) and friends — which
            // gate button activation — recognise the gesture.
            modifiers |= buttonDownMask(button);
        }
        return modifiers;
    }

    private static int keyboardModifiers(JsonObject data) {
        int modifiers = 0;
        if (optBool(data, "shift")) modifiers |= InputEvent.SHIFT_DOWN_MASK;
        if (optBool(data, "ctrl")) modifiers |= InputEvent.CTRL_DOWN_MASK;
        if (optBool(data, "alt")) modifiers |= InputEvent.ALT_DOWN_MASK;
        if (optBool(data, "meta")) modifiers |= InputEvent.META_DOWN_MASK;
        return modifiers;
    }

    private static int buttonDownMask(int button) {
        switch (button) {
            case 2: return InputEvent.BUTTON2_DOWN_MASK;
            case 3: return InputEvent.BUTTON3_DOWN_MASK;
            default: return InputEvent.BUTTON1_DOWN_MASK;
        }
    }

    private static int awtButton(int button) {
        switch (button) {
            case 2: return MouseEvent.BUTTON2;
            case 3: return MouseEvent.BUTTON3;
            default: return MouseEvent.BUTTON1;
        }
    }

    /**
     * Browser {@code KeyboardEvent.keyCode} values mostly coincide with AWT virtual key codes for letters, digits,
     * arrows, etc. The notable exception is Enter (13 in the browser, {@link KeyEvent#VK_ENTER} 10 in AWT).
     */
    private static int awtKeyCode(int browserKeyCode) {
        if (browserKeyCode == 13) {
            return KeyEvent.VK_ENTER;
        }
        return browserKeyCode;
    }

    // --- json helpers ----------------------------------------------------------------------------------------------

    private static String optString(JsonObject data, String key, String fallback) {
        return data.has(key) && !data.get(key).isJsonNull() ? data.get(key).getAsString() : fallback;
    }

    private static int optInt(JsonObject data, String key, int fallback) {
        return data.has(key) && !data.get(key).isJsonNull() ? data.get(key).getAsInt() : fallback;
    }

    private static boolean optBool(JsonObject data, String key) {
        return data.has(key) && !data.get(key).isJsonNull() && data.get(key).getAsBoolean();
    }
}
