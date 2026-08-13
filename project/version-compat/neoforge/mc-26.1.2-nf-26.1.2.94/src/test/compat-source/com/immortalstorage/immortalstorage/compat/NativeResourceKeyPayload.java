package com.immortalstorage.immortalstorage.compat;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.immortalstorage.core.resource.ResourceChannelKey;

import java.nio.charset.StandardCharsets;

/** Stable resource-location-safe envelope for addon-native storage keys. */
public final class NativeResourceKeyPayload {
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private NativeResourceKeyPayload() {}

    public static ResourceChannelKey encode(String channel, String namespace, JsonElement payload) {
        byte[] bytes = payload.toString().getBytes(StandardCharsets.UTF_8);
        char[] encoded = new char[bytes.length * 2];
        for (int index = 0; index < bytes.length; index++) {
            int value = bytes[index] & 0xff;
            encoded[index * 2] = HEX[value >>> 4];
            encoded[index * 2 + 1] = HEX[value & 0x0f];
        }
        return new ResourceChannelKey(channel, namespace + ":" + new String(encoded));
    }

    public static JsonElement decode(ResourceChannelKey key, String channel, String namespace) {
        if (key == null || !channel.equals(key.channel())) return null;
        String prefix = namespace + ":";
        if (!key.resourceId().startsWith(prefix)) return null;
        String encoded = key.resourceId().substring(prefix.length());
        if ((encoded.length() & 1) != 0) return null;
        byte[] bytes = new byte[encoded.length() / 2];
        for (int index = 0; index < bytes.length; index++) {
            int high = Character.digit(encoded.charAt(index * 2), 16);
            int low = Character.digit(encoded.charAt(index * 2 + 1), 16);
            if (high < 0 || low < 0) return null;
            bytes[index] = (byte) ((high << 4) | low);
        }
        try {
            return JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8));
        } catch (RuntimeException malformed) {
            return null;
        }
    }
}
