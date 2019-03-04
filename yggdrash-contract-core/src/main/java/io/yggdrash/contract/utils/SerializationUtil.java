package io.yggdrash.contract.utils;

import org.apache.commons.lang3.SerializationUtils;

import java.io.Serializable;

public class SerializationUtil {
    public static byte[] serialize(final Serializable obj) {
        return SerializationUtils.serialize(obj);
    }

    public static <T> T deserialize(final byte[] objectData) {
        return SerializationUtils.deserialize(objectData);
    }
}
