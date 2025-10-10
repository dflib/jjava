package org.dflib.jjava.jupyter.kernel.comm;

import org.dflib.jjava.jupyter.messages.Message;
import org.dflib.jjava.jupyter.messages.comm.CommOpenCommand;

@FunctionalInterface
public interface CommFactory<T extends Comm> {

    /**
     * Create a new {@link Comm} and optionally attach data to the open message before it is sent.
     *
     * @param manager           the {@link CommManager} that will be responsible for transporting messages
     *                          to and from the created {@link Comm}.
     * @param id                the id of the new {@link Comm}
     * @param target            the name of the target on the front-end to communicate with
     * @param openMessageToSend the message that will be sent after creating the comm. There are 2 places to attach
     *                          additional data to the send
     * @return a new comm. If data must be immediately sent it should be appended to the {@code openMessageToSend}.
     */
    T produce(CommManager manager, String id, String target, Message<CommOpenCommand> openMessageToSend);
}
