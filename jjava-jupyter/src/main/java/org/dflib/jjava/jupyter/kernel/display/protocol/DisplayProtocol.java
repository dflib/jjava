package org.dflib.jjava.jupyter.kernel.display.protocol;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.ToNumberPolicy;
import org.dflib.jjava.jupyter.kernel.display.DisplayData;

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Transport-independent codec and portability rules of the cross-JVM display protocol. A request is one JSON object;
 * a response is one token: {@code ok}, or a JSON string with the failure message. Nothing here depends on how the
 * bytes travel.
 */
public final class DisplayProtocol {

    static final Gson GSON = new GsonBuilder()
            .serializeNulls()
            .setObjectToNumberStrategy(ToNumberPolicy.LAZILY_PARSED_NUMBER)
            .create();

    private static final String OK = "ok";
    private static final String ACTION = "action";
    private static final String TRANSIENT = "transient";

    private DisplayProtocol() {
    }

    static JsonObject snapshot(DisplayRequest.Operation operation, String displayId, DisplayData data) {
        if (displayId == null) {
            throw new IllegalArgumentException(operation == DisplayRequest.Operation.UPDATE
                    ? "updateDisplay requires a non-null id"
                    : "display requires a non-null id");
        }

        Set<Object> path = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
        requirePortable(data.getData(), path);
        requirePortable(data.getMetadata(), path);
        requirePortable(data.getTransientData(), path);

        JsonObject bundle = new JsonObject();
        bundle.add("data", GSON.toJsonTree(data.getData()));
        bundle.add("metadata", GSON.toJsonTree(data.getMetadata()));
        bundle.add(TRANSIENT, GSON.toJsonTree(data.getTransientData()));
        if (operation == DisplayRequest.Operation.DISPLAY) {
            bundle.getAsJsonObject(TRANSIENT).addProperty("display_id", displayId);
        }
        return bundle;
    }

    public static String encode(DisplayRequest request) {
        JsonObject message = new JsonObject();
        if (request.operation() == DisplayRequest.Operation.UPDATE) {
            message.addProperty(ACTION, "updateDisplay");
            message.addProperty("id", request.displayId());
        } else {
            message.addProperty(ACTION, "display");
        }
        message.add("bundle", request.bundle());
        return GSON.toJson(message);
    }

    public static DisplayRequest decode(String line) {
        JsonObject message;
        try {
            message = GSON.fromJson(line, JsonObject.class);
        } catch (JsonParseException e) {
            throw new IllegalArgumentException("Display request must be a JSON object", e);
        }
        if (message == null) {
            throw new IllegalArgumentException("Display request must be a JSON object");
        }

        JsonElement bundle = message.get("bundle");
        if (bundle == null || !bundle.isJsonObject()) {
            throw new IllegalArgumentException("Display bundle must not be null");
        }

        JsonElement action = message.get(ACTION);
        String name = action != null && action.isJsonPrimitive() ? action.getAsString() : null;
        if ("display".equals(name)) {
            return new DisplayRequest(
                    DisplayRequest.Operation.DISPLAY,
                    transientDisplayId(bundle.getAsJsonObject()),
                    bundle.getAsJsonObject());
        }
        if ("updateDisplay".equals(name)) {
            JsonElement id = message.get("id");
            if (id == null || id.isJsonNull()) {
                throw new IllegalArgumentException("updateDisplay requires a non-null id");
            }
            return new DisplayRequest(DisplayRequest.Operation.UPDATE, id.getAsString(), bundle.getAsJsonObject());
        }
        throw new IllegalArgumentException("Unsupported display action: " + name);
    }

    /**
     * Returns the response token for a handled request: {@code null} means success.
     */
    public static String response(RuntimeException failure) {
        if (failure == null) {
            return OK;
        }
        String message = failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
        return GSON.toJson(message);
    }

    /**
     * Interprets a response token; throws unless it is {@code ok}.
     */
    public static void checkResponse(String response) {
        if (response == null) {
            throw new DisplayDeliveryException(
                    DisplayDeliveryException.Kind.UNCERTAIN, "Display callback closed without a response", null);
        }
        if (OK.equals(response)) {
            return;
        }

        String message;
        try {
            message = GSON.fromJson(response, String.class);
        } catch (JsonParseException e) {
            message = response;
        }
        throw new DisplayDeliveryException(
                DisplayDeliveryException.Kind.REJECTED,
                "Display callback failed: " + (message == null ? response : message),
                null);
    }

    private static String transientDisplayId(JsonObject bundle) {
        JsonElement transientData = bundle.get(TRANSIENT);
        JsonElement id = transientData != null && transientData.isJsonObject()
                ? transientData.getAsJsonObject().get("display_id")
                : null;
        return id != null && id.isJsonPrimitive() ? id.getAsString() : null;
    }

    private static void requirePortable(Object value, Set<Object> path) {
        if (value == null || value instanceof CharSequence || value instanceof Boolean || value instanceof Character) {
            return;
        }
        if (value instanceof Number number) {
            requireFinite(number);
            return;
        }

        enter(value, path);
        requireContainer(value, path);
        path.remove(value);
    }

    private static void requireContainer(Object value, Set<Object> path) {
        if (value instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                requireStringKey(entry.getKey());
                requirePortable(entry.getValue(), path);
            }
        } else if (value instanceof Iterable<?> items) {
            for (Object item : items) {
                requirePortable(item, path);
            }
        } else if (value.getClass().isArray()) {
            for (int i = 0; i < Array.getLength(value); i++) {
                requirePortable(Array.get(value, i), path);
            }
        } else {
            throw new IllegalArgumentException("Non-portable display value: " + value.getClass().getName());
        }
    }

    private static void requireStringKey(Object key) {
        if (!(key instanceof String)) {
            throw new IllegalArgumentException("Non-portable display value: "
                    + (key == null ? "null" : key.getClass().getName()));
        }
    }

    private static void requireFinite(Number number) {
        if (isNonFinite(number)) {
            throw new IllegalArgumentException("Non-portable display value: non-finite number");
        }
    }

    private static void enter(Object container, Set<Object> path) {
        if (!path.add(container)) {
            throw new IllegalArgumentException("Non-portable display value: cyclic structure");
        }
    }

    private static boolean isNonFinite(Number number) {
        if (number instanceof Double d) {
            return !Double.isFinite(d);
        }
        if (number instanceof Float f) {
            return !Float.isFinite(f);
        }
        return false;
    }
}
