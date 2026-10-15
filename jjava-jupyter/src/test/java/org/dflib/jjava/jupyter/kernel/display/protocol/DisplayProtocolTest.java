package org.dflib.jjava.jupyter.kernel.display.protocol;

import org.dflib.jjava.jupyter.kernel.display.DisplayData;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DisplayProtocolTest {

    @Test
    public void roundTripsDisplayAndUpdate() {
        DisplayData data = new DisplayData("hello");
        data.putHTML("<b>hi</b>");

        DisplayRequest display = DisplayRequest.of(DisplayRequest.Operation.DISPLAY, "id-1", data);
        DisplayRequest decodedDisplay = DisplayProtocol.decode(DisplayProtocol.encode(display));
        assertEquals(DisplayRequest.Operation.DISPLAY, decodedDisplay.operation());
        assertEquals("id-1", decodedDisplay.displayId());
        assertEquals("<b>hi</b>", decodedDisplay.toDisplayData().getData().get("text/html"));

        DisplayRequest update = DisplayRequest.of(DisplayRequest.Operation.UPDATE, "id-1", data);
        DisplayRequest decodedUpdate = DisplayProtocol.decode(DisplayProtocol.encode(update));
        assertEquals(DisplayRequest.Operation.UPDATE, decodedUpdate.operation());
        assertEquals("id-1", decodedUpdate.displayId());
    }

    @Test
    public void rejectsCyclicStructure() {
        List<Object> cycle = new ArrayList<>();
        cycle.add(cycle);
        DisplayData data = new DisplayData();
        data.putData("application/json", cycle);

        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> DisplayRequest.of(DisplayRequest.Operation.DISPLAY, "id", data));
        assertTrue(e.getMessage().contains("cyclic structure"));
    }

    @Test
    public void rejectsNonFiniteNumbers() {
        for (double value : new double[]{Double.NaN, Double.POSITIVE_INFINITY}) {
            DisplayData data = new DisplayData();
            data.putData("application/json", value);
            IllegalArgumentException e = assertThrows(
                    IllegalArgumentException.class,
                    () -> DisplayRequest.of(DisplayRequest.Operation.DISPLAY, "id", data));
            assertTrue(e.getMessage().contains("non-finite number"));
        }
    }

    @Test
    public void rejectsNonStringMapKeys() {
        Map<Object, Object> map = new LinkedHashMap<>();
        map.put(1, "bad");
        DisplayData data = new DisplayData();
        data.putData("application/json", map);

        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> DisplayRequest.of(DisplayRequest.Operation.DISPLAY, "id", data));
        assertTrue(e.getMessage().contains("Non-portable display value"));
    }

    @Test
    public void rejectsUpdateWithoutId() {
        DisplayData data = new DisplayData("x");
        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> DisplayRequest.of(DisplayRequest.Operation.UPDATE, null, data));
        assertEquals("updateDisplay requires a non-null id", e.getMessage());
    }

    @Test
    public void rejectsUnknownActionAndMalformedJson() {
        assertThrows(IllegalArgumentException.class,
                () -> DisplayProtocol.decode("{\"action\":\"explode\",\"bundle\":{}}"));
        assertThrows(IllegalArgumentException.class, () -> DisplayProtocol.decode("not json"));
    }
}
