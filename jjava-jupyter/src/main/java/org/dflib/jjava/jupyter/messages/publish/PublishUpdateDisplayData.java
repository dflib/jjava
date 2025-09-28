package org.dflib.jjava.jupyter.messages.publish;

import org.dflib.jjava.jupyter.kernel.display.DisplayData;
import org.dflib.jjava.jupyter.messages.ContentType;
import org.dflib.jjava.jupyter.messages.MessageType;

public class PublishUpdateDisplayData extends DisplayData implements ContentType<PublishUpdateDisplayData> {
    public static final MessageType<PublishUpdateDisplayData> MESSAGE_TYPE = MessageType.PUBLISH_UPDATE_DISPLAY_DATA;

    @Override
    public MessageType<PublishUpdateDisplayData> getType() {
        return MESSAGE_TYPE;
    }

    public PublishUpdateDisplayData(DisplayData data) {
        super(data);

        if (!data.hasDisplayId())
            throw new IllegalArgumentException("In order to update a display, the data must have a display_id.");
    }
}
