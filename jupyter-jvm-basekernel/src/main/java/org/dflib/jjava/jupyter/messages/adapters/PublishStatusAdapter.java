package org.dflib.jjava.jupyter.messages.adapters;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import org.dflib.jjava.jupyter.messages.publish.PublishStatus;

import java.lang.reflect.Type;

public class PublishStatusAdapter implements JsonDeserializer<PublishStatus> {
    public static final PublishStatusAdapter INSTANCE = new PublishStatusAdapter();

    private PublishStatusAdapter() { }

    @Override
    public PublishStatus deserialize(JsonElement element, Type type, JsonDeserializationContext ctx) throws JsonParseException {
        PublishStatus.State state = ctx.deserialize(element.getAsJsonObject().get("execution_result"), PublishStatus.State.class);
        switch (state) {
            case BUSY: return PublishStatus.BUSY;
            case IDLE: return PublishStatus.IDLE;
            case STARTING: return PublishStatus.STARTING;
            default: return null;
        }
    }
}
