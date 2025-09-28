package org.dflib.jjava.jupyter.messages;

public interface ReplyType<Req> {
    public MessageType<Req> getRequestType();
}
