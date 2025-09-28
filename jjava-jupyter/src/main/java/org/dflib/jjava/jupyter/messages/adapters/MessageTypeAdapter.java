package org.dflib.jjava.jupyter.messages.adapters;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.dflib.jjava.jupyter.messages.MessageType;

import java.lang.reflect.Type;

public class MessageTypeAdapter implements JsonSerializer<MessageType<?>>, JsonDeserializer<MessageType<?>> {
    public static final MessageTypeAdapter INSTANCE = new MessageTypeAdapter();

    private MessageTypeAdapter() { }

    @Override
    public MessageType<?> deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext ctx) throws JsonParseException {
        return MessageType.getType(jsonElement.getAsString());
    }

    @Override
    public JsonElement serialize(MessageType<?> messageType, Type type, JsonSerializationContext ctx) {
        return new JsonPrimitive(messageType.getName());
    }
}
