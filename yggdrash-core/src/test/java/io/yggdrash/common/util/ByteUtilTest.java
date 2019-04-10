/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.common.util;

import io.yggdrash.common.utils.ByteUtil;
import org.junit.Assert;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static io.yggdrash.common.utils.ByteUtil.EMPTY_BYTE_ARRAY;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ByteUtilTest {

    @Test
    public void testAppendByte() {
        byte[] bytes = "tes".getBytes();
        byte b = 0x74;
        Assert.assertArrayEquals("test".getBytes(), ByteUtil.appendByte(bytes, b));
    }

    @Test
    public void testBigIntegerToBytes() {
        byte[] expecteds = new byte[] {(byte) 0xff, (byte) 0xec, 0x78};
        BigInteger b = BigInteger.valueOf(16772216);
        byte[] actuals = ByteUtil.bigIntegerToBytes(b);
        assertArrayEquals(expecteds, actuals);
    }

    @Test
    public void testBigIntegerToBytesSign() {
        {
            BigInteger b = BigInteger.valueOf(-2);
            byte[] actuals = ByteUtil.bigIntegerToBytesSigned(b, 8);
            assertArrayEquals(Hex.decode("fffffffffffffffe"), actuals);
        }
        {
            BigInteger b = BigInteger.valueOf(2);
            byte[] actuals = ByteUtil.bigIntegerToBytesSigned(b, 8);
            assertArrayEquals(Hex.decode("0000000000000002"), actuals);
        }
        {
            BigInteger b = BigInteger.valueOf(0);
            byte[] actuals = ByteUtil.bigIntegerToBytesSigned(b, 8);
            assertArrayEquals(Hex.decode("0000000000000000"), actuals);
        }
        {
            BigInteger b = new BigInteger("eeeeeeeeeeeeee", 16);
            byte[] actuals = ByteUtil.bigIntegerToBytesSigned(b, 8);
            assertArrayEquals(Hex.decode("00eeeeeeeeeeeeee"), actuals);
        }
        {
            BigInteger b = new BigInteger("eeeeeeeeeeeeeeee", 16);
            byte[] actuals = ByteUtil.bigIntegerToBytesSigned(b, 8);
            assertArrayEquals(Hex.decode("eeeeeeeeeeeeeeee"), actuals);
        }
    }

    @Test
    public void testBigIntegerToBytesNegative() {
        byte[] expecteds = new byte[] {(byte) 0xff, 0x0, 0x13, (byte) 0x88};
        BigInteger b = BigInteger.valueOf(-16772216);
        byte[] actuals = ByteUtil.bigIntegerToBytes(b);
        assertArrayEquals(expecteds, actuals);
    }

    @Test
    public void testBigIntegerToBytesZero() {
        byte[] expecteds = new byte[] {0x00};
        BigInteger b = BigInteger.ZERO;
        byte[] actuals = ByteUtil.bigIntegerToBytes(b);
        assertArrayEquals(expecteds, actuals);
    }

    @Test
    public void testCalcPacketLength() {
        byte[] test = new byte[] {0x0f, 0x10, 0x43};
        byte[] expected = new byte[] {0x00, 0x00, 0x00, 0x03};
        assertArrayEquals(expected, ByteUtil.calcPacketLength(test));
    }

    @Test
    public void testByteArrayToInt() {
        assertEquals(0, ByteUtil.byteArrayToInt(null));
        assertEquals(0, ByteUtil.byteArrayToInt(new byte[0]));
    }

    @Test
    public void testNumBytes() {
        String test1 = "0";
        String test2 = "1";
        String test3 = "1000000000"; //3B9ACA00
        int expected1 = 1;
        int expected2 = 1;
        int expected3 = 4;
        assertEquals(expected1, ByteUtil.numBytes(test1));
        assertEquals(expected2, ByteUtil.numBytes(test2));
        assertEquals(expected3, ByteUtil.numBytes(test3));
    }

    @Test
    public void testStripLeadingZeroes() {
        byte[] test1 = null;
        byte[] expected1 = null;
        assertArrayEquals(expected1, ByteUtil.stripLeadingZeroes(test1));
        byte[] test2 = new byte[] {};
        byte[] expected2 = new byte[] {0};
        assertArrayEquals(expected2, ByteUtil.stripLeadingZeroes(test2));
        byte[] test3 = new byte[] {0x00};
        byte[] expected3 = new byte[] {0};
        assertArrayEquals(expected3, ByteUtil.stripLeadingZeroes(test3));
        byte[] test4 = new byte[] {0x00, 0x01};
        byte[] expected4 = new byte[] {0x01};
        assertArrayEquals(expected4, ByteUtil.stripLeadingZeroes(test4));
        byte[] test5 = new byte[] {0x00, 0x00, 0x01};
        byte[] expected5 = new byte[] {0x01};
        assertArrayEquals(expected5, ByteUtil.stripLeadingZeroes(test5));
    }

    @Test
    public void testMatchingNibbleLength1() {
        // a larger than b
        byte[] a = new byte[] {0x00, 0x01};
        byte[] b = new byte[] {0x00};
        int result = ByteUtil.matchingNibbleLength(a, b);
        assertEquals(1, result);
    }

    @Test
    public void testMatchingNibbleLength2() {
        // b larger than a
        byte[] a = new byte[] {0x00};
        byte[] b = new byte[] {0x00, 0x01};
        int result = ByteUtil.matchingNibbleLength(a, b);
        assertEquals(1, result);
    }

    @Test
    public void testMatchingNibbleLength3() {
        // a and b the same length equal
        byte[] a = new byte[] {0x00};
        byte[] b = new byte[] {0x00};
        int result = ByteUtil.matchingNibbleLength(a, b);
        assertEquals(1, result);
    }

    @Test
    public void testMatchingNibbleLength4() {
        // a and b the same length not equal
        byte[] a = new byte[] {0x01};
        byte[] b = new byte[] {0x00};
        int result = ByteUtil.matchingNibbleLength(a, b);
        assertEquals(0, result);
    }

    @Test
    public void testNiceNiblesOutput_1() {
        byte[] test = {7, 0, 7, 5, 7, 0, 7, 0, 7, 9};
        String result = "\\x07\\x00\\x07\\x05\\x07\\x00\\x07\\x00\\x07\\x09";
        assertEquals(result, ByteUtil.nibblesToPrettyString(test));
    }

    @Test
    public void testNiceNiblesOutput_2() {
        byte[] test = {7, 0, 7, 0xf, 7, 0, 0xa, 0, 7, 9};
        String result = "\\x07\\x00\\x07\\x0f\\x07\\x00\\x0a\\x00\\x07\\x09";
        assertEquals(result, ByteUtil.nibblesToPrettyString(test));
    }

    @Test(expected = NullPointerException.class)
    public void testMatchingNibbleLength5() {
        // a == null
        byte[] a = null;
        byte[] b = new byte[] {0x00};
        ByteUtil.matchingNibbleLength(a, b);
    }

    @Test(expected = NullPointerException.class)
    public void testMatchingNibbleLength6() {
        // b == null
        byte[] a = new byte[] {0x00};
        byte[] b = null;
        ByteUtil.matchingNibbleLength(a, b);
    }

    @Test
    public void testMatchingNibbleLength7() {
        // a or b is empty
        byte[] a = new byte[0];
        byte[] b = new byte[] {0x00};
        int result = ByteUtil.matchingNibbleLength(a, b);
        assertEquals(0, result);
    }

    @Test
    public void firstNonZeroByte_1() {

        byte[] data = Hex.decode(
                "0000000000000000000000000000000000000000000000000000000000000000");
        int result = ByteUtil.firstNonZeroByte(data);

        assertEquals(-1, result);
    }

    @Test
    public void firstNonZeroByte_2() {

        byte[] data = Hex.decode(
                "0000000000000000000000000000000000000000000000000000000000332211");
        int result = ByteUtil.firstNonZeroByte(data);

        assertEquals(29, result);
    }

    @Test
    public void firstNonZeroByte_3() {

        byte[] data = Hex.decode(
                "2211009988776655443322110099887766554433221100998877665544332211");
        int result = ByteUtil.firstNonZeroByte(data);

        assertEquals(0, result);
    }

    @Test
    public void setBitTest() {
        /*
            Set on
         */
        byte[] data = ByteBuffer.allocate(4).putInt(0).array();
        int posBit = 24;
        int expected = 16777216;
        int result;
        byte[] ret = ByteUtil.setBit(data, posBit, 1);
        result = ByteUtil.byteArrayToInt(ret);
        assertEquals(expected, result);

        posBit = 25;
        expected = 50331648;
        ret = ByteUtil.setBit(data, posBit, 1);
        result = ByteUtil.byteArrayToInt(ret);
        assertEquals(expected, result);

        posBit = 2;
        expected = 50331652;
        ret = ByteUtil.setBit(data, posBit, 1);
        result = ByteUtil.byteArrayToInt(ret);
        assertEquals(expected, result);

        /*
            Set off
         */
        posBit = 24;
        expected = 33554436;
        ret = ByteUtil.setBit(data, posBit, 0);
        result = ByteUtil.byteArrayToInt(ret);
        assertEquals(expected, result);

        posBit = 25;
        expected = 4;
        ret = ByteUtil.setBit(data, posBit, 0);
        result = ByteUtil.byteArrayToInt(ret);
        assertEquals(expected, result);

        posBit = 2;
        expected = 0;
        ret = ByteUtil.setBit(data, posBit, 0);
        result = ByteUtil.byteArrayToInt(ret);
        assertEquals(expected, result);
    }

    @Test
    public void getBitTest() {
        byte[] data = ByteBuffer.allocate(4).putInt(0).array();
        ByteUtil.setBit(data, 24, 1);
        ByteUtil.setBit(data, 25, 1);
        ByteUtil.setBit(data, 2, 1);

        List<Integer> found = new ArrayList<>();
        for (int i = 0; i < (data.length * 8); i++) {
            int res = ByteUtil.getBit(data, i);
            if (res == 1) {
                if (i != 24 && i != 25 && i != 2) {
                    fail();
                } else {
                    found.add(i);
                }
            } else {
                if (i == 24 || i == 25 || i == 2) {
                    fail();
                }
            }
        }

        if (found.size() != 3) {
            fail();
        }
        assertEquals(2, (int) found.get(0));
        assertEquals(24, (int) found.get(1));
        assertEquals(25, (int) found.get(2));
    }

    @Test
    public void numToBytesTest() {
        byte[] bytes = ByteUtil.intToBytesNoLeadZeroes(-1);
        assertArrayEquals(bytes, Hex.decode("ffffffff"));
        bytes = ByteUtil.intToBytesNoLeadZeroes(1);
        assertArrayEquals(bytes, Hex.decode("01"));
        bytes = ByteUtil.intToBytesNoLeadZeroes(255);
        assertArrayEquals(bytes, Hex.decode("ff"));
        bytes = ByteUtil.intToBytesNoLeadZeroes(256);
        assertArrayEquals(bytes, Hex.decode("0100"));
        bytes = ByteUtil.intToBytesNoLeadZeroes(0);
        assertArrayEquals(EMPTY_BYTE_ARRAY, bytes);

        bytes = ByteUtil.intToBytes(-1);
        assertArrayEquals(bytes, Hex.decode("ffffffff"));
        bytes = ByteUtil.intToBytes(1);
        assertArrayEquals(bytes, Hex.decode("00000001"));
        bytes = ByteUtil.intToBytes(255);
        assertArrayEquals(bytes, Hex.decode("000000ff"));
        bytes = ByteUtil.intToBytes(256);
        assertArrayEquals(bytes, Hex.decode("00000100"));
        bytes = ByteUtil.intToBytes(0);
        assertArrayEquals(bytes, Hex.decode("00000000"));

        bytes = ByteUtil.longToBytesNoLeadZeroes(-1);
        assertArrayEquals(bytes, Hex.decode("ffffffffffffffff"));
        bytes = ByteUtil.longToBytesNoLeadZeroes(1);
        assertArrayEquals(bytes, Hex.decode("01"));
        bytes = ByteUtil.longToBytesNoLeadZeroes(255);
        assertArrayEquals(bytes, Hex.decode("ff"));
        bytes = ByteUtil.longToBytesNoLeadZeroes(1L << 32);
        assertArrayEquals(bytes, Hex.decode("0100000000"));
        bytes = ByteUtil.longToBytesNoLeadZeroes(0);
        assertArrayEquals(EMPTY_BYTE_ARRAY, bytes);

        bytes = ByteUtil.longToBytes(-1);
        assertArrayEquals(bytes, Hex.decode("ffffffffffffffff"));
        bytes = ByteUtil.longToBytes(1);
        assertArrayEquals(bytes, Hex.decode("0000000000000001"));
        bytes = ByteUtil.longToBytes(255);
        assertArrayEquals(bytes, Hex.decode("00000000000000ff"));
        bytes = ByteUtil.longToBytes(256);
        assertArrayEquals(bytes, Hex.decode("0000000000000100"));
        bytes = ByteUtil.longToBytes(0);
        assertArrayEquals(bytes, Hex.decode("0000000000000000"));
    }

    @Test
    public void testIpConversion() {
        String ip1 = "0.0.0.0";
        byte[] ip1Bytes = ByteUtil.hostToBytes(ip1);
        assertEquals(ip1, ByteUtil.bytesToIp(ip1Bytes));

        String ip2 = "35.36.37.138";
        byte[] ip2Bytes = ByteUtil.hostToBytes(ip2);
        assertEquals(ip2, ByteUtil.bytesToIp(ip2Bytes));

        String ip3 = "255.255.255.255";
        byte[] ip3Bytes = ByteUtil.hostToBytes(ip3);
        assertEquals(ip3, ByteUtil.bytesToIp(ip3Bytes));

        // Fallback case
        String ip4 = "255.255.255.256";
        byte[] ip4Bytes = ByteUtil.hostToBytes(ip4);
        assertEquals("0.0.0.0", ByteUtil.bytesToIp(ip4Bytes));
    }

    @Test
    public void testNumberOfLeadingZeros() {

        int n0 = ByteUtil.numberOfLeadingZeros(new byte[0]);
        assertEquals(0, n0);

        int n1 = ByteUtil.numberOfLeadingZeros(Hex.decode("05"));
        assertEquals(5, n1);

        int n2 = ByteUtil.numberOfLeadingZeros(Hex.decode("01"));
        assertEquals(7, n2);

        int n3 = ByteUtil.numberOfLeadingZeros(Hex.decode("00"));
        assertEquals(8, n3);

        int n4 = ByteUtil.numberOfLeadingZeros(Hex.decode("ff"));
        assertEquals(0, n4);


        byte[] v1 = Hex.decode("1040");

        int n5 = ByteUtil.numberOfLeadingZeros(v1);
        assertEquals(3, n5);

        // add leading zero bytes
        byte[] v2 = new byte[4];
        System.arraycopy(v1, 0, v2, 2, v1.length);

        int n6 = ByteUtil.numberOfLeadingZeros(v2);
        assertEquals(19, n6);

        byte[] v3 = new byte[8];

        int n7 = ByteUtil.numberOfLeadingZeros(v3);
        assertEquals(64, n7);

    }

    @Test
    public void nullArrayToNumber() {
        assertEquals(BigInteger.ZERO, ByteUtil.bytesToBigInteger(null));
        assertEquals(0L, ByteUtil.byteArrayToLong(null));
        assertEquals(0, ByteUtil.byteArrayToInt(null));
    }
}
