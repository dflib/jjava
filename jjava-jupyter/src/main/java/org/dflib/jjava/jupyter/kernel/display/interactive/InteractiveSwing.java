package org.dflib.jjava.jupyter.kernel.display.interactive;

import com.google.gson.JsonObject;
import org.dflib.jjava.jupyter.kernel.BaseKernel;
import org.dflib.jjava.jupyter.kernel.comm.CommManager;
import org.dflib.jjava.jupyter.kernel.display.DisplayData;
import org.dflib.jjava.jupyter.kernel.display.Renderer;
import org.dflib.jjava.jupyter.kernel.display.mime.MIMEType;

import java.awt.Component;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Entry point for embedding an interactive {@link Component} in a notebook.
 *
 * <p>It emits an output carrying the {@link #MIME_TYPE custom MIME type} so the JupyterLab extension can mount a canvas,
 * then opens a {@value #TARGET_NAME} comm whose {@code comm_open} data carries a {@code token}. The extension matches
 * that token to the canvas it just created and starts exchanging frames/events with the
 * {@link InteractiveSwingSession}.
 *
 * <p>The same output also carries a static {@code image/png} snapshot, so a frontend <em>without</em> the extension
 * (or any non-Lab frontend) degrades gracefully to a still image instead of a placeholder message.
 */
public final class InteractiveSwing {

    /** Comm target the JupyterLab extension registers a handler for. */
    public static final String TARGET_NAME = "jjava.swing.v1";

    /** MIME type of the placeholder output the JupyterLab extension renders into a canvas. */
    public static final String MIME_TYPE = "application/vnd.jjava.swing.v1+json";

    private InteractiveSwing() {
    }

    public static InteractiveSwingSession show(BaseKernel kernel, Component component) {
        // A JFXPanel needs a window ancestor (for its GraphicsConfiguration) to map input; no-op for pure Swing.
        FxBridge.ensureScreened(component);

        String token = UUID.randomUUID().toString();
        InteractiveSwingSession session = new InteractiveSwingSession(token, component);

        DisplayData data = buildDisplayData(
                kernel.getRenderer(), token, session.getWidth(), session.getHeight(), component);
        kernel.display(data);

        CommManager commManager = kernel.getCommManager();
        JsonObject openData = new JsonObject();
        openData.addProperty("token", token);
        openData.addProperty("width", session.getWidth());
        openData.addProperty("height", session.getHeight());

        InteractiveSwingComm comm = commManager.openComm(
                TARGET_NAME, openData,
                (manager, id, target, message) -> new InteractiveSwingComm(manager, id, target, session));

        if (comm != null) {
            session.bind(comm);
        }

        return session;
    }

    /**
     * Build the output bundle: the interactive {@link #MIME_TYPE} payload, plus a static {@code image/png} snapshot as a
     * fallback for frontends without the extension, plus a {@code text/plain} description.
     */
    static DisplayData buildDisplayData(Renderer renderer, String token, int width, int height, Component component) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("token", token);
        payload.put("width", width);
        payload.put("height", height);

        DisplayData data = new DisplayData();
        data.putData(MIME_TYPE, payload);

        try {
            Object png = renderer.renderAs(component, MIMEType.IMAGE_PNG.toString()).getData(MIMEType.IMAGE_PNG);
            if (png != null) {
                data.putData(MIMEType.IMAGE_PNG, png);
            }
        } catch (Exception e) {
            // The static snapshot is best-effort; the interactive payload is still emitted.
        }

        data.putText("Interactive Swing component — install the jjava-swing JupyterLab extension for interactivity "
                + "(a static snapshot is shown otherwise).");
        return data;
    }
}
