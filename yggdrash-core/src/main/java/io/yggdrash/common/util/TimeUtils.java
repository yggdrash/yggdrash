package io.yggdrash.common.util;

public class TimeUtils {

    private TimeUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static long time() {
        return System.currentTimeMillis();
    }
}
