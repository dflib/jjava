package org.dflib.jjava.jupyter.messages;

public interface ContentType<T> {
    public MessageType<T> getType();
}
