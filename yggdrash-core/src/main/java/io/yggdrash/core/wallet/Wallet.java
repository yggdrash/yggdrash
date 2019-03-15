/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.core.wallet;

import com.google.common.base.Strings;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.common.crypto.AESEncrypt;
import io.yggdrash.common.crypto.ECKey;
import io.yggdrash.common.crypto.HashUtil;
import io.yggdrash.common.crypto.HexUtil;
import io.yggdrash.common.crypto.Password;
import io.yggdrash.common.utils.ByteUtil;
import io.yggdrash.common.utils.FileUtil;
import io.yggdrash.common.utils.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.util.encoders.Hex;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Wallet Class.
 */
public class Wallet {

    // todo: check security

    private static final Logger logger = LoggerFactory.getLogger(Wallet.class);

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

    /**
     * Wallet Constructor(generate key file).
     *
     * @param key      ECKey
     * @param keyPath  keyPath(directory)
     * @param keyName  keyName
     * @param password password
     * @throws IOException                IOException
     * @throws InvalidCipherTextException InvalidCipherTextException
     */
    public Wallet(ECKey key, String keyPath, String keyName, String password)
            throws IOException, InvalidCipherTextException {
        if (!Password.passwordValid(password)) {
            logger.error("Invalid Password");
            throw new IOException("Invalid Password");
        }
        encryptKeyFileInit(key, keyPath, keyName, password);
    }

    /**
     * Wallet Constructor as loading the key file.
     *
     * @param keyPath  keyPath(directory)
     * @param keyName  keyName
     * @param password password
     * @throws IOException                IOException
     * @throws InvalidCipherTextException InvalidCipherTextException
     */
    public Wallet(String keyPath, String keyName, String password)
            throws IOException, InvalidCipherTextException {
        decryptKeyFileInit(keyPath, keyName, password);
    }

    /**
     * Wallet constructor.
     *
     * @param keyPathName key file path + name
     * @param password    password
     * @throws IOException                IOException
     * @throws InvalidCipherTextException InvalidCipherTextException
     */
    public Wallet(String keyPathName, String password)
            throws IOException, InvalidCipherTextException {

        this(FileUtil.getFilePath(keyPathName), FileUtil.getFileName(keyPathName), password);
    }

    /**
     * Wallet constructor.
     *
     * @throws IOException                IOException
     * @throws InvalidCipherTextException InvalidCipherTextException
     */
    public Wallet() throws IOException, InvalidCipherTextException {
        this(new DefaultConfig());
    }

    /**
     * Wallet constructor with DefaultConfig.
     *
     * @param config DefaultConfig
     * @throws IOException                IOException
     * @throws InvalidCipherTextException InvalidCipherTextException
     */
    public Wallet(DefaultConfig config) throws IOException, InvalidCipherTextException {
        //todo: change password logic to CLI for security

        String keyFilePathName = config.getKeyPath();
        String keyPassword = config.getKeyPassword();

        if (Strings.isNullOrEmpty(keyFilePathName) || Strings.isNullOrEmpty(keyPassword)) {
            logger.error("Invalid keyPath or keyPassword");
            throw new IOException("Invalid keyPath or keyPassword");
        } else {
            // check password validation
            if (!Password.passwordValid(keyPassword)) {
                logger.error("Invalid keyPassword format"
                        + "(length:12-32, 1 more lower/upper/digit/special");
                throw new IOException("Invalid keyPassword format");
            }

            Path path = Paths.get(keyFilePathName);
            String keyPath = path.getParent().toString();
            String keyName = path.getFileName().toString();

            try {
                decryptKeyFileInit(keyPath, keyName, keyPassword);
            } catch (Exception e) {
                logger.debug("Key file is not exist. Create New key file.");

                try {
                    encryptKeyFileInit(key, keyPath, keyName, keyPassword);
                } catch (IOException ioe) {
                    logger.error("Cannot generate the Key file at " + keyPath + keyName);
                    throw new IOException("Cannot generate the Key file");
                } catch (InvalidCipherTextException ice) {
                    logger.error("Error InvalidCipherTextException: " + keyPath + keyName);
                    throw new InvalidCipherTextException("Error InvalidCipherTextException");
                }
            }
        }

    }

    /**
     * Get wallet file keyPath.
     *
     * @return keyPath
     */
    String getKeyPath() {
        return keyPath;
    }

    /**
     * Get keyName(filename, address).
     *
     * @return key name(filename)
     */
    String getKeyName() {
        return keyName;
    }

    /**
     * Get public key
     *
     * @return public key
     */
    public byte[] getPubicKey() {
        return key.getPubKey();
    }

    public String getPubicKeyHex() {
        return Hex.toHexString(key.getPubKey());
    }

    /**
     * Get address as byte[20]
     *
     * @return address as byte[20]
     */
    public byte[] getAddress() {
        return this.address;
    }

    /**
     * Get hex address
     *
     * @return hex address
     */
    public String getHexAddress() {
        return Hex.toHexString(address);
    }

    /**
     * Sign the plain data.
     *
     * @param data plain data
     * @return signature as byte[65]
     */
    public byte[] sign(byte[] data) {
        return this.sign(data, false);
    }

    /**
     * Sign the hashed data by sha3().
     *
     * @param hashedData hashed data
     * @return signature as byte[65]
     * @deprecated use sign(byte[] data, boolean hashed)
     */
    public byte[] signHashedData(byte[] hashedData) {
        return this.sign(hashedData, true);
    }

    /**
     * Sign data.
     *
     * @param data   plain data
     * @param hashed whether hashed or not
     * @return signature as byte[65]
     */
    public byte[] sign(byte[] data, boolean hashed) {
        if (hashed) {
            return key.sign(data).toBinary();
        } else {
            return key.sign(HashUtil.sha3(data)).toBinary();
        }
    }

    /**
     * Sign data as hex string.
     *
     * @param data   plain data
     * @param hashed whether hashed or not
     * @return signature as hex string
     */
    public String signHex(byte[] data, boolean hashed) {
        return HexUtil.toHexString(this.sign(data, hashed));
    }

    /**
     * Verify the signature with hashed data.
     *
     * @param hashedData hashed Data
     * @param signature  signature
     * @return verification result
     */
    boolean verifyHashedData(byte[] hashedData, byte[] signature) {
        ECKey.ECDSASignature sig = new ECKey.ECDSASignature(signature);
        return key.verify(hashedData, sig);
    }

    /**
     * Verify the signature with plain data.
     *
     * @param data      plain data for signed
     * @param signature signature
     * @return verification result
     */
    public boolean verify(byte[] data, byte[] signature) {
        ECKey.ECDSASignature sig = new ECKey.ECDSASignature(signature);
        return key.verify(HashUtil.sha3(data), sig);
    }

    /**
     * Verify the signature as static
     *
     * @param data signed data
     * @param signature  signature
     * @param hashed whether hashed data or not
     * @return verification result
     */
    public static boolean verify(byte[] data, byte[] signature, boolean hashed) {
        // todo: check pubkey or delete method
        return verify(data, signature, hashed, null);
    }

    /**
     * Verify the signature with public key
     *
     * @param data signed data
     * @param signature  signature
     * @param hashed whether hashed data or not
     * @param pubKey public key for verifying
     * @return verification result
     */
    public static boolean verify(byte[] data, byte[] signature, boolean hashed, byte[] pubKey) {
        ECKey.ECDSASignature ecdsaSignature = new ECKey.ECDSASignature(signature);
        byte[] hashedData = hashed ? data : HashUtil.sha3(data);
        ECKey ecKeyPub;
        try {
            ecKeyPub = ECKey.signatureToKey(hashedData, ecdsaSignature);
        } catch (SignatureException e) {
            logger.debug("Invalid signature" + e.getMessage());
            return false;
        }

        //todo: check pubkey
        if (pubKey != null && !Arrays.equals(ecKeyPub.getPubKey(), pubKey)) {
            logger.debug("Invalid signature");
            return false;
        }

        return ecKeyPub.verify(hashedData, ecdsaSignature);
    }

    public static byte[] calculatePubKey(byte[] data, byte[] signature, boolean hashed) {
        ECKey.ECDSASignature ecdsaSignature = new ECKey.ECDSASignature(signature);
        byte[] hashedData = hashed ? data : HashUtil.sha3(data);
        ECKey ecKeyPub;
        try {
            ecKeyPub = ECKey.signatureToKey(hashedData, ecdsaSignature);
        } catch (SignatureException e) {
            logger.debug("Invalid signature" + e.getMessage());
            return null;
        }

        return ecKeyPub.getPubKey();
    }

    public static byte[] calculateAddress(byte[] publicKey) {
        return HashUtil.sha3omit12(
                Arrays.copyOfRange(publicKey, 1, publicKey.length));
    }

    /**
     * Gets node id.
     *
     * @return the node id string
     */
    public String getNodeId() {
        return Hex.toHexString(key.getNodeId());
    }

    private void encryptKeyFileInit(ECKey key, String keyPath, String keyName, String password)
            throws IOException, InvalidCipherTextException {
        if (key == null) {
            key = new ECKey();
        }

        this.key = key;
        this.keyPath = keyPath;
        this.keyName = keyName;
        this.address = key.getAddress();
        this.publicKey = key.getPubKey();

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
                key.getPrivKeyBytes(),
                ByteUtil.parseBytes(kdfPass, 0, 16),
                iv);
        byte[] mac = HashUtil.hash(
                ByteUtil.merge(ByteUtil.parseBytes(kdfPass, 16, 16), encData),
                WALLET_PBKDF2_HMAC_HASH);

        // file permission
        File file = new File(this.keyPath, this.keyName);
        Set<PosixFilePermission> perms = new HashSet<>();

        if (file.exists()) {
            perms.add(PosixFilePermission.OWNER_WRITE);
            Files.setPosixFilePermissions(file.toPath(), perms);
        }

        JsonObject keyJsonObject = makeKeyJsonObject(Hex.toHexString(this.address),
                Hex.toHexString(iv),
                Hex.toHexString(encData),
                Hex.toHexString(salt),
                Hex.toHexString(mac));

        iv = null;
        salt = null;

        FileUtil.writeFile(file,
                new GsonBuilder().setPrettyPrinting().create().toJson(keyJsonObject).getBytes());

        perms = new HashSet<>();
        perms.add(PosixFilePermission.OWNER_READ);
        Files.setPosixFilePermissions(file.toPath(), perms);
    }

    private JsonObject makeKeyJsonObject(
            String address, String iv, String encData, String salt, String mac) {
        JsonObject keyJsonObject = new JsonObject();
        keyJsonObject.addProperty("address", address);

        JsonObject cryptoJsonObject = new JsonObject();
        cryptoJsonObject.addProperty("cipher", WALLET_KEY_ENCRYPT_ALGORITHM);

        JsonObject ivJsonObject = new JsonObject();
        ivJsonObject.addProperty("iv", iv);

        cryptoJsonObject.add("cipherparams", ivJsonObject);
        cryptoJsonObject.addProperty("ciphertext", encData);
        cryptoJsonObject.addProperty("kdf", WALLET_PBKDF2_NAME);

        JsonObject kdfparamsJsonObject = new JsonObject();
        kdfparamsJsonObject.addProperty("c", WALLET_PBKDF2_ITERATION);
        kdfparamsJsonObject.addProperty("dklen", WALLET_PBKDF2_DKLEN);
        kdfparamsJsonObject.addProperty("prf", WALLET_PBKDF2_PRF);
        kdfparamsJsonObject.addProperty("salt", salt);

        cryptoJsonObject.add("kdfparams", kdfparamsJsonObject);
        cryptoJsonObject.addProperty("mac", mac);
        keyJsonObject.add("crypto", cryptoJsonObject);

        return keyJsonObject;
    }

    private void decryptKeyFileInit(String keyPath, String keyName, String password)
            throws IOException, InvalidCipherTextException {
        File keyFile = FileUtil.getFile(keyPath, keyName);
        String json = FileUtil.readFileToString(keyFile, StandardCharsets.UTF_8);
        JsonObject keyJsonObject = JsonUtil.parseJsonObject(json);

        byte[] salt = Hex.decode(keyJsonObject.getAsJsonObject("crypto")
                .getAsJsonObject("kdfparams")
                .get("salt")
                .getAsString());
        byte[] kdfPass = HashUtil.pbkdf2(
                password.getBytes(),
                salt,
                WALLET_PBKDF2_ITERATION,
                WALLET_PBKDF2_DKLEN,
                WALLET_PBKDF2_ALGORITHM);
        byte[] encData = Hex.decode(keyJsonObject.getAsJsonObject("crypto")
                .get("ciphertext")
                .getAsString());

        byte[] newMac = HashUtil.hash(
                ByteUtil.merge(ByteUtil.parseBytes(kdfPass, 16, 16), encData),
                WALLET_PBKDF2_HMAC_HASH);
        byte[] mac = Hex.decode(keyJsonObject.getAsJsonObject("crypto")
                .get("mac")
                .getAsString());
        if (!Arrays.equals(newMac, mac)) {
            throw new InvalidCipherTextException("mac is not valid");
        }

        byte[] iv = Hex.decode(keyJsonObject.getAsJsonObject("crypto")
                .getAsJsonObject("cipherparams")
                .get("iv")
                .getAsString());

        byte[] priKey = AESEncrypt.decrypt(
                encData, ByteUtil.parseBytes(kdfPass, 0, 16), iv);
        this.key = ECKey.fromPrivate(priKey);
        this.keyPath = keyPath;
        this.keyName = keyName;
        this.address = key.getAddress();
        this.publicKey = key.getPubKey();
    }

    @Override
    public String toString() {
        return "Wallet{"
                + "keyPath=" + keyPath
                + ", keyName=" + keyName
                + ", address=" + getHexAddress()
                + ", publicKey=" + Hex.toHexString(publicKey)
                + '}';
    }

}