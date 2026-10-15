package org.dflib.jjava.jupyter.kernel.display.protocol;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.dflib.jjava.jupyter.kernel.display.DisplayData;

import java.util.Map;
import java.util.function.BiConsumer;

public record DisplayRequest(Operation operation, String displayId, JsonObject bundle) {

    public enum Operation {
        DISPLAY,
        UPDATE
    }

    public static DisplayRequest of(Operation operation, String displayId, DisplayData data) {
        return new DisplayRequest(operation, displayId, DisplayProtocol.snapshot(operation, displayId, data));
    }

    public DisplayData toDisplayData() {
        DisplayData out = new DisplayData();
        copy("data", out::putData);
        copy("metadata", out::putMetaData);
        copy("transient", out::putTransientData);
        if (displayId != null) {
            out.setDisplayId(displayId);
        }
        return out;
    }

    private void copy(String section, BiConsumer<String, Object> sink) {
        JsonElement values = bundle.get(section);
        if (values != null && values.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : values.getAsJsonObject().entrySet()) {
                sink.accept(entry.getKey(), DisplayProtocol.GSON.fromJson(entry.getValue(), Object.class));
            }
        }
    }
}
