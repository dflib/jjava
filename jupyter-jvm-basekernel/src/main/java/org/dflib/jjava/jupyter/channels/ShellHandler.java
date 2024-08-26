package org.dflib.jjava.jupyter.channels;

import org.dflib.jjava.jupyter.messages.Message;

@FunctionalInterface
public interface ShellHandler<T> {
    public void handle(ShellReplyEnvironment env, Message<T> message);
}
