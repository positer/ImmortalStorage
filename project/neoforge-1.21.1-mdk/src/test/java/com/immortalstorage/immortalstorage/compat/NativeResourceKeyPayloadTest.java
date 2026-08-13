package com.immortalstorage.immortalstorage.compat;

import com.google.gson.JsonParser;
import com.immortalstorage.core.resource.ResourceChannelKey;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

final class NativeResourceKeyPayloadTest {
    @Test
    void roundTripsArbitraryAddonPayloadThroughResourceLocationSafeHex() {
        var payload = JsonParser.parseString("{\"type\":\"addon:mana\",\"key\":{\"id\":\"addon:rune\"}}");
        ResourceChannelKey encoded = NativeResourceKeyPayload.encode("rs_registered", "native", payload);

        assertEquals("rs_registered", encoded.channel());
        assertEquals(payload, NativeResourceKeyPayload.decode(encoded, "rs_registered", "native"));
    }

    @Test
    void rejectsForeignOrMalformedPayloads() {
        assertNull(NativeResourceKeyPayload.decode(
                new ResourceChannelKey("other", "native:00"), "rs_registered", "native"));
        assertNull(NativeResourceKeyPayload.decode(
                new ResourceChannelKey("rs_registered", "native:0"), "rs_registered", "native"));
    }
}
