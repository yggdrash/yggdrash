package io.yggdrash.contract;

import com.google.gson.JsonObject;
import io.yggdrash.common.crypto.ECKey;
import io.yggdrash.common.crypto.HashUtil;
import io.yggdrash.common.crypto.HexUtil;
import io.yggdrash.common.utils.ByteUtil;
import io.yggdrash.common.utils.FileUtil;
import io.yggdrash.common.utils.JsonUtil;
import io.yggdrash.core.wallet.AesEncrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.util.encoders.Hex;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class TestWallet {

    private static final Logger log = LoggerFactory.getLogger(TestWallet.class);

    private static final String WALLET_PBKDF2_NAME = "pbkdf2";
    private static final int WALLET_PBKDF2_ITERATION = 262144;
    private static final int WALLET_PBKDF2_DKLEN = 32;
    private static final String WALLET_PBKDF2_PRF = "hmac-sha256";
    private static final String WALLET_PBKDF2_HMAC_HASH = "KECCAK-256";
    private static final String WALLET_PBKDF2_ALGORITHM = "SHA-256";
    private static final String WALLET_KEY_ENCRYPT_ALGORITHM = "aes-128-cbc";

    private ECKey key;
    private String keyPath;
    private String keyName;
    private byte[] address;
    private byte[] publicKey;

    public TestWallet(File file, String password) throws IOException, InvalidCipherTextException {
        decryptKeyFileInit(file, password);

    }

    public byte[] sign(byte[] data, boolean hashed) {
        ECKey.ECDSASignature signature = null;
        if (hashed) {
            signature = key.sign(data);
        } else {
            signature = key.sign(HashUtil.sha3(data));
        }
        return signature.toBinary();

    }

    private void decryptKeyFileInit(File keyFile, String password)
            throws IOException, InvalidCipherTextException {
        String json = FileUtil.readFileToString(keyFile, FileUtil.DEFAULT_CHARSET);
        JsonObject keyJsonObject = JsonUtil.parseJsonObject(json);

        byte[] salt = Hex.decode(getCryptoJsonObect(keyJsonObject)
                .getAsJsonObject("kdfparams")
                .get("salt")
                .getAsString());
        byte[] kdfPass = HashUtil.pbkdf2(
                password.getBytes(),
                salt,
                WALLET_PBKDF2_ITERATION,
                WALLET_PBKDF2_DKLEN,
                WALLET_PBKDF2_ALGORITHM);
        byte[] encData = Hex.decode(getCryptoJsonObect(keyJsonObject)
                .get("ciphertext")
                .getAsString());

        byte[] newMac = HashUtil.hash(
                ByteUtil.merge(ByteUtil.parseBytes(kdfPass, 16, 16), encData),
                WALLET_PBKDF2_HMAC_HASH);
        byte[] mac = Hex.decode(getCryptoJsonObect(keyJsonObject)
                .get("mac")
                .getAsString());
        if (!Arrays.equals(newMac, mac)) {
            throw new InvalidCipherTextException("mac is not valid");
        }

        byte[] iv = Hex.decode(getCryptoJsonObect(keyJsonObject)
                .getAsJsonObject("cipherparams")
                .get("iv")
                .getAsString());

        byte[] priKey = AesEncrypt.decrypt(
                encData, ByteUtil.parseBytes(kdfPass, 0, 16), iv);
        this.key = ECKey.fromPrivate(priKey);
        this.keyPath = keyPath;
        this.keyName = keyName;
        this.address = key.getAddress();
        this.publicKey = key.getPubKey();
    }

    private JsonObject getCryptoJsonObect(JsonObject keyJsonObject) {
        return keyJsonObject.getAsJsonObject("crypto");
    }

    public boolean verify(byte[] data, byte[] signature, boolean isHashed) {
        ECKey.ECDSASignature sig = new ECKey.ECDSASignature(signature);
        if (isHashed) {
            return key.verify(data, sig);
        } else {
            return key.verify(HashUtil.sha3(data), sig);
        }

    }

    public byte[] getAddress() {
        return this.address;
    }

    public String getAddressHexString() {
        return HexUtil.toHexString(getAddress());
    }
}