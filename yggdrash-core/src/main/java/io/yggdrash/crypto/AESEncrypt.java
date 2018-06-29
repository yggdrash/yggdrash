package io.yggdrash.crypto;

import java.security.SecureRandom;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.crypto.engines.AESEngine;
import org.spongycastle.crypto.modes.CBCBlockCipher;
import org.spongycastle.crypto.paddings.PKCS7Padding;
import org.spongycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.crypto.params.ParametersWithIV;

/**
 * AESEncrypt Class
 * AES CBC PKCS7Padding.
 */
public class AESEncrypt {

  //todo: security check

  private static int AES_KEYLENGTH = 128;

  /**
   * encrypt data as AES.
   * @param plain plain data
   * @param key key data
   * @return encrypted data
   * @throws InvalidCipherTextException InvalidCipherTextException
   */
  public static byte[] encrypt(byte[] plain, byte[] key) throws InvalidCipherTextException {

    //todo: exception catch for security
    PaddedBufferedBlockCipher bbc =
        new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESEngine()), new PKCS7Padding());
    KeyParameter kp = new KeyParameter(key);
    byte[] ivBytes = new byte[AES_KEYLENGTH / 8];
    SecureRandom prng = new SecureRandom();
    prng.nextBytes(ivBytes);

    bbc.init(true, new ParametersWithIV(kp,ivBytes));
    byte[] encData = new byte[bbc.getOutputSize(plain.length)];
    int len = bbc.processBytes(plain, 0, plain.length, encData, 0);
    len += bbc.doFinal(encData, len);

    byte[] ivEncData = new byte[len + ivBytes.length];
    System.arraycopy(ivBytes, 0, ivEncData, 0, ivBytes.length);
    System.arraycopy(encData, 0, ivEncData, ivBytes.length, encData.length);

    return ivEncData;

  }

  /**
   * decrypt data as AES.
   * @param ivEncData encrypted data with iv (iv(16) + encData)
   * @param key key data
   * @return plain data
   * @throws InvalidCipherTextException InvalidCipherTextException
   */
  public static byte[] decrypt(byte[] ivEncData, byte[] key) throws InvalidCipherTextException {

    //todo: exception catch for security
    PaddedBufferedBlockCipher bbc =
        new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESEngine()), new PKCS7Padding());
    KeyParameter kp = new KeyParameter(key);
    byte[] ivBytes = new byte[AES_KEYLENGTH / 8];
    System.arraycopy(ivEncData, 0, ivBytes, 0, ivBytes.length);

    byte[] encData = new byte[ivEncData.length - ivBytes.length];
    System.arraycopy(ivEncData, ivBytes.length, encData, 0, encData.length);

    bbc.init(false, new ParametersWithIV(kp,ivBytes));
    byte[] plainData = new byte[bbc.getOutputSize(encData.length)];
    int len = bbc.processBytes(encData, 0, encData.length, plainData, 0);
    len += bbc.doFinal(plainData, len);

    byte[] removePadding = new byte[len];
    System.arraycopy(plainData, 0, removePadding, 0, len);

    return removePadding;

  }

}
