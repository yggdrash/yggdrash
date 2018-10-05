package io.yggdrash.crypto;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.util.encoders.Hex;

import static org.junit.Assert.assertArrayEquals;

public class AESEncryptTest {

    private static final Logger log = LoggerFactory.getLogger(AESEncryptTest.class);

    @Test
    public void testEncryptDecrypt1() throws InvalidCipherTextException {

        // password generation using KDF
        String password = "Aa1234567890#";
        byte[] kdf = Password.generateKeyDerivation(password.getBytes(), 32);

        byte[] plainBytes = "01234567890123450123456789012345345".getBytes();
        log.info("plain: {}", Hex.toHexString(plainBytes));

        byte[] encData = AESEncrypt.encrypt(plainBytes, kdf);
        log.info("encrypt: {}", Hex.toHexString(encData));

        byte[] plainData = AESEncrypt.decrypt(encData, kdf);
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

        // password generation using KDF
        String password = "Aa1234567890#";
        byte[] kdf = Password.generateKeyDerivation(password.getBytes(), 32);

        byte[] plain = "0123456789".getBytes();
        byte[] plainBytes = new byte[100000];
        for (int i = 0; i < plainBytes.length / plain.length; i++) {
            System.arraycopy(plain, 0, plainBytes, i * plain.length, plain.length);
        }
        log.info("plain: {}", Hex.toHexString(plainBytes));

        byte[] encData = AESEncrypt.encrypt(plainBytes, kdf);
        log.info("encrypt: {}", Hex.toHexString(encData));

        byte[] plainData = AESEncrypt.decrypt(encData, kdf);
        log.info("decrypt: {}", Hex.toHexString(plainData));

        assertArrayEquals(plainBytes, plainData);

    }
}
