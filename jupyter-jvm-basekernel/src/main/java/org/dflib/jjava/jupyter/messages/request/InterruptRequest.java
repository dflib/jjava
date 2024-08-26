package org.dflib.jjava.jupyter.messages.request;

import org.dflib.jjava.jupyter.messages.ContentType;
import org.dflib.jjava.jupyter.messages.MessageType;
import org.dflib.jjava.jupyter.messages.RequestType;
import org.dflib.jjava.jupyter.messages.reply.InterruptReply;

public class InterruptRequest implements ContentType<InterruptRequest>, RequestType<InterruptReply> {
    public static final MessageType<InterruptRequest> MESSAGE_TYPE = MessageType.INTERRUPT_REQUEST;
    public static final MessageType<InterruptReply> REPLY_MESSAGE_TYPE = MessageType.INTERRUPT_REPLY;

    @Override
    public MessageType<InterruptRequest> getType() {
        return MESSAGE_TYPE;
    }

    @Override
    public MessageType<InterruptReply> getReplyType() {
        return REPLY_MESSAGE_TYPE;
    }
}
