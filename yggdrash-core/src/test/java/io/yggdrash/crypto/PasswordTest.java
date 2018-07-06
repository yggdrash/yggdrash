package io.yggdrash.crypto;

import io.yggdrash.util.ByteUtil;
import org.junit.Test;

import static io.yggdrash.crypto.Password.generateKeyDerivation;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PasswordTest {

    @Test
    public void testPasswordCheck() {

        String badpass1 = "123456789012";
        String badpass2 = "abcdefghijklm";
        String badpass3 = "Ab^45678901";
        String badpass4 = "Ab^45678901234567890123456789012ddddddddddd";
        String badpass5 = "Ab345678901234567890123456789012";
        String badpass6 = "123456789012345678901234567890123";

        String okpass1 = "Ab^4567890123dddddddd";

        assertFalse(Password.passwordValid(badpass1));
        assertFalse(Password.passwordValid(badpass2));
        assertFalse(Password.passwordValid(badpass3));
        assertFalse(Password.passwordValid(badpass4));
        assertFalse(Password.passwordValid(badpass5));
        assertFalse(Password.passwordValid(badpass6));

        assertTrue(Password.passwordValid(okpass1));
    }


    @Test
    public void testGenerateKeyDerivation() {
        byte[] kdfData;
        kdfData = generateKeyDerivation("testdata".getBytes(), 32);
        System.out.println(ByteUtil.toHexString(kdfData));
        assertArrayEquals(kdfData, ByteUtil.hexStringToBytes("0cc2fac56bbf672b4f6922d8938d62a0eb590efe9acfac00bd0fa771f2bf42c7"));
    }


}
