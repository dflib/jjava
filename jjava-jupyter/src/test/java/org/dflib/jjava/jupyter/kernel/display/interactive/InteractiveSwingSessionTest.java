package org.dflib.jjava.jupyter.kernel.display.interactive;

import com.google.gson.JsonObject;
import org.dflib.jjava.jupyter.kernel.comm.Comm;
import org.dflib.jjava.jupyter.kernel.comm.CommManager;
import org.dflib.jjava.jupyter.kernel.display.DisplayData;
import org.dflib.jjava.jupyter.kernel.display.Renderer;
import org.dflib.jjava.jupyter.kernel.display.common.Swing;
import org.dflib.jjava.jupyter.kernel.display.mime.MIMEType;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class InteractiveSwingSessionTest {

    static {
        System.setProperty("java.awt.headless", "true");
    }

    /** A {@link Comm} that records frames instead of putting them on a socket. */
    private static class RecordingComm extends Comm {
        final List<byte[]> frames = new ArrayList<>();
        final CountDownLatch firstFrame = new CountDownLatch(1);

        RecordingComm() {
            super(new CommManager(), "test-comm", InteractiveSwing.TARGET_NAME);
        }

        @Override
        public void send(JsonObject data, Map<String, Object> metadata, List<byte[]> blobs) {
            if (blobs != null && !blobs.isEmpty()) {
                frames.add(blobs.get(0));
                firstFrame.countDown();
            }
        }

        @Override
        protected void onMessage(org.dflib.jjava.jupyter.messages.Message<org.dflib.jjava.jupyter.messages.comm.CommMsgCommand> message) {
        }

        @Override
        protected void onClose(org.dflib.jjava.jupyter.messages.Message<org.dflib.jjava.jupyter.messages.comm.CommCloseCommand> closeMessage, boolean sending) {
        }
    }

    private static JsonObject key(String action, String ch) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "key");
        o.addProperty("action", action);
        o.addProperty("char", ch);
        o.addProperty("keyCode", ch.isEmpty() ? 0 : Character.toUpperCase(ch.charAt(0)));
        return o;
    }

    private static void typeChar(InteractiveSwingSession session, String ch) {
        session.handleMessage(key("down", ch));
        session.handleMessage(key("press", ch));
        session.handleMessage(key("up", ch));
    }

    private static JsonObject mouse(String action, int x, int y) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "mouse");
        o.addProperty("action", action);
        o.addProperty("x", x);
        o.addProperty("y", y);
        o.addProperty("button", 1);
        return o;
    }

    private static void flushEventQueue() throws Exception {
        // The EDT is single-threaded and FIFO, so anything queued by handleMessage() has run once this returns.
        SwingUtilities.invokeAndWait(() -> { });
    }

    @Test
    public void streamsAFrameOnBind() throws Exception {
        JButton button = new JButton("Hi");
        button.setSize(120, 40);
        InteractiveSwingSession session = new InteractiveSwingSession("tok-1", button);
        RecordingComm comm = new RecordingComm();

        session.bind(comm);

        assertTrue(comm.firstFrame.await(2, TimeUnit.SECONDS), "expected an initial frame to be streamed");
        assertTrue(comm.frames.get(0).length > 0, "frame should be non-empty PNG bytes");
    }

    @Test
    public void firesActionListenerOnClickGesture() throws Exception {
        AtomicInteger clicks = new AtomicInteger();
        JButton button = new JButton("Click");
        button.addActionListener(e -> clicks.incrementAndGet());

        InteractiveSwingSession session = new InteractiveSwingSession("tok-2", button);
        session.bind(new RecordingComm());

        // Press then release inside the button's bounds: the button fires its action on release.
        session.handleMessage(mouse("down", 30, 15));
        session.handleMessage(mouse("up", 30, 15));
        flushEventQueue();

        assertEquals(1, clicks.get(), "the button's ActionListener should have fired once");
    }

    @Test
    public void skipsUnchangedFramesButStreamsChangedOnes() throws Exception {
        JLabel label = new JLabel("v1");
        label.setSize(140, 30);
        InteractiveSwingSession session = new InteractiveSwingSession("tok-dup", label);
        RecordingComm comm = new RecordingComm();

        session.bind(comm);
        assertTrue(comm.firstFrame.await(2, TimeUnit.SECONDS));
        int afterFirst = comm.frames.size();

        // Re-request with no change: the identical frame must be suppressed (no encode/send).
        session.requestFrame();
        Thread.sleep(200);
        assertEquals(afterFirst, comm.frames.size(), "an unchanged frame should be skipped");

        // Change the visible content: a new frame must be streamed.
        SwingUtilities.invokeAndWait(() -> label.setText("v2 has changed"));
        session.requestFrame();
        Thread.sleep(200);
        assertTrue(comm.frames.size() > afterFirst, "a changed frame should be streamed");
    }

    @Test
    public void outputBundleCarriesInteractiveAndStaticFallback() {
        Renderer renderer = new Renderer();
        Swing.registerAll(renderer);
        JButton button = new JButton("Hi");
        button.setSize(120, 40);

        DisplayData data = InteractiveSwing.buildDisplayData(renderer, "tok", 120, 40, button);

        // Interactive payload for the extension...
        assertTrue(data.hasDataForType(MIMEType.parse(InteractiveSwing.MIME_TYPE)),
                "interactive MIME payload should be present");
        // ...and a static PNG fallback for frontends without it.
        assertNotNull(data.getData(MIMEType.IMAGE_PNG), "static PNG fallback should be present");
    }

    @Test
    public void typesIntoSwingTextField() throws Exception {
        JTextField field = new JTextField();
        field.setSize(160, 24);
        InteractiveSwingSession session = new InteractiveSwingSession("tok-type", field);
        session.bind(new RecordingComm());

        // Click to focus the field, then type "hi".
        session.handleMessage(mouse("down", 10, 10));
        session.handleMessage(mouse("up", 10, 10));
        typeChar(session, "h");
        typeChar(session, "i");
        flushEventQueue();

        assertEquals("hi", field.getText(), "typed characters should be inserted into the text field");
    }

    @Test
    public void fxBridgeRejectsNonNodes() {
        // Name-based detection must not false-positive on ordinary objects (and must not require JavaFX to be present).
        assertFalse(FxBridge.isNode(null));
        assertFalse(FxBridge.isNode("not a node"));
        assertFalse(FxBridge.isNode(new JButton()));
    }

    @Test
    public void resizeUpdatesDimensions() throws Exception {
        JButton button = new JButton("Resize");
        InteractiveSwingSession session = new InteractiveSwingSession("tok-3", button);
        session.bind(new RecordingComm());

        JsonObject resize = new JsonObject();
        resize.addProperty("type", "resize");
        resize.addProperty("width", 321);
        resize.addProperty("height", 123);
        session.handleMessage(resize);
        flushEventQueue();

        assertEquals(321, session.getWidth());
        assertEquals(123, session.getHeight());
    }
}
