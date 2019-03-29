/* Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */

package io.yggdrash.common.utils;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class ByteUtil {

    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    private static final byte[] ZERO_BYTE_ARRAY = new byte[] {0};

    private ByteUtil() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Creates a copy of bytes and appends b to the end of it
     */
    public static byte[] appendByte(byte[] bytes, byte b) {
        byte[] result = Arrays.copyOf(bytes, bytes.length + 1);
        result[result.length - 1] = b;
        return result;
    }

    /**
     * The regular {@link BigInteger#toByteArray()} method isn't quite what we often need:
     * it appends a leading zero to indicate that the number is positive and may need padding.
     *
     * @param b        the integer to format into a byte array
     * @param numBytes the desired size of the resulting byte array
     * @return numBytes byte long array.
     */
    public static byte[] bigIntegerToBytes(BigInteger b, int numBytes) {
        if (b == null) {
            return null;
        }
        byte[] bytes = new byte[numBytes];
        byte[] biBytes = b.toByteArray();
        int start = (biBytes.length == numBytes + 1) ? 1 : 0;
        int length = Math.min(biBytes.length, numBytes);
        System.arraycopy(biBytes, start, bytes, numBytes - length, length);
        return bytes;
    }

    /**
     * Omitting sign indication byte.
     * <br><br>
     * Instead of {@link org.spongycastle.util.BigIntegers#asUnsignedByteArray(BigInteger)}
     * <br>we use this custom method to avoid an empty array in case of BigInteger.ZERO
     *
     * @param value - any big integer number. A <code>null</code>-value will return <code>null</code>
     * @return A byte array without a leading zero byte if present in the signed encoding.
     * BigInteger.ZERO will return an array with length 1 and byte-value 0.
     */
    public static byte[] bigIntegerToBytes(BigInteger value) {
        if (value == null) {
            return null;
        }

        byte[] data = value.toByteArray();

        if (data.length != 1 && data[0] == 0) {
            byte[] tmp = new byte[data.length - 1];
            System.arraycopy(data, 1, tmp, 0, tmp.length);
            data = tmp;
        }
        return data;
    }

    public static byte[] bigIntegerToBytesSigned(BigInteger b, int numBytes) {
        if (b == null) {
            return null;
        }
        byte[] bytes = new byte[numBytes];
        Arrays.fill(bytes, b.signum() < 0 ? (byte) 0xFF : 0x00);
        byte[] biBytes = b.toByteArray();
        int start = (biBytes.length == numBytes + 1) ? 1 : 0;
        int length = Math.min(biBytes.length, numBytes);
        System.arraycopy(biBytes, start, bytes, numBytes - length, length);
        return bytes;
    }

    /**
     * Cast hex encoded value from byte[] to BigInteger
     * null is parsed like byte[0]
     *
     * @param bb byte array contains the values
     * @return unsigned positive BigInteger value.
     */
    public static BigInteger bytesToBigInteger(byte[] bb) {
        return (bb == null || bb.length == 0) ? BigInteger.ZERO : new BigInteger(1, bb);
    }

    /**
     * Returns the amount of nibbles that match each other from 0 ...
     * amount will never be larger than smallest input
     *
     * @param a - first input
     * @param b - second input
     * @return Number of bytes that match
     */
    public static int matchingNibbleLength(byte[] a, byte[] b) {
        int i = 0;
        int length = a.length < b.length ? a.length : b.length;
        while (i < length) {
            if (a[i] != b[i]) {
                return i;
            }
            i++;
        }
        return i;
    }

    /**
     * Converts a long value into a byte array.
     *
     * @param val - long value to convert
     * @return <code>byte[]</code> of length 8, representing the long value
     */
    public static byte[] longToBytes(long val) {
        return ByteBuffer.allocate(Long.BYTES).putLong(val).array();
    }

    /**
     * Converts a long value into a byte array.
     *
     * @param val - long value to convert
     * @return decimal value with leading byte that are zeroes striped
     */
    public static byte[] longToBytesNoLeadZeroes(long val) {

        // todo: improve performance by while strip numbers until (long >> 8 == 0)
        if (val == 0) {
            return EMPTY_BYTE_ARRAY;
        }

        byte[] data = ByteBuffer.allocate(Long.BYTES).putLong(val).array();

        return stripLeadingZeroes(data);
    }

    /**
     * Converts int value into a byte array.
     *
     * @param val - int value to convert
     * @return <code>byte[]</code> of length 4, representing the int value
     */
    public static byte[] intToBytes(int val) {
        return ByteBuffer.allocate(Integer.BYTES).putInt(val).array();
    }

    /**
     * Converts a int value into a byte array.
     *
     * @param val - int value to convert
     * @return value with leading byte that are zeroes striped
     */
    public static byte[] intToBytesNoLeadZeroes(int val) {

        if (val == 0) {
            return EMPTY_BYTE_ARRAY;
        }

        int length = 0;

        int tmpVal = val;
        while (tmpVal != 0) {
            tmpVal = tmpVal >>> 8;
            ++length;
        }

        byte[] result = new byte[length];

        int index = result.length - 1;
        while (val != 0) {

            result[index] = (byte) (val & 0xFF);
            val = val >>> 8;
            index -= 1;
        }

        return result;
    }

    /**
     * Calculate packet length
     *
     * @param msg byte[]
     * @return byte-array with 4 elements
     */
    public static byte[] calcPacketLength(byte[] msg) {
        int msgLen = msg.length;
        return new byte[] {
                (byte) ((msgLen >> 24) & 0xFF),
                (byte) ((msgLen >> 16) & 0xFF),
                (byte) ((msgLen >> 8) & 0xFF),
                (byte) ((msgLen) & 0xFF)};
    }

    /**
     * Cast hex encoded value from byte[] to int
     * null is parsed like byte[0]
     * <p>
     * Limited to Integer.MAX_VALUE: 2^32-1 (4 bytes)
     *
     * @param b array contains the values
     * @return unsigned positive int value.
     */
    public static int byteArrayToInt(byte[] b) {
        if (b == null || b.length == 0) {
            return 0;
        }
        return new BigInteger(1, b).intValue();
    }

    /**
     * Cast hex encoded value from byte[] to long
     * null is parsed like byte[0]
     * <p>
     * Limited to Long.MAX_VALUE: 2<sup>63</sup>-1 (8 bytes)
     *
     * @param b array contains the values
     * @return unsigned positive long value.
     */
    public static long byteArrayToLong(byte[] b) {
        if (b == null || b.length == 0) {
            return 0;
        }
        return new BigInteger(1, b).longValue();
    }

    /**
     * Turn nibbles to a pretty looking output string
     * <p>
     * Example. [ 1, 2, 3, 4, 5 ] becomes '\x11\x23\x45'
     *
     * @param nibbles - getting byte of data [ 04 ] and turning
     *                it to a '\x04' representation
     * @return pretty string of nibbles
     */
    public static String nibblesToPrettyString(byte[] nibbles) {
        StringBuilder builder = new StringBuilder();
        for (byte nibble : nibbles) {
            final String nibbleString = oneByteToHexString(nibble);
            builder.append("\\x").append(nibbleString);
        }
        return builder.toString();
    }

    private static String oneByteToHexString(byte value) {
        String retVal = Integer.toString(value & 0xFF, 16);
        if (retVal.length() == 1) {
            retVal = "0" + retVal;
        }
        return retVal;
    }

    /**
     * Calculate the number of bytes need
     * to encode the number
     *
     * @param val - number
     * @return number of min bytes used to encode the number
     */
    public static int numBytes(String val) {
        if (!val.matches("^[0-9]+$")) {
            throw new Error("value is not number");
        }

        BigInteger bigInt = new BigInteger(val);
        int bytes = 0;

        while (!bigInt.equals(BigInteger.ZERO)) {
            bigInt = bigInt.shiftRight(8);
            ++bytes;
        }
        if (bytes == 0) {
            ++bytes;
        }
        return bytes;
    }

    public static int firstNonZeroByte(byte[] data) {
        for (int i = 0; i < data.length; ++i) {
            if (data[i] != 0) {
                return i;
            }
        }
        return -1;
    }

    public static byte[] stripLeadingZeroes(byte[] data) {

        if (data == null) {
            return null;
        }

        final int firstNonZero = firstNonZeroByte(data);
        switch (firstNonZero) {
            case -1:
                return ZERO_BYTE_ARRAY;

            case 0:
                return data;

            default:
                byte[] result = new byte[data.length - firstNonZero];
                System.arraycopy(data, firstNonZero, result, 0, data.length - firstNonZero);

                return result;
        }
    }

    public static byte[] setBit(byte[] data, int pos, int val) {

        if ((data.length * 8) - 1 < pos) {
            throw new Error("outside byte array limit, pos: " + pos);
        }

        int posByte = data.length - 1 - (pos) / 8;
        int posBit = (pos) % 8;
        byte setter = (byte) (1 << (posBit));
        byte toBeSet = data[posByte];
        byte result;
        if (val == 1) {
            result = (byte) (toBeSet | setter);
        } else {
            result = (byte) (toBeSet & ~setter);
        }

        data[posByte] = result;
        return data;
    }

    public static int getBit(byte[] data, int pos) {

        if ((data.length * 8) - 1 < pos) {
            throw new Error("outside byte array limit, pos: " + pos);
        }

        int posByte = data.length - 1 - pos / 8;
        int posBit = pos % 8;
        byte dataByte = data[posByte];
        return Math.min(1, ((dataByte & 0xff) & (1 << (posBit))));
    }

    public static byte[] and(byte[] b1, byte[] b2) {
        if (b1.length != b2.length) {
            throw new RuntimeException("Array sizes differ");
        }
        byte[] ret = new byte[b1.length];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = (byte) (b1[i] & b2[i]);
        }
        return ret;
    }

    public static byte[] or(byte[] b1, byte[] b2) {
        if (b1.length != b2.length) {
            throw new RuntimeException("Array sizes differ");
        }
        byte[] ret = new byte[b1.length];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = (byte) (b1[i] | b2[i]);
        }
        return ret;
    }

    /**
     * @param arrays - arrays to merge
     * @return - merged array
     */
    public static byte[] merge(byte[]... arrays) {
        int count = 0;
        for (byte[] array : arrays) {
            count += array.length;
        }

        // Create new array and copy all array contents
        byte[] mergedArray = new byte[count];
        int start = 0;
        for (byte[] array : arrays) {
            System.arraycopy(array, 0, mergedArray, start, array.length);
            start += array.length;
        }
        return mergedArray;
    }

    public static int length(byte[]... bytes) {
        int result = 0;
        for (byte[] array : bytes) {
            result += (array == null) ? 0 : array.length;
        }
        return result;
    }

    /**
     * Converts string representation of host/ip to 4-bytes byte[] IPv4
     */
    public static byte[] hostToBytes(String ip) {
        byte[] bytesIp;
        try {
            bytesIp = InetAddress.getByName(ip).getAddress();
        } catch (UnknownHostException e) {
            bytesIp = new byte[4];  // fall back to invalid 0.0.0.0 address
        }

        return bytesIp;
    }

    /**
     * Converts 4 bytes IPv4 IP to String representation
     */
    public static String bytesToIp(byte[] bytesIp) {

        StringBuilder sb = new StringBuilder();
        sb.append(bytesIp[0] & 0xFF);
        sb.append(".");
        sb.append(bytesIp[1] & 0xFF);
        sb.append(".");
        sb.append(bytesIp[2] & 0xFF);
        sb.append(".");
        sb.append(bytesIp[3] & 0xFF);

        return sb.toString();
    }

    /**
     * Returns a number of zero bits preceding the highest-order ("leftmost") one-bit
     * interpreting input array as a big-endian integer value
     */
    public static int numberOfLeadingZeros(byte[] bytes) {

        int i = firstNonZeroByte(bytes);

        if (i == -1) {
            return bytes.length * 8;
        } else {
            int byteLeadingZeros = Integer.numberOfLeadingZeros((int) bytes[i] & 0xff) - 24;
            return i * 8 + byteLeadingZeros;
        }
    }

    /**
     * Parses fixed number of bytes starting from {@code offset} in {@code input} array.
     * If {@code input} has not enough bytes return array will be right padded with zero bytes.
     * I.e. if {@code offset} is higher than {@code input.length} then zero byte array of length
     * {@code len} will be returned
     */
    public static byte[] parseBytes(byte[] input, int offset, int len) {

        if (offset >= input.length || len == 0) {
            return EMPTY_BYTE_ARRAY;
        }

        byte[] bytes = new byte[len];
        System.arraycopy(input, offset, bytes, 0, Math.min(input.length - offset, len));
        return bytes;
    }
}