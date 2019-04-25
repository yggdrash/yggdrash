package io.yggdrash.core.wallet;

import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.crypto.engines.AESEngine;
import org.spongycastle.crypto.modes.CBCBlockCipher;
import org.spongycastle.crypto.paddings.PKCS7Padding;
import org.spongycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.crypto.params.ParametersWithIV;

import java.security.SecureRandom;

/**
 * AesEncrypt Class
 * AES CBC PKCS7Padding.
 */
public class AesEncrypt {

    private static final int AES_KEY_LENGTH = 128;

    private AesEncrypt() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * encrypt data as AES.
     *
     * @param plain plain data
     * @param key   key data
     * @return encrypted data
     * @throws InvalidCipherTextException InvalidCipherTextException
     */
    static byte[] encrypt(byte[] plain, byte[] key) throws InvalidCipherTextException {
        PaddedBufferedBlockCipher bbc = new PaddedBufferedBlockCipher(
                new CBCBlockCipher(new AESEngine()), new PKCS7Padding());
        KeyParameter kp = new KeyParameter(key);
        byte[] ivBytes = new byte[AES_KEY_LENGTH / 8];
        SecureRandom prng = new SecureRandom();
        prng.nextBytes(ivBytes);

        bbc.init(true, new ParametersWithIV(kp, ivBytes));
        byte[] encData = new byte[bbc.getOutputSize(plain.length)];
        int len = bbc.processBytes(plain, 0, plain.length, encData, 0);
        len += bbc.doFinal(encData, len);

        byte[] ivEncData = new byte[len + ivBytes.length];
        System.arraycopy(ivBytes, 0, ivEncData, 0, ivBytes.length);
        System.arraycopy(encData, 0, ivEncData, ivBytes.length, encData.length);

        return ivEncData;
    }


    /**
     * encrypt data as AES.
     *
     * @param plain plain data
     * @param key   key data
     * @param ivBytes iv data
     * @return encrypted data
     * @throws InvalidCipherTextException InvalidCipherTextException
     */
    public static byte[] encrypt(byte[] plain, byte[] key, byte[] ivBytes) throws InvalidCipherTextException {
        PaddedBufferedBlockCipher bbc = new PaddedBufferedBlockCipher(
                new CBCBlockCipher(new AESEngine()), new PKCS7Padding());
        KeyParameter kp = new KeyParameter(key);

        bbc.init(true, new ParametersWithIV(kp, ivBytes));
        byte[] encData = new byte[bbc.getOutputSize(plain.length)];
        int len = bbc.processBytes(plain, 0, plain.length, encData, 0);
        len += bbc.doFinal(encData, len);

        byte[] removePadding = new byte[len];
        System.arraycopy(encData, 0, removePadding, 0, len);

        return removePadding;
    }

    /**
     * decrypt data as AES.
     *
     * @param ivEncData encrypted data with iv (iv(16) + encData)
     * @param key       key data
     * @return plain data
     * @throws InvalidCipherTextException InvalidCipherTextException
     */
    static byte[] decrypt(byte[] ivEncData, byte[] key) throws InvalidCipherTextException {

        //todo: exception catch for security
        PaddedBufferedBlockCipher bbc = new PaddedBufferedBlockCipher(
                new CBCBlockCipher(new AESEngine()), new PKCS7Padding());
        KeyParameter kp = new KeyParameter(key);
        byte[] ivBytes = new byte[AES_KEY_LENGTH / 8];
        System.arraycopy(ivEncData, 0, ivBytes, 0, ivBytes.length);

        byte[] encData = new byte[ivEncData.length - ivBytes.length];
        System.arraycopy(ivEncData, ivBytes.length, encData, 0, encData.length);

        bbc.init(false, new ParametersWithIV(kp, ivBytes));
        byte[] plainData = new byte[bbc.getOutputSize(encData.length)];
        int len = bbc.processBytes(encData, 0, encData.length, plainData, 0);
        len += bbc.doFinal(plainData, len);

        byte[] removePadding = new byte[len];
        System.arraycopy(plainData, 0, removePadding, 0, len);

        return removePadding;
    }

    /**
     * decrypt data as AES.
     *
     * @param encData encrypted data
     * @param key       key data
     * @return plain data
     * @throws InvalidCipherTextException InvalidCipherTextException
     */
    public static byte[] decrypt(byte[] encData, byte[] key, byte[] ivBytes) throws InvalidCipherTextException {
        PaddedBufferedBlockCipher bbc = new PaddedBufferedBlockCipher(
                new CBCBlockCipher(new AESEngine()), new PKCS7Padding());
        KeyParameter kp = new KeyParameter(key);

        bbc.init(false, new ParametersWithIV(kp, ivBytes));
        byte[] plainData = new byte[bbc.getOutputSize(encData.length)];
        int len = bbc.processBytes(encData, 0, encData.length, plainData, 0);
        len += bbc.doFinal(plainData, len);

        byte[] removePadding = new byte[len];
        System.arraycopy(plainData, 0, removePadding, 0, len);

        return removePadding;
    }
}
