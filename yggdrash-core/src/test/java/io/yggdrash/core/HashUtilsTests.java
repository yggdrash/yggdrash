package io.yggdrash.core;

import org.junit.Test;

import java.security.NoSuchAlgorithmException;

public class HashUtilsTests {
    @Test
    public void test() {
        try {
            String test = HashUtils.sha256Hex("test");
            System.out.println(test);
            System.out.println(HashUtils.sha256Base64("test"));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }
}
