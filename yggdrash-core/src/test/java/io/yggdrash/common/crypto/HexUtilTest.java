package io.yggdrash.common.crypto;

import org.junit.Assert;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class HexUtilTest {
    private static final String VALID_ADDR = "6c386a4b26f73c802f34673f7248bb118f97424a";

    @Test
    public void testToHexString() {
        assertEquals("", HexUtil.toHexString(null));
    }

    @Test
    public void testHexStringToBytes() {
        {
            String str = "0000";
            byte[] actuals = HexUtil.hexStringToBytes(str);
            byte[] expected = new byte[] {0, 0};
            assertArrayEquals(expected, actuals);
        }
        {
            String str = "0x0000";
            byte[] actuals = HexUtil.hexStringToBytes(str);
            byte[] expected = new byte[] {0, 0};
            assertArrayEquals(expected, actuals);
        }
        {
            String str = "0x45a6";
            byte[] actuals = HexUtil.hexStringToBytes(str);
            byte[] expected = new byte[] {69, -90};
            assertArrayEquals(expected, actuals);
        }
        {
            String str = "1963093cee500c081443e1045c40264b670517af";
            byte[] actuals = HexUtil.hexStringToBytes(str);
            byte[] expected = Hex.decode(str);
            assertArrayEquals(expected, actuals);
        }
        {
            String str = "0x"; // Empty
            byte[] actuals = HexUtil.hexStringToBytes(str);
            byte[] expected = new byte[] {};
            assertArrayEquals(expected, actuals);
        }
        {
            String str = "0"; // Same as 0x00
            byte[] actuals = HexUtil.hexStringToBytes(str);
            byte[] expected = new byte[] {0};
            assertArrayEquals(expected, actuals);
        }
        {
            String str = "0x00"; // This case shouldn't be empty array
            byte[] actuals = HexUtil.hexStringToBytes(str);
            byte[] expected = new byte[] {0};
            assertArrayEquals(expected, actuals);
        }
        {
            String str = "0xd"; // Should work with odd length, adding leading 0
            byte[] actuals = HexUtil.hexStringToBytes(str);
            byte[] expected = new byte[] {13};
            assertArrayEquals(expected, actuals);
        }
        {
            String str = "0xd0d"; // Should work with odd length, adding leading 0
            byte[] actuals = HexUtil.hexStringToBytes(str);
            byte[] expected = new byte[] {13, 13};
            assertArrayEquals(expected, actuals);
        }
    }

    @Test
    public void unifiedNumericToBigInteger() {
        Assert.assertEquals(1, HexUtil.unifiedNumericToBigInteger("1").intValue());
    }

    @Test
    public void isValidAddress() {
        Assert.assertTrue(HexUtil.isValidAddress(HexUtil.addressStringToBytes(VALID_ADDR)));
    }

    @Test
    public void getAddressShortString() {
        Assert.assertEquals("6c386a...", HexUtil.getAddressShortString(HexUtil.addressStringToBytes(VALID_ADDR)));
    }

    @Test
    public void getHashListShort() {
        List<byte[]> hashList = java.util.Arrays.asList("a".getBytes(), "b".getBytes());
        Assert.assertEquals(" 61...62", HexUtil.getHashListShort(hashList));
    }
}