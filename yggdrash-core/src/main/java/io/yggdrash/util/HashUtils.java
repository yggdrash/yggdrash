package io.yggdrash.util;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.binary.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtils {
    // hash algorithm is fixed
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final Logger log = LoggerFactory.getLogger(HashUtils.class);

    public static byte[] sha256(byte[] input) {
        try {
            return MessageDigest.getInstance(HashUtils.HASH_ALGORITHM).digest(input);
        } catch (NoSuchAlgorithmException e) {
            log.error("No Such Algorithm", e);
            throw new RuntimeException(e);
        }
    }

    public static String hashString(String input) {
        return Hex.encodeHexString(sha256(StringUtils.getBytesUtf8(input)));
    }
}
