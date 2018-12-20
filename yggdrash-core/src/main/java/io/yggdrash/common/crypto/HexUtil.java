/*
 * Copyright (c) [2016] [ <ether.camp> ]
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

package io.yggdrash.common.crypto;

import io.yggdrash.common.util.ByteUtil;
import org.spongycastle.util.encoders.DecoderException;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.List;
import java.util.regex.Pattern;

public class HexUtil {

    /**
     * Convert a byte-array into a hex String.<br>
     * Works similar to {@link Hex#toHexString}
     * but allows for <code>null</code>
     *
     * @param data - byte-array to convert to a hex-string
     * @return hex representation of the data.<br>
     * Returns an empty String if the input is <code>null</code>
     * @see Hex#toHexString
     */
    public static String toHexString(byte[] data) {
        return data == null ? "" : Hex.toHexString(data);
    }

    public static String toHexString(long number) {
        byte[] bytes = ByteUtil.longToBytes(number);
        return toHexString(bytes);
    }

    public static long hexStringToLong(String data) {
        byte[] bytes = hexStringToBytes(data);
        return ByteUtil.byteArrayToLong(bytes);
    }

    /**
     * Converts string hex representation to data bytes
     * Accepts following hex:
     * - with or without 0x prefix
     * - with no leading 0, like 0xabc -> 0x0abc
     *
     * @param data String like '0xa5e..' or just 'a5e..'
     * @return decoded bytes array
     */
    public static byte[] hexStringToBytes(String data) {
        if (data == null) {
            return new byte[0];
        }
        if (data.startsWith("0x")) {
            data = data.substring(2);
        }
        if (data.length() % 2 == 1) {
            data = "0" + data;
        }
        return Hex.decode(data);
    }

    /**
     * @param number should be in form '0x34fabd34....'
     * @return String
     */
    public static BigInteger unifiedNumericToBigInteger(String number) {

        boolean match = Pattern.matches("0[xX][0-9a-fA-F]+", number);
        if (!match) {
            return (new BigInteger(number));
        } else {
            number = number.substring(2);
            number = number.length() % 2 != 0 ? "0".concat(number) : number;
            byte[] numberBytes = Hex.decode(number);
            return (new BigInteger(1, numberBytes));
        }
    }

    public static String getHashListShort(List<byte[]> blockHashes) {
        if (blockHashes.isEmpty()) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder();
        String firstHash = Hex.toHexString(blockHashes.get(0));
        String lastHash = Hex.toHexString(blockHashes.get(blockHashes.size() - 1));
        return sb.append(" ").append(firstHash).append("...").append(lastHash).toString();
    }

    /**
     * Decodes a hex string to address bytes and checks validity
     *
     * @param hex - a hex string of the address, e.g., 6c386a4b26f73c802f34673f7248bb118f97424a
     * @return - decode and validated address byte[]
     */
    public static byte[] addressStringToBytes(String hex) {
        final byte[] addr;
        try {
            addr = Hex.decode(hex);
        } catch (DecoderException addressIsNotValid) {
            return null;
        }

        if (isValidAddress(addr)) {
            return addr;
        }
        return null;
    }

    /**
     * @param addr length should be 20
     * @return short string represent 1f21c...
     */
    public static String getAddressShortString(byte[] addr) {

        if (!isValidAddress(addr)) {
            throw new Error("not an address");
        }

        String addrShort = Hex.toHexString(addr, 0, 3);

        return addrShort + "...";
    }

    public static boolean isValidAddress(byte[] addr) {
        return addr != null && addr.length == 20;
    }
}