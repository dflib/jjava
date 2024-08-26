package org.dflib.jjava.jupyter.messages.publish;

import org.dflib.jjava.jupyter.messages.ContentType;
import org.dflib.jjava.jupyter.messages.MessageType;

public class PublishClearOutput implements ContentType<PublishClearOutput> {
    public static final MessageType<PublishClearOutput> MESSAGE_TYPE = MessageType.PUBLISH_CLEAR_OUTPUT;

    @Override
    public MessageType<PublishClearOutput> getType() {
        return MESSAGE_TYPE;
    }

    public static final PublishClearOutput NOW = new PublishClearOutput(false);
    public static final PublishClearOutput BEFORE_NEXT_OUTPUT = new PublishClearOutput(true);

    /**
     * Wait to clear the output until the
     */
    private final boolean wait;

    private PublishClearOutput(boolean wait) {
        this.wait = wait;
    }

    public boolean shouldWait() {
        return wait;
    }
}
