package org.dflib.jjava.jupyter.messages.publish;

import com.google.gson.annotations.SerializedName;
import org.dflib.jjava.jupyter.messages.ContentType;
import org.dflib.jjava.jupyter.messages.MessageType;

public class PublishStream implements ContentType<PublishStream> {
    public static final MessageType<PublishStream> MESSAGE_TYPE = MessageType.PUBLISH_STREAM;

    @Override
    public MessageType<PublishStream> getType() {
        return MESSAGE_TYPE;
    }

    public enum StreamType {
        @SerializedName("stdout") OUT,
        @SerializedName("stderr") ERR
    }

    /**
     * One of 'stdout' or 'stderr'
     */
    @SerializedName("name")
    private final StreamType type;
    private final String text;

    public PublishStream(StreamType type, String text) {
        this.type = type;
        this.text = text;
    }

    public StreamType getStreamType() {
        return type;
    }

    public String getText() {
        return text;
    }
}
