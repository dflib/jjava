package org.dflib.jjava.jupyter.messages.reply;

import com.google.gson.annotations.SerializedName;
import org.dflib.jjava.jupyter.messages.ContentType;
import org.dflib.jjava.jupyter.messages.MessageType;
import org.dflib.jjava.jupyter.messages.ReplyType;
import org.dflib.jjava.jupyter.messages.request.CommInfoRequest;

import java.util.Map;

public class CommInfoReply implements ContentType<CommInfoReply>, ReplyType<CommInfoRequest> {
    public static final MessageType<CommInfoReply> MESSAGE_TYPE = MessageType.COMM_INFO_REPLY;
    public static final MessageType<CommInfoRequest> REQUEST_MESSAGE_TYPE = MessageType.COMM_INFO_REQUEST;

    @Override
    public MessageType<CommInfoReply> getType() {
        return MESSAGE_TYPE;
    }

    @Override
    public MessageType<CommInfoRequest> getRequestType() {
        return REQUEST_MESSAGE_TYPE;
    }

    public static class CommInfo {
        @SerializedName("target_name")
        protected final String targetName;

        public CommInfo(String targetName) {
            this.targetName = targetName;
        }

        public String getTargetName() {
            return targetName;
        }
    }

    /**
     * A map of uuid to target_name for the comms
     */
    protected final Map<String, CommInfo> comms;

    public CommInfoReply(Map<String, CommInfo> comms) {
        this.comms = comms;
    }

    public Map<String, CommInfo> getComms() {
        return comms;
    }
}
