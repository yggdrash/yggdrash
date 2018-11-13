package io.yggdrash.common.util;

import org.spongycastle.util.encoders.Hex;

public class TimeUtils {
    public static long time() {
        return System.currentTimeMillis();
    }

    public static String hexTime() {
        byte[] time = ByteUtil.longToBytes(time());
        return Hex.toHexString(time);
    }
}
