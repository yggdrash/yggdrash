package io.yggdrash.common.crypto;

import io.yggdrash.common.util.ByteUtil;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.util.encoders.Hex;

import java.security.SecureRandom;

import static org.junit.Assert.assertArrayEquals;

public class AESEncryptTest {

    private static final Logger log = LoggerFactory.getLogger(AESEncryptTest.class);

    private static final int WALLET_PBKDF2_ITERATION = 262144;
    private static final int WALLET_PBKDF2_DKLEN = 32;
    private static final String WALLET_PBKDF2_ALGORITHM = "SHA-256";

    @Test
    public void testEncryptDecrypt1() throws InvalidCipherTextException {
        byte[] plainBytes = "01234567890123450123456789012345345".getBytes();
        log.info("plain: {}", Hex.toHexString(plainBytes));

        String password = "Aa1234567890#";
        log.info("password: {}", password);

        byte[] iv = new byte[16];
        byte[] salt = new byte[32];
        SecureRandom prng = new SecureRandom();
        prng.nextBytes(iv);
        prng.nextBytes(salt);

        byte[] kdfPass = HashUtil.pbkdf2(
                password.getBytes(),
                salt,
                WALLET_PBKDF2_ITERATION,
                WALLET_PBKDF2_DKLEN,
                WALLET_PBKDF2_ALGORITHM);
        byte[] encData = AESEncrypt.encrypt(
                plainBytes,
                ByteUtil.parseBytes(kdfPass, 0, 16),
                iv);
        log.info("encrypt: {}", Hex.toHexString(encData));

        byte[] plainData = AESEncrypt.decrypt(
                encData, ByteUtil.parseBytes(kdfPass, 0, 16), iv);
        log.info("decrypt: {}", Hex.toHexString(plainData));

        assertArrayEquals(plainBytes, plainData);
    }

    /**
     * test encryption/decryption with large data 100 kByte.
     *
     * throws InvalidCipherTextException
     */
    @Test
    public void testEncryptDecrypt2() throws InvalidCipherTextException {
        byte[] plain = "0123456789".getBytes();
        byte[] plainBytes = new byte[100000];

        for (int i = 0; i < plainBytes.length / plain.length; i++) {
            System.arraycopy(plain, 0, plainBytes, i * plain.length, plain.length);
        }
        log.info("plain: {}", Hex.toHexString(plainBytes));

        String password = "Aa1234567890#";
        log.info("password: {}", password);

        byte[] iv = new byte[16];
        byte[] salt = new byte[32];
        SecureRandom prng = new SecureRandom();
        prng.nextBytes(iv);
        prng.nextBytes(salt);

        byte[] kdfPass = HashUtil.pbkdf2(
                password.getBytes(),
                salt,
                WALLET_PBKDF2_ITERATION,
                WALLET_PBKDF2_DKLEN,
                WALLET_PBKDF2_ALGORITHM);
        byte[] encData = AESEncrypt.encrypt(
                plainBytes,
                ByteUtil.parseBytes(kdfPass, 0, 16),
                iv);
        log.info("encrypt: {}", Hex.toHexString(encData));

        byte[] plainData = AESEncrypt.decrypt(
                encData, ByteUtil.parseBytes(kdfPass, 0, 16), iv);
        log.info("decrypt: {}", Hex.toHexString(plainData));

        assertArrayEquals(plainBytes, plainData);
    }
}
