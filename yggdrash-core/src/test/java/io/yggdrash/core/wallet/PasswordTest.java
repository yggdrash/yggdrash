package io.yggdrash.core.wallet;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PasswordTest {

    private static final Logger log = LoggerFactory.getLogger(PasswordTest.class);

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

        for (int i = 0x21; i <= 0x7e; i++) {
            if (i == 0x30) {
                i = 0x3a;
            } else if (i == 0x41) {
                i = 0x5b;
            } else if (i == 0x61) {
                i = 0x7b;
            }

            assertTrue(Password.passwordValid(sample + (char) i));
            log.debug("password test ok: " + sample + (char)i);
        }

    }
}
