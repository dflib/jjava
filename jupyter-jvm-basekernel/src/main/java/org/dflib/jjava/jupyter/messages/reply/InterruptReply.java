package org.dflib.jjava.jupyter.messages.reply;

import org.dflib.jjava.jupyter.messages.ContentType;
import org.dflib.jjava.jupyter.messages.MessageType;
import org.dflib.jjava.jupyter.messages.ReplyType;
import org.dflib.jjava.jupyter.messages.request.InterruptRequest;

public class InterruptReply implements ContentType<InterruptReply>, ReplyType<InterruptRequest> {
    public static final MessageType<InterruptReply> MESSAGE_TYPE = MessageType.INTERRUPT_REPLY;
    public static final MessageType<InterruptRequest> REQUEST_MESSAGE_TYPE = MessageType.INTERRUPT_REQUEST;

    @Override
    public MessageType<InterruptReply> getType() {
        return MESSAGE_TYPE;
    }

    @Override
    public MessageType<InterruptRequest> getRequestType() {
        return REQUEST_MESSAGE_TYPE;
    }
}
