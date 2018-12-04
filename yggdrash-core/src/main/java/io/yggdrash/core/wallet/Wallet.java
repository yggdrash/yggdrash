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
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.common.crypto.AESEncrypt;
import io.yggdrash.common.crypto.ECKey;
import io.yggdrash.common.crypto.HashUtil;
import io.yggdrash.common.crypto.Password;
import io.yggdrash.common.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.util.encoders.Hex;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
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

        generateKeyFile(key, keyPath, keyName, password);
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

        loadFromKeyFile(keyPath, keyName, password);
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
            logger.error("Empty keyPath or keyPassword");
            throw new IllegalArgumentException("Empty keyPath or keyPassword");
        } else {
            Path path = Paths.get(keyFilePathName);
            String keyPath = path.getParent().toString();
            String keyName = path.getFileName().toString();

            try {
                loadFromKeyFile(keyPath, keyName, keyPassword);
            } catch (InvalidCipherTextException ice) {
                logger.warn("Invalid cipherText");
                throw ice;
            } catch (Exception e) {
                logger.warn("Key file is not exist.");
                generateKeyFile(new ECKey(), keyPath, keyName, keyPassword);
            }
        }
    }

    private void generateKeyFile(ECKey key, String keyPath, String keyName, String password)
            throws IOException, InvalidCipherTextException {
        // check password validation
        if (!Password.passwordValid(password)) {
            logger.error("Invalid keyPassword format"
                    + "(length:12-32, 1 more lower/upper/digit/special");
            throw new IllegalArgumentException("Invalid keyPassword format");
        }

        if (key == null) {
            key = new ECKey();
        }

        try {
            byte[] kdfPass = Password.generateKeyDerivation(password.getBytes(), 32);
            byte[] encData = AESEncrypt.encrypt(key.getPrivKeyBytes(), kdfPass);

            File file = new File(keyPath, keyName);
            Set<PosixFilePermission> perms = new HashSet<>();

            if (file.exists()) {
                perms.add(PosixFilePermission.OWNER_WRITE);
                Files.setPosixFilePermissions(file.toPath(), perms);
            }

            FileUtil.writeFile(file, encData);

            perms = new HashSet<>();
            perms.add(PosixFilePermission.OWNER_READ);
            Files.setPosixFilePermissions(file.toPath(), perms);
            logger.info("Created a new key file at " + file.getAbsolutePath());

            setKeyFileInfo(encData, kdfPass, keyPath, keyName);
        } catch (IOException ioe) {
            logger.error("Cannot generate the Key file at " + keyPath + File.separator + keyName);
            throw new IOException("Cannot generate the Key file");
        } catch (InvalidCipherTextException ice) {
            logger.error("Error InvalidCipherTextException: " + keyPath + File.separator + keyName);
            throw new InvalidCipherTextException("Error InvalidCipherTextException");
        }
    }

    private void loadFromKeyFile(String keyPath, String keyName, String password)
            throws IOException, InvalidCipherTextException {

        byte[] encData = FileUtil.readFile(keyPath, keyName);
        byte[] kdfPass = Password.generateKeyDerivation(password.getBytes(), 32);

        setKeyFileInfo(encData, kdfPass, keyPath, keyName);
    }

    private void setKeyFileInfo(byte[] encData, byte[] kdfPass, String keyPath, String keyName)
            throws InvalidCipherTextException {
        byte[] priKey = AESEncrypt.decrypt(encData, kdfPass);
        this.key = ECKey.fromPrivate(priKey);
        this.keyPath = keyPath;
        this.keyName = keyName;
        this.address = key.getAddress();
        this.publicKey = key.getPubKey();
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
    byte[] sign(byte[] data) {
        return key.sign(HashUtil.sha3(data)).toBinary();
    }

    /**
     * Sign the hashed data by sha3().
     *
     * @param hashedData hashed data
     * @return signature as byte[65]
     */
    public byte[] signHashedData(byte[] hashedData) {
        return key.sign(hashedData).toBinary();
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

        if (pubKey != null && !Arrays.equals(ecKeyPub.getPubKey(), pubKey)) {
            logger.debug("Invalid signature");
            return false;
        }

        return ecKeyPub.verify(hashedData, ecdsaSignature);
    }

    /**
     * Gets node id.
     *
     * @return the node id string
     */
    public String getNodeId() {
        return Hex.toHexString(key.getNodeId());
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