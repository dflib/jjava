package org.dflib.jjava.jupyter.messages.adapters;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.dflib.jjava.jupyter.kernel.ExpressionValue;
import org.dflib.jjava.jupyter.kernel.display.DisplayData;

import java.lang.reflect.Type;

/**
 * Decode/encode an {@link ExpressionValue} as either a {@link ExpressionValue.Error} or {@link ExpressionValue.Success}
 * based on the {@code "status"} field.
 */
public class ExpressionValueAdapter implements JsonSerializer<ExpressionValue>, JsonDeserializer<ExpressionValue> {
    public static final ExpressionValueAdapter INSTANCE = new ExpressionValueAdapter();

    private ExpressionValueAdapter() { }

    @Override
    public ExpressionValue deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext ctx) throws JsonParseException {
        if (jsonElement.isJsonObject()) {
            JsonElement status = jsonElement.getAsJsonObject().get("status");
            if (status != null && status.isJsonPrimitive()
                    && status.getAsString().equalsIgnoreCase("error"))
                return ctx.deserialize(jsonElement, ExpressionValue.Error.class);
        }

        DisplayData data = ctx.deserialize(jsonElement, DisplayData.class);
        return new ExpressionValue.Success(data);
    }

    @Override
    public JsonElement serialize(ExpressionValue exprVal, Type type, JsonSerializationContext ctx) {
        if (exprVal.isSuccess())
            return ctx.serialize(exprVal, ExpressionValue.Success.class);
        else
            return ctx.serialize(exprVal, ExpressionValue.Error.class);
    }
}
