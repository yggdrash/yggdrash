package io.yggdrash.core;

import io.yggdrash.crypto.AESEncrypt;
import io.yggdrash.crypto.ECKey;
import io.yggdrash.crypto.HashUtil;
import io.yggdrash.crypto.Password;
import io.yggdrash.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.util.encoders.Hex;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
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
     * Wallet Consturctor(generate key file).
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
            logger.error("password invalid : " + password);
            throw new IOException("Invalid Password");
        }

        if (key == null) {
            key = new ECKey();
        }

        this.key = key;
        this.keyPath = keyPath;
        this.keyName = keyName;
        this.address = key.getAddress();
        this.publicKey = key.getPubKey();

        byte[] kdfPass = Password.generateKeyDerivation(password.getBytes(), 32);
        byte[] encData = AESEncrypt.encrypt(key.getPrivKeyBytes(), kdfPass);

        File file = new File(this.keyPath, this.keyName);
        Set<PosixFilePermission> perms = new HashSet<>();

        if (file.exists()) {
            perms.add(PosixFilePermission.OWNER_WRITE);
            Files.setPosixFilePermissions(file.toPath(), perms);
        }

        FileUtil.writeFile(file, encData);

        perms = new HashSet<>();
        perms.add(PosixFilePermission.OWNER_READ);
        Files.setPosixFilePermissions(file.toPath(), perms);
    }

    /**
     * Wallet Constructor as loading the keyfile.
     *
     * @param keyPath  keyPath(directory)
     * @param keyName  keyName
     * @param password password
     * @throws IOException                IOException
     * @throws InvalidCipherTextException InvalidCipherTextException
     */
    public Wallet(String keyPath, String keyName, String password)
            throws IOException, InvalidCipherTextException {

        byte[] encData = FileUtil.readFile(keyPath, keyName);
        byte[] kdfPass = Password.generateKeyDerivation(password.getBytes(), 32);

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
    public String getKeyPath() {
        return keyPath;
    }

    /**
     * Get keyName(filename, address).
     *
     * @return key name(filename)
     */
    public String getKeyName() {
        return keyName;
    }

    /**
     * Get public key
     * @return public key
     */
    public byte[] getPubicKey() {
        return key.getPubKey();
    }

    /**
     * Get address as byte[20]
     * @return address as byte[20]
     */
    public byte[] getAddress() {
        return this.address;
    }

    /**
     * Sign the data.
     *
     * @param data data for signning
     * @return signature as byte[65]
     */
    public byte[] sign(byte[] data) {
        return key.sign(HashUtil.sha3(data)).toBinary();
    }

    /**
     * Verify the sign data with data & signature.
     *
     * @param data data for signed
     * @param signature signature
     * @return verification result
     */
    public boolean verify(byte[] data, byte[] signature) {

        ECKey.ECDSASignature sig = new ECKey.ECDSASignature(signature);

        return key.verify(HashUtil.sha3(data), sig);
    }

    @Override
    public String toString() {
        return "Wallet{"
                + "keyPath=" + keyPath
                + ", keyName=" + keyName
                + ", address=" + Hex.toHexString(address)
                + ", publidKey=" + Hex.toHexString(publicKey)
                + '}';
    }

}