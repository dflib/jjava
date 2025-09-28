package org.dflib.jjava.jupyter.messages.publish;

import com.google.gson.annotations.SerializedName;
import org.dflib.jjava.jupyter.kernel.display.DisplayData;
import org.dflib.jjava.jupyter.messages.ContentType;
import org.dflib.jjava.jupyter.messages.MessageType;

public class PublishExecuteResult extends DisplayData implements ContentType<PublishExecuteResult> {
    public static final MessageType<PublishExecuteResult> MESSAGE_TYPE = MessageType.PUBLISH_EXECUTION_RESULT;

    @Override
    public MessageType<PublishExecuteResult> getType() {
        return MESSAGE_TYPE;
    }

    @SerializedName("execution_count")
    private final int count;

    public PublishExecuteResult(int count, DisplayData data) {
        super(data);
        this.count = count;
    }

    public int getCount() {
        return count;
    }
}
