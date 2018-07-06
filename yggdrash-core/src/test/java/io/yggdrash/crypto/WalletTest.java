package io.yggdrash.crypto;

import io.yggdrash.core.Wallet;
import io.yggdrash.util.ByteUtil;
import io.yggdrash.util.FileUtil;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.crypto.engines.AESEngine;
import org.spongycastle.crypto.modes.SICBlockCipher;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.crypto.params.ParametersWithIV;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.security.SecureRandom;

import static org.junit.Assert.assertArrayEquals;

/**
 * This is the test class for managing the node's wallet(key).
 */
public class WalletTest {
    private static final Logger log = LoggerFactory.getLogger(WalletTest.class);

    // key encryption with random iv
    @Test
    public void testKeyEncryption2() {

        final int AES_KEYLENGTH = 128;

        // key generation
        ECKey ecKey = new ECKey();
        byte[] priKey = ecKey.getPrivKeyBytes();

        // password generation using KDF
        String password = "Aa1234567890#";
        byte[] kdf = Password.generateKeyDerivation(password.getBytes(), 32);
        byte[] keyBytes = ByteUtil.parseBytes(kdf, 0, 32);

        // generate iv
        byte[] ivBytes = new byte[AES_KEYLENGTH / 8];
        SecureRandom prng = new SecureRandom();
        prng.nextBytes(ivBytes);

        byte[] plainBytes = "01234567890123456789".getBytes();
        log.info("plain: {}", Hex.toHexString(plainBytes));

        KeyParameter key = new KeyParameter(keyBytes);
        ParametersWithIV params = new ParametersWithIV(key, ivBytes);

        AESEngine engine = new AESEngine();
        SICBlockCipher ctrEngine = new SICBlockCipher(engine);

        ctrEngine.init(true, params);

        // encrypt
        byte[] cipher = new byte[plainBytes.length];
        ctrEngine.processBytes(plainBytes, 0, plainBytes.length, cipher, 0);
        log.info("cipher: {}", Hex.toHexString(cipher));

        // decrypt
        byte[] output = new byte[cipher.length];
        ctrEngine.init(false, params);
        ctrEngine.processBytes(cipher, 0, cipher.length, output, 0);
        log.info("plain: {}", Hex.toHexString(output));

        assertArrayEquals(plainBytes, output);
    }

    // key encryption
    @Test
    public void testKeyEncryption3() {

        final int AES_KEYLENGTH = 128;

        // key generation
        ECKey ecKey = new ECKey();
        byte[] priKey = ecKey.getPrivKeyBytes();

        // password generation using KDF
        String password = "Aa1234567890#";
        byte[] kdf = Password.generateKeyDerivation(password.getBytes(), 32);
        byte[] keyBytes = ByteUtil.parseBytes(kdf, 0, 32);

        // generate iv
        byte[] ivBytes = new byte[AES_KEYLENGTH / 8];
        SecureRandom prng = new SecureRandom();
        prng.nextBytes(ivBytes);

        byte[] plainBytes = "01234567890123456789".getBytes();
        log.info("plain: {}", Hex.toHexString(plainBytes));

        KeyParameter key = new KeyParameter(keyBytes);
        ParametersWithIV params = new ParametersWithIV(key, ivBytes);

        AESEngine engine = new AESEngine();
        SICBlockCipher ctrEngine = new SICBlockCipher(engine);

        ctrEngine.init(true, params);

        // encrypt
        byte[] cipher = new byte[plainBytes.length];
        ctrEngine.processBytes(plainBytes, 0, plainBytes.length, cipher, 0);
        log.info("cipher: {}", Hex.toHexString(cipher));

        // final encrypt data : iv+encData
        byte[] finalData = new byte[ivBytes.length + cipher.length];
        System.arraycopy(ivBytes, 0, finalData, 0, ivBytes.length);
        System.arraycopy(cipher, 0, finalData, ivBytes.length, cipher.length);

        // decrypt
        byte[] ivNew = new byte[AES_KEYLENGTH / 8];
        System.arraycopy(finalData, 0, ivNew, 0, ivNew.length);
        byte[] encData = new byte[finalData.length - ivNew.length];
        System.arraycopy(finalData, ivNew.length, encData, 0, encData.length);

        ParametersWithIV params2 = new ParametersWithIV(key, ivNew);
        AESEngine engine2 = new AESEngine();
        SICBlockCipher ctrEngine2 = new SICBlockCipher(engine2);

        byte[] output = new byte[encData.length];
        ctrEngine2.init(false, params2);
        ctrEngine2.processBytes(encData, 0, encData.length, output, 0);
        log.info("plain: {}", Hex.toHexString(output));

        assertArrayEquals(plainBytes, output);
    }

    // encrypt test
    @Test
    public void testAESEncryption() throws InvalidCipherTextException {

        // password generation using KDF
        String password = "Aa1234567890#";
        byte[] kdf = Password.generateKeyDerivation(password.getBytes(), 32);

        byte[] plainBytes = "01234567890123450123456789012345345".getBytes();
        log.info("plain: {}", Hex.toHexString(plainBytes));

        byte[] encData = AESEncrypt.encrypt(plainBytes, kdf);
        log.info("encrypt: {}", Hex.toHexString(encData));

        byte[] plainData = AESEncrypt.decrypt(encData, kdf);
        log.info("decrypt: {}", Hex.toHexString(plainData));

    }

    // save the encKey as file
    @Test
    public void testKeySave() throws IOException, InvalidCipherTextException {

        // generate key & save a file
        Wallet wt = new Wallet("/tmp/", "Password1234!");

        byte[] encData = FileUtil.readFile(wt.getPath(), wt.getKeyName());
        System.out.println("path:" + wt.getPath() + wt.getKeyName());
        System.out.println("encData:" + Hex.toHexString(encData));
        System.out.println("pubKey:" + Hex.toHexString(wt.getKey().getPubKey()));
        System.out.println("priKey:" + Hex.toHexString(wt.getKey().getPrivKeyBytes()));

        // load key
        Wallet wt2 = new Wallet("/tmp/", wt.getKeyName(), "Password1234!");
        System.out.println("pubKey2:" + Hex.toHexString(wt2.getKey().getPubKey()));
        System.out.println("priKey2:" + Hex.toHexString(wt2.getKey().getPrivKeyBytes()));

        assertArrayEquals(wt.getKey().getPrivKeyBytes(), wt2.getKey().getPrivKeyBytes());
    }


}
