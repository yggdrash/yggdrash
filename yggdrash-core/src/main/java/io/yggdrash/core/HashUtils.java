package io.yggdrash.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtils {

    private static final Logger log = LoggerFactory.getLogger(HashUtils.class);

    public static byte[] sha256(byte[] input) {
        try {
            MessageDigest sha256digest = MessageDigest.getInstance("SHA-256");
            return sha256digest.digest(input);
        } catch (NoSuchAlgorithmException e) {
            log.error("No Such Algorithm", e);
            throw new RuntimeException(e);
        }
    }
}
