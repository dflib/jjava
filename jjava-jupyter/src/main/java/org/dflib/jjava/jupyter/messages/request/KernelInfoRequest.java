package org.dflib.jjava.jupyter.messages.request;

import org.dflib.jjava.jupyter.messages.ContentType;
import org.dflib.jjava.jupyter.messages.MessageType;
import org.dflib.jjava.jupyter.messages.RequestType;
import org.dflib.jjava.jupyter.messages.reply.KernelInfoReply;

public class KernelInfoRequest implements ContentType<KernelInfoRequest>, RequestType<KernelInfoReply> {
    public static final MessageType<KernelInfoRequest> MESSAGE_TYPE = MessageType.KERNEL_INFO_REQUEST;
    public static final MessageType<KernelInfoReply> REPLY_MESSAGE_TYPE = MessageType.KERNEL_INFO_REPLY;

    @Override
    public MessageType<KernelInfoRequest> getType() {
        return MESSAGE_TYPE;
    }

    @Override
    public MessageType<KernelInfoReply> getReplyType() {
        return REPLY_MESSAGE_TYPE;
    }
}
