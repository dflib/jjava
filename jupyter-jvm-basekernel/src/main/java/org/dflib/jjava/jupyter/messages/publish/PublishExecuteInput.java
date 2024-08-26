package org.dflib.jjava.jupyter.messages.publish;

import com.google.gson.annotations.SerializedName;
import org.dflib.jjava.jupyter.messages.ContentType;
import org.dflib.jjava.jupyter.messages.MessageType;

public class PublishExecuteInput implements ContentType<PublishExecuteInput> {
    public static final MessageType<PublishExecuteInput> MESSAGE_TYPE = MessageType.PUBLISH_EXECUTE_INPUT;

    @Override
    public MessageType<PublishExecuteInput> getType() {
        return MESSAGE_TYPE;
    }

    /**
     * The code that is currently being executed
     */
    private final String code;

    /**
     * The current execution count
     */
    @SerializedName("execution_count")
    private final int count;

    public PublishExecuteInput(String code, int count) {
        this.code = code;
        this.count = count;
    }

    public String getCode() {
        return code;
    }

    public int getCount() {
        return count;
    }
}
