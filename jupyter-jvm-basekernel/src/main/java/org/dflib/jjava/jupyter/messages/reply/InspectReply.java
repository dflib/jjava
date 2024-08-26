package org.dflib.jjava.jupyter.messages.reply;

import org.dflib.jjava.jupyter.kernel.display.DisplayData;
import org.dflib.jjava.jupyter.messages.ContentType;
import org.dflib.jjava.jupyter.messages.MessageType;
import org.dflib.jjava.jupyter.messages.ReplyType;
import org.dflib.jjava.jupyter.messages.request.InspectRequest;

public class InspectReply extends DisplayData implements ContentType<InspectReply>, ReplyType<InspectRequest> {
    public static final MessageType<InspectReply> MESSAGE_TYPE = MessageType.INSPECT_REPLY;
    public static final MessageType<InspectRequest> REQUEST_MESSAGE_TYPE = MessageType.INSPECT_REQUEST;

    @Override
    public MessageType<InspectReply> getType() {
        return MESSAGE_TYPE;
    }

    @Override
    public MessageType<InspectRequest> getRequestType() {
        return REQUEST_MESSAGE_TYPE;
    }

    protected final String status = "ok";
    protected final boolean found;

    public InspectReply(boolean found, DisplayData data) {
        super(data);
        this.found = found;
    }

    public String getStatus() {
        return status;
    }

    public boolean isFound() {
        return found;
    }
}
