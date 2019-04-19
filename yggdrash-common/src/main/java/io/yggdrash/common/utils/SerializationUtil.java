package io.yggdrash.common.utils;

import com.google.gson.JsonElement;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class SerializationUtil {
    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    private SerializationUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static byte[] serializeJson(JsonElement json) {
        return json.toString().getBytes(DEFAULT_CHARSET);
    }

    public static byte[] serializeString(String string) {
        return string.getBytes(DEFAULT_CHARSET);
    }

    public static String deserializeString(byte[] data) {
        return new String(data, DEFAULT_CHARSET);
    }
}
