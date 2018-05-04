package io.yggdrash.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/*
    https://github.com/eugenp/tutorials/blob/master/core-java/src/main/java/com/baeldung/hashing/SHA256Hashing.java
    https://stackoverflow.com/questions/5531455/how-to-hash-some-string-with-sha256-in-java
 */
public class HashUtils {

    private static final Logger log = LoggerFactory.getLogger(HashUtils.class);

    public static String sha256Hex(final String originalString) {
        final MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
        final byte[] encodedHash = digest.digest(originalString.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(encodedHash);
    }

    public static String sha256Base64(final String originalString) throws
            NoSuchAlgorithmException {
        final MessageDigest digest = MessageDigest.getInstance("SHA-256");
        final byte[] encodedHash = digest.digest(originalString.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encodedHash);
    }

//    public static String HashWithGuava(final String originalString) {
//        final String sha256hex = Hashing.sha256().hashString(originalString, StandardCharsets.UTF_8).toString();
//        return sha256hex;
//    }
//
//    public static String HashWithApacheCommons(final String originalString) {
//        final String sha256hex = DigestUtils.sha256Hex(originalString);
//        return sha256hex;
//    }
//
//    public static String HashWithBouncyCastle(final String originalString) throws NoSuchAlgorithmException {
//        final MessageDigest digest = MessageDigest.getInstance("SHA-256");
//        final byte[] hash = digest.digest(originalString.getBytes(StandardCharsets.UTF_8));
//        final String sha256hex = new String(Hex.encode(hash));
//        return sha256hex;
//    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder();
        for (byte aHash : hash) {
            String hex = Integer.toHexString(0xff & aHash);
            if (hex.length() == 1)
                hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
