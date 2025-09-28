package org.dflib.jjava.jupyter.messages.publish;

import com.google.gson.annotations.SerializedName;
import org.dflib.jjava.jupyter.messages.ContentType;
import org.dflib.jjava.jupyter.messages.MessageType;

public class PublishStatus implements ContentType<PublishStatus> {
    public static final MessageType<PublishStatus> MESSAGE_TYPE = MessageType.PUBLISH_STATUS;

    @Override
    public MessageType<PublishStatus> getType() {
        return MESSAGE_TYPE;
    }

    public static final PublishStatus BUSY = new PublishStatus(State.BUSY);
    public static final PublishStatus IDLE = new PublishStatus(State.IDLE);
    public static final PublishStatus STARTING = new PublishStatus(State.STARTING);

    public static PublishStatus forState(State state) {
        switch (state) {
            case BUSY: return BUSY;
            case IDLE: return IDLE;
            case STARTING: return STARTING;
            default: return null;
        }
    }

    public enum State {
        @SerializedName("busy") BUSY,
        @SerializedName("idle") IDLE,
        @SerializedName("starting") STARTING
    }

    @SerializedName("execution_state")
    private final State state;

    private PublishStatus(State state) {
        this.state = state;
    }

    public State getState() {
        return state;
    }
}
