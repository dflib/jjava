package org.dflib.jjava.jupyter.messages;

public interface RequestType<Rep> {
    public MessageType<Rep> getReplyType();
}
