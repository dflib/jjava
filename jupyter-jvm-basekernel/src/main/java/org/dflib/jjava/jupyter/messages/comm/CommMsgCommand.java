package org.dflib.jjava.jupyter.messages.comm;

import com.google.gson.JsonObject;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import org.dflib.jjava.jupyter.messages.ContentType;
import org.dflib.jjava.jupyter.messages.MessageType;
import org.dflib.jjava.jupyter.messages.adapters.IdentityJsonElementAdapter;

public class CommMsgCommand implements ContentType<CommMsgCommand> {
    public static final MessageType<CommMsgCommand> MESSAGE_TYPE = MessageType.COMM_MSG_COMMAND;

    @Override
    public MessageType<CommMsgCommand> getType() {
        return MESSAGE_TYPE;
    }

    @SerializedName("comm_id")
    protected final String commId;

    @JsonAdapter(IdentityJsonElementAdapter.class)
    protected final JsonObject data;

    public CommMsgCommand(String commId, JsonObject data) {
        this.commId = commId;
        this.data = data;
    }

    public String getCommID() {
        return commId;
    }

    public JsonObject getData() {
        return data;
    }
}
