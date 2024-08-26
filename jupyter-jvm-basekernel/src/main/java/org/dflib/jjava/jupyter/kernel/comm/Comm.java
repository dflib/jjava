package org.dflib.jjava.jupyter.kernel.comm;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.dflib.jjava.jupyter.messages.Message;
import org.dflib.jjava.jupyter.messages.comm.CommCloseCommand;
import org.dflib.jjava.jupyter.messages.comm.CommMsgCommand;

import java.util.List;
import java.util.Map;

public abstract class Comm {
    private final CommManager manager;
    private final String id;
    private final String targetName;

    private boolean closed = false;

    public Comm(CommManager manager, String id, String targetName) {
        this(manager, id, targetName, null);
    }

    public Comm(CommManager manager, String id, String targetName, JsonElement initializationData) {
        this.manager = manager;
        this.id = id;
        this.targetName = targetName;
    }

    public String getID() {
        return this.id;
    }

    public String getTargetName() {
        return this.targetName;
    }

    public boolean isClosed() {
        return closed;
    }

    public void send(JsonObject data) {
        this.manager.messageComm(this.getID(), data);
    }

    public void send(JsonObject data, Map<String, Object> metadata, List<byte[]> blobs) {
        this.manager.messageComm(this.getID(), data, metadata, blobs);
    }

    /**
     * A callback for when the kernel receives a message who's destination is
     * this comm. This handler gets access to the entire message so that if desired
     * the comms may make use of the low level blob segments or want to make use of
     * the parent, identities, etc.
     * <p>
     * The data that most comms would be interested in is the {@link CommMsgCommand#getData()}
     * which is the payload attached to a message when sent via the frontend's {@code comm.send({})}
     * function. Since this can be any arbitrary JSON serializable thing is is given as a
     * {@link JsonElement}. It is recommended to deserialize it this handler to avoid
     * passing the JsonElement to too many other classes in case the serialization library changes in
     * the future.
     *
     * @param message the message received from the frontend that is targeted at this comm
     */
    protected abstract void onMessage(Message<CommMsgCommand> message);

    /**
     * Invoked when this comm is closed. The similar {@link Comm#close()} method is used
     * to close this comm where as this {@code onClose} is a callback to clean up this comm
     * when it is closed either by this side or as the result of a message from the frontend.
     * <dl>
     * <dt>If {@code sending}:</dt>
     * <dd>
     * then this method is free to modify the {@code closeMessage} to add any additional data
     * to the {@link CommCloseCommand#getData()} or the {@link Message#getBlobs()}. The message
     * will be sent <strong>after</strong> the execution of this method.
     * </dd>
     * <dt>If {@code !sending}:</dt>
     * <dd>
     * then this method may be interested in using the destructuring data in {@link CommCloseCommand#getData()}
     * that is snt by the front-end upon triggering th close.
     * </dd>
     * </dl>
     *
     * @param closeMessage the message triggering the close if from. This may contain some destructuring
     *                     parameters in {@link CommCloseCommand#getData()} if the frontend component
     *                     decided to send something.
     * @param sending      a boolean flag signaling if the close is the being triggered by this side
     *                     ({@code sending == true}) or from the front-end ({@code sending == false}).
     */
    protected abstract void onClose(Message<CommCloseCommand> closeMessage, boolean sending);

    public final void close() {
        if (this.closed) return;
        this.manager.closeComm(this);
        this.closed = true;
    }
}
