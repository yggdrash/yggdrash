package io.yggdrash.core.wallet;

import io.yggdrash.TestConstants.SlowTest;
import io.yggdrash.common.crypto.HashUtil;
import io.yggdrash.common.utils.ByteUtil;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.crypto.engines.AESEngine;
import org.spongycastle.crypto.modes.SICBlockCipher;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.crypto.params.ParametersWithIV;
import org.spongycastle.util.encoders.Hex;

import java.security.SecureRandom;

import static org.junit.Assert.assertArrayEquals;

public class AesEncryptTest extends SlowTest {

    private static final Logger log = LoggerFactory.getLogger(AesEncryptTest.class);

    private static final int WALLET_PBKDF2_ITERATION = 262144;
    private static final int WALLET_PBKDF2_DKLEN = 32;
    private static final String WALLET_PBKDF2_ALGORITHM = "SHA-256";

    @Test
    public void testEncryptDecrypt_WithIv() throws InvalidCipherTextException {
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
        byte[] encData = AesEncrypt.encrypt(
                plainBytes,
                ByteUtil.parseBytes(kdfPass, 0, 16),
                iv);
        log.info("encrypt: {}", Hex.toHexString(encData));

        byte[] plainData = AesEncrypt.decrypt(
                encData, ByteUtil.parseBytes(kdfPass, 0, 16), iv);
        log.info("decrypt: {}", Hex.toHexString(plainData));

        assertArrayEquals(plainBytes, plainData);
    }

    /**
     * Test encryption/decryption with large data 100 kByte.
     *
     * throws InvalidCipherTextException
     */
    @Test
    public void testEncryptDecrypt_WithIv_100k() throws InvalidCipherTextException {
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
        byte[] encData = AesEncrypt.encrypt(
                plainBytes,
                ByteUtil.parseBytes(kdfPass, 0, 16),
                iv);
        log.info("encrypt: {}", Hex.toHexString(encData));

        byte[] plainData = AesEncrypt.decrypt(
                encData, ByteUtil.parseBytes(kdfPass, 0, 16), iv);
        log.info("decrypt: {}", Hex.toHexString(plainData));

        assertArrayEquals(plainBytes, plainData);
    }

    @Test
    public void testEncryptDecrypt_WithOutIv() throws InvalidCipherTextException {
        byte[] kdf = new byte[32];
        SecureRandom prng = new SecureRandom();
        prng.nextBytes(kdf);

        byte[] plainBytes = "01234567890123450123456789012345345".getBytes();
        log.info("plain: {}", Hex.toHexString(plainBytes));

        byte[] encData = AesEncrypt.encrypt(plainBytes, kdf);
        log.info("encrypt: {}", Hex.toHexString(encData));

        byte[] plainData = AesEncrypt.decrypt(encData, kdf);
        log.info("decrypt: {}", Hex.toHexString(plainData));

        assertArrayEquals(plainBytes, plainData);
    }

    @Test
    public void testKeyEncryption_AesCtr() {
        String password = "Aa1234567890#";
        log.debug("password: {}", password);

        byte[] plainBytes = "01234567890123456789".getBytes();
        log.debug("plain: {}", Hex.toHexString(plainBytes));

        byte[] iv = new byte[16];
        byte[] salt = new byte[32];
        SecureRandom prng = new SecureRandom();
        prng.nextBytes(iv);
        prng.nextBytes(salt);
        log.debug("iv: {}", Hex.toHexString(iv));
        log.debug("salt: {}", Hex.toHexString(salt));

        byte[] kdfPass = HashUtil.pbkdf2(
                password.getBytes(),
                salt,
                WALLET_PBKDF2_ITERATION,
                WALLET_PBKDF2_DKLEN,
                WALLET_PBKDF2_ALGORITHM);

        KeyParameter key = new KeyParameter(ByteUtil.parseBytes(kdfPass, 0, 16));
        ParametersWithIV params = new ParametersWithIV(key, iv);

        AESEngine engine = new AESEngine();
        SICBlockCipher ctrEngine = new SICBlockCipher(engine);

        ctrEngine.init(true, params);

        // encrypt
        byte[] cipher = new byte[plainBytes.length];
        ctrEngine.processBytes(plainBytes, 0, plainBytes.length, cipher, 0);
        log.debug("cipher: {}", Hex.toHexString(cipher));

        // decrypt
        byte[] output = new byte[cipher.length];
        ctrEngine.init(false, params);
        ctrEngine.processBytes(cipher, 0, cipher.length, output, 0);
        log.debug("plain: {}", Hex.toHexString(output));

        assertArrayEquals(plainBytes, output);
    }

    @Test
    public void testAesEncryption_AesCbc() throws InvalidCipherTextException {
        String password = "Aa1234567890#";
        log.debug("password: {}", password);

        byte[] plainBytes = "01234567890123456789".getBytes();
        log.debug("plain: {}", Hex.toHexString(plainBytes));

        byte[] iv = new byte[16];
        byte[] salt = new byte[32];
        SecureRandom prng = new SecureRandom();
        prng.nextBytes(iv);
        prng.nextBytes(salt);
        log.debug("iv: {}", Hex.toHexString(iv));
        log.debug("salt: {}", Hex.toHexString(salt));

        byte[] kdfPass = HashUtil.pbkdf2(
                password.getBytes(),
                salt,
                WALLET_PBKDF2_ITERATION,
                WALLET_PBKDF2_DKLEN,
                WALLET_PBKDF2_ALGORITHM);

        byte[] encData = AesEncrypt.encrypt(
                plainBytes, ByteUtil.parseBytes(kdfPass, 0, 16), iv);
        log.debug("encrypt: {}", Hex.toHexString(encData));

        byte[] plainData = AesEncrypt.decrypt(
                encData, ByteUtil.parseBytes(kdfPass, 0, 16), iv);
        log.debug("decrypt: {}", Hex.toHexString(plainData));
        assertArrayEquals(plainBytes, plainData);
    }
}
