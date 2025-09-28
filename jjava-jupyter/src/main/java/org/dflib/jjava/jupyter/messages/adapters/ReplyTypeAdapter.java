package org.dflib.jjava.jupyter.messages.adapters;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import org.dflib.jjava.jupyter.messages.ReplyType;
import org.dflib.jjava.jupyter.messages.reply.ErrorReply;

import java.lang.reflect.Type;

public class ReplyTypeAdapter implements JsonDeserializer<ReplyType<?>> {
    private final Gson replyGson;

    /**
     * <b>Important:</b> the given instance must <b>not</b> have this type
     * adapter registered or deserialization with this deserializer will
     * cause a stack overflow exception.
     *
     * @param replyGson the gson instance to use when deserializing replies.
     */
    public ReplyTypeAdapter(Gson replyGson) {
        this.replyGson = replyGson;
    }

    @Override
    public ReplyType<?> deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext ctx) throws JsonParseException {
        // If the reply is an error, decode as an ErrorReply instead of the content type
        if (jsonElement.isJsonObject()) {
            JsonElement status = jsonElement.getAsJsonObject().get("status");
            if (status != null && status.isJsonPrimitive()
                    && status.getAsString().equalsIgnoreCase("error"))
                return this.replyGson.fromJson(jsonElement, ErrorReply.class);
        }

        return this.replyGson.fromJson(jsonElement, type);
    }
}
