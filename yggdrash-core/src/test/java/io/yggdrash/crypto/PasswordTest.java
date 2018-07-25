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
        assertFalse(Password.passwordValid(badpass1));

        String badpass2 = "abcdefghijklm";
        assertFalse(Password.passwordValid(badpass2));

        String badpass3 = "Ab^45678901";
        assertFalse(Password.passwordValid(badpass3));

        String badpass4 = "Ab^45678901234567890123456789012ddddddddddd";
        assertFalse(Password.passwordValid(badpass4));

        String badpass5 = "Ab345678901234567890123456789012";
        assertFalse(Password.passwordValid(badpass5));

        String badpass6 = "123456789012345678901234567890123";
        assertFalse(Password.passwordValid(badpass6));

        String badpass7 = "Ab!12345678901234567890 ";
        assertFalse(Password.passwordValid(badpass7));


        String okpass1 = "Ab^4567890123dddddddd";
        assertTrue(Password.passwordValid(okpass1));

        String okpass2 = "Ab!4567890123dddddddd";
        assertTrue(Password.passwordValid(okpass2));

        String okpass3 = "Ab#4567890123dddddddd";
        assertTrue(Password.passwordValid(okpass3));

        String okpass4 = "Ab$4567890123dddddddd";
        assertTrue(Password.passwordValid(okpass4));

        String okpass5 = "Ab~4567890123dddddddd";
        assertTrue(Password.passwordValid(okpass5));

        String okpass6 = "Ab&4567890123dddddddd";
        assertTrue(Password.passwordValid(okpass6));

        String okpass7 = "Ab*4567890123dddddddd";
        assertTrue(Password.passwordValid(okpass7));

        String okpass8 = "Ab(4567890123dddddddd";
        assertTrue(Password.passwordValid(okpass8));

        String okpass9 = "Ab)4567890123dddddddd";
        assertTrue(Password.passwordValid(okpass9));

        String okpass10 = "Ab-4567890123dddddddd";
        assertTrue(Password.passwordValid(okpass10));

        String sample = "Aa123456789";

        for(int i=0x21 ; i <= 0x7e ; i++) {
            if(i == 0x30) {
                i = 0x3a;
            } else if (i == 0x41) {
                i = 0x5b;
            } else if (i == 0x61) {
                i = 0x7b;
            }

            assertTrue(Password.passwordValid(sample+(char)i));
            System.out.println("password test ok: " + sample + (char)i);
        }

    }


    @Test
    public void testGenerateKeyDerivation() {
        byte[] kdfData;
        kdfData = generateKeyDerivation("testdata".getBytes(), 32);
        System.out.println(ByteUtil.toHexString(kdfData));
        assertArrayEquals(kdfData, ByteUtil.hexStringToBytes("0cc2fac56bbf672b4f6922d8938d62a0eb590efe9acfac00bd0fa771f2bf42c7"));
    }


}
