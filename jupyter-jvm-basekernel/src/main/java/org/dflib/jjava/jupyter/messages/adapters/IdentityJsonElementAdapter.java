package org.dflib.jjava.jupyter.messages.adapters;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

/**
 * A {@link JsonElement} type adapter that serializes null whether it is enabled on the
 * writer or not. It must be explicitly enabled with the {@link com.google.gson.annotations.JsonAdapter @JsonAdapter}
 * annotation.
 */
public class IdentityJsonElementAdapter extends TypeAdapter<JsonElement> {
    private static final ThreadLocal<Gson> GSON = ThreadLocal.withInitial(() ->
            new GsonBuilder().serializeNulls().create());

    @Override
    public void write(JsonWriter out, JsonElement value) throws IOException {
        if (out.getSerializeNulls()) {
            GSON.get().toJson(value, out);
        } else {
            out.setSerializeNulls(true);
            try {
                GSON.get().toJson(value, out);
            } finally {
                out.setSerializeNulls(false);
            }
        }
    }

    @Override
    public JsonElement read(JsonReader in) throws IOException {
        return GSON.get().fromJson(in, JsonElement.class);
    }
}
