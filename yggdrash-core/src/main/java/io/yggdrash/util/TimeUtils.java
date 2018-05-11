package io.yggdrash.util;

public class TimeUtils {

    public static long getCurrenttime() {
        return System.currentTimeMillis() / 1000;
    }

    public static long time() {
        return System.nanoTime();
    }
}
