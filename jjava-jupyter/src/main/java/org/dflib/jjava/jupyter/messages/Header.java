package org.dflib.jjava.jupyter.messages;

import com.google.gson.annotations.SerializedName;

import java.util.UUID;

public class Header<T> {
    public static final String KERNEL_USERNAME = "kernel";
    public static final String PROTOCOL_VERISON = "5.3";

    private final String id;
    private final String username;

    @SerializedName("session")
    private final String sessionId;

    @SerializedName("date")
    private final KernelTimestamp timestamp;

    @SerializedName("msg_type")
    private final MessageType<T> type;

    private final String version;

    public Header(MessageType<T> type) {
        this("", type);
    }

    public Header(String sessionId, MessageType<T> type) {
        this(
                UUID.randomUUID().toString(),
                KERNEL_USERNAME,
                sessionId,
                KernelTimestamp.now(),
                type,
                PROTOCOL_VERISON
        );
    }

    public Header(MessageContext ctx, MessageType<T> type) {
        this(
                UUID.randomUUID().toString(),
                ctx != null ? ctx.getHeader().getUsername() : KERNEL_USERNAME,
                ctx != null ? ctx.getHeader().getSessionId() : null,
                KernelTimestamp.now(),
                type,
                PROTOCOL_VERISON
        );
    }

    public Header(String id, String username, String sessionId, KernelTimestamp timestamp, MessageType<T> type, String version) {
        this.id = id;
        this.username = username;
        this.sessionId = sessionId;
        this.timestamp = timestamp;
        this.type = type;
        this.version = version;
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getSessionId() {
        return sessionId;
    }

    public KernelTimestamp getTimestamp() {
        return timestamp;
    }

    public MessageType<T> getType() {
        return type;
    }

    public String getVersion() {
        return version;
    }
}
