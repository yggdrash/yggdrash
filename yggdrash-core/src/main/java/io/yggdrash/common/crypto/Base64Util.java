package io.yggdrash.common.crypto;

import org.apache.commons.codec.binary.Base64;

public class Base64Util {
    public static String encodeBase64(String val) {
        return new String(Base64.encodeBase64(val.getBytes()));
    }

    public static String encodeBase64(byte[] val) {
        return new String(Base64.encodeBase64(val));
    }
}
