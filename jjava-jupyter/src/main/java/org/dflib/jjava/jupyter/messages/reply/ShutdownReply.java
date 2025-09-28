package org.dflib.jjava.jupyter.messages.reply;

import org.dflib.jjava.jupyter.messages.ContentType;
import org.dflib.jjava.jupyter.messages.MessageType;
import org.dflib.jjava.jupyter.messages.ReplyType;
import org.dflib.jjava.jupyter.messages.request.ShutdownRequest;

public class ShutdownReply implements ContentType<ShutdownReply>, ReplyType<ShutdownRequest> {
    public static final MessageType<ShutdownReply> MESSAGE_TYPE = MessageType.SHUTDOWN_REPLY;
    public static final MessageType<ShutdownRequest> REQUEST_MESSAGE_TYPE = MessageType.SHUTDOWN_REQUEST;

    @Override
    public MessageType<ShutdownReply> getType() {
        return MESSAGE_TYPE;
    }

    @Override
    public MessageType<ShutdownRequest> getRequestType() {
        return REQUEST_MESSAGE_TYPE;
    }

    public static final ShutdownReply SHUTDOWN_AND_RESTART = new ShutdownReply(true);
    public static final ShutdownReply SHUTDOWN = new ShutdownReply(false);

    protected boolean restart;

    private ShutdownReply(boolean restart) {
        this.restart = restart;
    }

    public boolean isRestart() {
        return restart;
    }
}
