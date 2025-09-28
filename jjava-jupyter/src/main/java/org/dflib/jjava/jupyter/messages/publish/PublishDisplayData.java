package org.dflib.jjava.jupyter.messages.publish;

import org.dflib.jjava.jupyter.kernel.display.DisplayData;
import org.dflib.jjava.jupyter.messages.ContentType;
import org.dflib.jjava.jupyter.messages.MessageType;

public class PublishDisplayData extends DisplayData implements ContentType<PublishDisplayData> {
    public static final MessageType<PublishDisplayData> MESSAGE_TYPE = MessageType.PUBLISH_DISPLAY_DATA;

    @Override
    public MessageType<PublishDisplayData> getType() {
        return MESSAGE_TYPE;
    }

    public PublishDisplayData(DisplayData data) {
        super(data);
    }
}
