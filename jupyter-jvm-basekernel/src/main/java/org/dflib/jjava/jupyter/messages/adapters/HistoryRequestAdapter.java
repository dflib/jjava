package org.dflib.jjava.jupyter.messages.adapters;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import org.dflib.jjava.jupyter.messages.request.HistoryRequest;

import java.lang.reflect.Type;

public class HistoryRequestAdapter implements JsonDeserializer<HistoryRequest> {
    public static final HistoryRequestAdapter INSTANCE = new HistoryRequestAdapter();

    private HistoryRequestAdapter() { }

    @Override
    public HistoryRequest deserialize(JsonElement element, Type type, JsonDeserializationContext ctx) throws JsonParseException {
        JsonObject object = element.getAsJsonObject();
        JsonPrimitive accessTypeRaw = object.getAsJsonPrimitive("hist_access_type");

        HistoryRequest.AccessType accessType = ctx.deserialize(accessTypeRaw, HistoryRequest.AccessType.class);
        switch (accessType) {
            case RANGE:
                return ctx.deserialize(element, HistoryRequest.Range.class);
            case TAIL:
                return ctx.deserialize(element, HistoryRequest.Tail.class);
            case SEARCH:
                return ctx.deserialize(element, HistoryRequest.Search.class);
            default:
                throw new IllegalArgumentException("Unknown hist_access_type " + String.valueOf(accessTypeRaw));
        }
    }
}
