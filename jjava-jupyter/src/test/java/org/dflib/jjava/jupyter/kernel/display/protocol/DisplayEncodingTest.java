package org.dflib.jjava.jupyter.kernel.display.protocol;

import org.dflib.jjava.jupyter.kernel.display.DisplayData;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class DisplayEncodingTest {

    @Test
    public void nestedJsonNumbersSurviveRoundTripWithoutPrecisionLoss() {
        long integer = 9007199254740993L;
        BigDecimal decimal = new BigDecimal("12345678901234567890.123456789012345678901234567890");
        DisplayData data = new DisplayData();
        data.putData("application/json", Map.of("integer", integer, "decimal", decimal));

        String line = DisplayProtocol.encode(DisplayRequest.of(DisplayRequest.Operation.DISPLAY, "id", data));
        DisplayData decoded = DisplayProtocol.decode(line).toDisplayData();
        Map<?, ?> numbers = (Map<?, ?>) decoded.getData().get("application/json");

        assertEquals(Long.toString(integer), ((Number) numbers.get("integer")).toString());
        assertEquals(decimal.toString(), ((Number) numbers.get("decimal")).toString());

        String reencoded = DisplayProtocol.encode(DisplayRequest.of(DisplayRequest.Operation.DISPLAY, "id", decoded));
        var json = DisplayProtocol.decode(reencoded).bundle().getAsJsonObject("data")
                .getAsJsonObject("application/json");
        assertEquals(Long.toString(integer), json.get("integer").getAsNumber().toString());
        assertEquals(decimal.toString(), json.get("decimal").getAsNumber().toString());
    }

    @Test
    public void multilineNonAsciiAndPngSurviveRoundTripUnchanged() {
        String multiline = "line one\nline two\r\nend";
        String nonAscii = "héllo — 日本語 🎉";
        String png = Base64.getEncoder().encodeToString(new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0A, 0x1A, 0x0A});

        DisplayData data = new DisplayData(multiline);
        data.putData("text/x-non-ascii", nonAscii);
        data.putData("image/png", png);

        String line = DisplayProtocol.encode(DisplayRequest.of(DisplayRequest.Operation.DISPLAY, "id", data));
        DisplayData decoded = DisplayProtocol.decode(line).toDisplayData();

        assertEquals(multiline, decoded.getData().get("text/plain"));
        assertEquals(nonAscii, decoded.getData().get("text/x-non-ascii"));
        assertEquals(png, decoded.getData().get("image/png"));
    }

    @Test
    public void encodedRequestNeverContainsRawLineFeed() {
        DisplayData data = new DisplayData("a\nb\nc");
        String line = DisplayProtocol.encode(DisplayRequest.of(DisplayRequest.Operation.DISPLAY, "id", data));

        assertFalse(line.contains("\n"), line);
    }
}
