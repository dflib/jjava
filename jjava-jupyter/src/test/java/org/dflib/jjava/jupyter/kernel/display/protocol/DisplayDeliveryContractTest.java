package org.dflib.jjava.jupyter.kernel.display.protocol;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.dflib.jjava.jupyter.kernel.display.DisplayData;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.dflib.jjava.jupyter.kernel.display.protocol.DisplayDeliveryException.Kind.REJECTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contract every delivery must satisfy, shared by the transports and the in-memory substitute. Transport-specific
 * behaviour (unavailable endpoint, stalled peer) is tested in the transport's own test classes.
 */
public abstract class DisplayDeliveryContractTest {

    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    protected abstract DisplayDelivery delivery(DisplayRequestHandler handler);

    @Test
    public void bundleSurvivesDelivery() {
        List<DisplayRequest> received = new ArrayList<>();
        DisplayData sent = bundle();

        delivery(received::add).deliver(DisplayRequest.of(DisplayRequest.Operation.DISPLAY, "id-1", sent));

        DisplayRequest request = received.get(0);
        assertEquals("id-1", request.displayId());
        DisplayData got = request.toDisplayData();
        assertEquals(GSON.toJsonTree(sent.getData()), GSON.toJsonTree(got.getData()));
        assertEquals(GSON.toJsonTree(sent.getMetadata()), GSON.toJsonTree(got.getMetadata()));
        assertEquals("id-1", got.getDisplayId());
    }

    @Test
    public void updateCarriesTargetId() {
        List<DisplayRequest> received = new ArrayList<>();
        DisplayData data = new DisplayData("updated");
        data.putTransientData("display_id", "other");

        delivery(received::add).deliver(DisplayRequest.of(DisplayRequest.Operation.UPDATE, "target", data));

        DisplayRequest request = received.get(0);
        assertEquals(DisplayRequest.Operation.UPDATE, request.operation());
        assertEquals("target", request.displayId());
        assertEquals("target", request.toDisplayData().getDisplayId());
    }

    @Test
    public void sequentialOperationsKeepOrder() {
        List<String> ids = new ArrayList<>();
        DisplayDelivery delivery = delivery(r -> ids.add(r.displayId()));

        List<String> expected = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String id = String.valueOf(i);
            expected.add(id);
            delivery.deliver(DisplayRequest.of(DisplayRequest.Operation.DISPLAY, id, new DisplayData(id)));
        }
        assertEquals(expected, ids);
    }

    @Test
    public void handlerFailureIsRejectedWithItsMessage() {
        DisplayDelivery delivery = delivery(r -> {
            throw new IllegalStateException("boom");
        });
        DisplayRequest request = DisplayRequest.of(DisplayRequest.Operation.DISPLAY, "x", new DisplayData("t"));

        DisplayDeliveryException e = assertThrows(
                DisplayDeliveryException.class,
                () -> delivery.deliver(request));
        assertEquals(REJECTED, e.kind());
        assertTrue(e.getMessage().contains("boom"));
    }

    private static DisplayData bundle() {
        DisplayData data = new DisplayData("portable");
        data.putHTML("<b>portable</b>");
        data.putData("image/png", "AQID");

        Map<String, Object> json = new LinkedHashMap<>();
        json.put("values", Arrays.asList("kept", null));
        json.put("nothing", null);
        data.putData("application/json", json);

        data.putMetaData("render", Map.of("expanded", "yes"));
        data.putTransientData("custom", "preserved");
        return data;
    }
}
