package io.yggdrash.util;

public class SerializeUtils {

    public static byte[] serialize(Object obj) {

        return obj.toString().getBytes();
    }


}
