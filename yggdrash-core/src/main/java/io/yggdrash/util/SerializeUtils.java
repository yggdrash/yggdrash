package io.yggdrash.util;

import java.nio.charset.Charset;

public class SerializeUtils {
    private static final Charset CHARSET = Charset.defaultCharset();

    private SerializeUtils() {
    }

    public static byte[] serialize(Object obj) {
        return obj.toString().getBytes(CHARSET);
    }

}
