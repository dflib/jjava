package org.dflib.jjava.jupyter.kernel.comm;

import org.dflib.jjava.jupyter.messages.Message;
import org.dflib.jjava.jupyter.messages.comm.CommOpenCommand;

@FunctionalInterface
public interface CommTarget {
    /**
     * Create a new comm as a result of the frontend making a {@code comm_open} request. This
     * is designed to be a constructor reference to a class that extends {@link Comm} overriding
     * the {@link Comm#onMessage(Message)}.
     *
     * For example a plain no-op handler may be {@code CommTarget noop = Comm::new;}. Which would create
     * comms that do nothing when the receive a message.
     *
     * @param commManager the manager that will be responsible for forwarding messages from
     *                    the frontend
     * @param id          the id for the comm. This will be unique.
     * @param targetName  the name of this target
     * @param msg         the entire message that the manager received commanding it to open the comm. This may
     *                    carry additional data in the messages content. Specifically the {@link
     *                    CommOpenCommand#getData() data field}.
     *
     * @return the newly created comm
     */
    public Comm createComm(CommManager commManager, String id, String targetName, Message<CommOpenCommand> msg);
}
