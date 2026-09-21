package org.dflib.jjava.jupyter.kernel.display.interactive;

import org.dflib.jjava.jupyter.kernel.comm.Comm;
import org.dflib.jjava.jupyter.kernel.comm.CommManager;
import org.dflib.jjava.jupyter.messages.Message;
import org.dflib.jjava.jupyter.messages.comm.CommCloseCommand;
import org.dflib.jjava.jupyter.messages.comm.CommMsgCommand;

/**
 * The kernel side of the {@value InteractiveSwing#TARGET_NAME} comm. It is opened by the kernel (see
 * {@link InteractiveSwing#show}) and forwards frontend messages to its {@link InteractiveSwingSession}, tearing the
 * session down when the comm closes.
 */
class InteractiveSwingComm extends Comm {

    private final InteractiveSwingSession session;

    InteractiveSwingComm(CommManager manager, String id, String targetName, InteractiveSwingSession session) {
        super(manager, id, targetName);
        this.session = session;
    }

    @Override
    protected void onMessage(Message<CommMsgCommand> message) {
        if (session != null) {
            session.handleMessage(message.getContent().getData());
        }
    }

    @Override
    protected void onClose(Message<CommCloseCommand> closeMessage, boolean sending) {
        if (session != null) {
            session.dispose();
        }
    }
}
