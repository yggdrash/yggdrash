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

package io.yggdrash.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.DecoderException;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

public class Utils {
    private static final Logger log = LoggerFactory.getLogger(Utils.class);
    private static final BigInteger _1000_ = new BigInteger("1000");
    private static final SecureRandom random = new SecureRandom();
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final JsonParser jsonParser = new JsonParser();

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

    public static String longToTimePeriod(long msec) {
        if (msec < 1000) {
            return msec + "ms";
        }
        if (msec < 3000) {
            return String.format("%.2fs", msec / 1000d);
        }
        if (msec < 60 * 1000) {
            return (msec / 1000) + "s";
        }
        long sec = msec / 1000;
        if (sec < 5 * 60) {
            return (sec / 60) + "m" + (sec % 60) + "s";
        }
        long min = sec / 60;
        if (min < 60) {
            return min + "m";
        }
        long hour = min / 60;
        if (min < 24 * 60) {
            return hour + "h" + (min % 60) + "m";
        }
        long day = hour / 24;
        return day + "d" + (day % 24) + "h";
    }

    public static String getValueShortString(BigInteger number) {
        BigInteger result = number;
        int pow = 0;
        while (result.compareTo(_1000_) == 1 || result.compareTo(_1000_) == 0) {
            result = result.divide(_1000_);
            pow += 3;
        }
        return result.toString() + "\u00b7(" + "10^" + pow + ")";
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

    public static boolean isValidAddress(byte[] addr) {
        return addr != null && addr.length == 20;
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

    public static SecureRandom getRandom() {
        return random;
    }

    static double getJavaVersion() {
        String version = System.getProperty("java.version");

        // on android this property equals to 0
        if (version.equals("0")) {
            return 0;
        }
        // 12-ea
        if (version.contains("-")) {
            int indexOfDash = version.indexOf("-");
            return Double.parseDouble(version.substring(0, indexOfDash));
        }
        if (!version.contains(".")) {
            return Double.parseDouble(version);
        }
        int pos = 0, count = 0;
        for (; pos < version.length() && count < 2; pos++) {
            if (version.charAt(pos) == '.') {
                count++;
            }
        }
        return Double.parseDouble(version.substring(0, pos - 1));
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

    public static String getNodeIdShort(String nodeId) {
        return nodeId == null ? "<null>" : nodeId.substring(0, 8);
    }

    public static long toUnixTime(long javaTime) {
        return javaTime / 1000;
    }

    public static long fromUnixTime(long unixTime) {
        return unixTime * 1000;
    }

    @SafeVarargs
    public static <T> T[] mergeArrays(T[]... arr) {
        int size = 0;
        for (T[] ts : arr) {
            size += ts.length;
        }
        @SuppressWarnings("unchecked")
        T[] ret = (T[]) Array.newInstance(arr[0].getClass().getComponentType(), size);
        int off = 0;
        for (T[] ts : arr) {
            System.arraycopy(ts, 0, ret, off, ts.length);
            off += ts.length;
        }
        return ret;
    }

    public static String align(String s, char fillChar, int targetLen, boolean alignRight) {
        if (targetLen <= s.length()) {
            return s;
        }
        String alignString = repeat("" + fillChar, targetLen - s.length());
        return alignRight ? alignString + s : s + alignString;

    }

    public static String repeat(String s, int n) {
        if (s.length() == 1) {
            byte[] bb = new byte[n];
            Arrays.fill(bb, s.getBytes()[0]);
            return new String(bb);
        } else {
            StringBuilder ret = new StringBuilder();
            for (int i = 0; i < n; i++) {
                ret.append(s);
            }
            return ret.toString();
        }
    }

    /**
     * Show std err messages in red and throw RuntimeException to stop execution.
     */
    public static void showErrorAndExit(String message, String... messages) {
        LoggerFactory.getLogger("general").error(message);
        final String ANSI_RED = "\u001B[31m";
        final String ANSI_RESET = "\u001B[0m";

        System.err.println(ANSI_RED);
        System.err.println();
        System.err.println("        " + message);
        for (String msg : messages) {
            System.err.println("        " + msg);
        }
        System.err.println();
        System.err.println(ANSI_RESET);

        throw new RuntimeException(message);
    }

    /**
     * Show std warning messages in red.
     */
    public static void showWarn(String message, String... messages) {
        LoggerFactory.getLogger("general").warn(message);
        final String ANSI_RED = "\u001B[31m";
        final String ANSI_RESET = "\u001B[0m";

        System.err.println(ANSI_RED);
        System.err.println();
        System.err.println("        " + message);
        for (String msg : messages) {
            System.err.println("        " + msg);
        }
        System.err.println();
        System.err.println(ANSI_RESET);
    }

    public static String sizeToStr(long size) {
        if (size < 2 * (1L << 10)) {
            return size + "b";
        }
        if (size < 2 * (1L << 20)) {
            return String.format("%dKb", size / (1L << 10));
        }
        if (size < 2 * (1L << 30)) {
            return String.format("%dMb", size / (1L << 20));
        }
        return String.format("%dGb", size / (1L << 30));
    }

    public static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static HashMap convertJsonToMap(JsonObject json) {
        try {
            return mapper.readValue(json.toString(), HashMap.class);
        } catch (IOException e) {
            log.warn("convert fail json to map err={}", e);
            return null;
        }
    }

    public static String convertObjToString(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("convert fail obj to string err={}", e);
            return null;
        }
    }

    public static JsonObject convertObjToJsonObject(Object obj) {
        String string = convertObjToString(obj);
        return parseJsonObject(string);
    }

    public static JsonArray parseJsonArray(String data) {
        return (JsonArray) jsonParser.parse(data);
    }

    public static JsonObject parseJsonObject(String json) {
        return (JsonObject) jsonParser.parse(json);
    }

    public static JsonObject parseJsonObject(Reader json) {
        return (JsonObject) jsonParser.parse(json);
    }

}