package org.dflib.jjava.jupyter.messages.adapters;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.dflib.jjava.jupyter.kernel.history.HistoryEntry;

import java.lang.reflect.Type;

public class HistoryEntryAdapter implements JsonSerializer<HistoryEntry> {
    public static final HistoryEntryAdapter INSTANCE = new HistoryEntryAdapter();

    private HistoryEntryAdapter() { }

    @Override
    public JsonElement serialize(HistoryEntry src, Type type, JsonSerializationContext ctx) {
        JsonArray tuple = new JsonArray();

        tuple.add(src.getSession());
        tuple.add(src.getCellNumber());

        if (src.hasOutput()) {
            JsonArray ioPair = new JsonArray();

            ioPair.add(src.getInput());
            ioPair.add(src.getOutput());

            tuple.add(ioPair);
        } else {
            tuple.add(src.getInput());
        }

        return tuple;
    }
}
