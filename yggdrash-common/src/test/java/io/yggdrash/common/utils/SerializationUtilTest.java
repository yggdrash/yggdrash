package io.yggdrash.common.utils;

import org.junit.Assert;
import org.junit.Test;

public class SerializationUtilTest {

    @Test
    public void serializeString() {
        String str = "{type:트랜잭션}";
        Assert.assertEquals(11, str.length());
        byte[] data = SerializationUtil.serializeString(str);
        Assert.assertEquals(19, data.length);

        Assert.assertEquals(str, SerializationUtil.deserializeString(data));
    }
}