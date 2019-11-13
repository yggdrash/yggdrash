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
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import io.yggdrash.TestConstants;
import io.yggdrash.common.config.Constants;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.common.crypto.ECKey;
import io.yggdrash.common.crypto.HashUtil;
import io.yggdrash.common.utils.FileUtil;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;

import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class WalletTest {
    private static final Logger log = LoggerFactory.getLogger(WalletTest.class);

    private static final String ALPHA_CAPS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String ALPHA = "abcdefghijklmnopqrstuvwxyz";
    private static final String NUMERIC = "0123456789";
    private static final String SPECIAL_CHARS = "!@#$%^&*_=+-/";

    @Test
    public void testKeySave() throws IOException, InvalidCipherTextException {
        // generate key & save a file
        Wallet wallet = new Wallet(null, "tmp/", "nodePri.key", "Aa1234567890!");

        byte[] encData = FileUtil.readFile(wallet.getKeyPath(), wallet.getKeyName());
        log.debug("path:" + wallet.getKeyPath() + wallet.getKeyName());
        log.debug("encData:" + Hex.toHexString(encData));
        log.debug("pubKey:" + Hex.toHexString(wallet.getPubicKey()));

        // load key
        Wallet wallet1 = new Wallet("tmp/", wallet.getKeyName(), "Aa1234567890!");
        log.debug("pubKey2:" + Hex.toHexString(wallet1.getPubicKey()));

        assertArrayEquals(wallet.getPubicKey(), wallet1.getPubicKey());
    }

    @Test
    public void testKeyGenerationWithConsole() throws IOException, InvalidCipherTextException {
        TestConstants.ConsoleTest.apply();

        // generate key & save a file
        Wallet wallet = new Wallet((ECKey) null, "tmp/nodePri.key");

        byte[] encData = FileUtil.readFile(wallet.getKeyPath(), wallet.getKeyName());
        log.debug("path:" + wallet.getKeyPath() + wallet.getKeyName());
        log.debug("encData:" + Hex.toHexString(encData));
        log.debug("pubKey:" + Hex.toHexString(wallet.getPubicKey()));

        // load key
        Wallet wallet1 = new Wallet(wallet.getKeyPath() + wallet.getKeyName());
        log.debug("pubKey2:" + Hex.toHexString(wallet1.getPubicKey()));
        assertArrayEquals(wallet.getPubicKey(), wallet1.getPubicKey());
    }

    /**
     * Check a wallet generation with filepath & filename.
     */
    @Test
    public void testWalletGeneration() {
        DefaultConfig defaultConfig = new DefaultConfig();
        String keyFilePath = defaultConfig.getString(Constants.YGGDRASH_KEY_PATH);

        assertFalse("Check yggdrash.conf(yggdrash.key.path & yggdrash.key.password)",
                Strings.isNullOrEmpty(keyFilePath));
        log.debug("Private key: " + keyFilePath);

        String password = "Aa1234567890!";

        // check whether the key file exists
        Path path = Paths.get(keyFilePath);
        String keyPath = path.getParent().toString();
        String keyName = path.getFileName().toString();

        Wallet wallet = null;
        try {
            wallet = new Wallet(keyPath, keyName, password);
        } catch (Exception e) {
            log.error("Key file is not exist");

            try {
                wallet = new Wallet(null, keyPath, keyName, password);
            } catch (Exception ex) {
                ex.printStackTrace();
                fail("Wallet Exception");
            }
        }

        log.debug("wallet= " + wallet.toString());
    }

    /**
     * Check a wallet generation with filePathName.
     */
    @Test
    public void testWalletGenerationWithFilePath() {
        DefaultConfig defaultConfig = new DefaultConfig();
        String keyFilePath = defaultConfig.getString(Constants.YGGDRASH_KEY_PATH);

        assertFalse("Check yggdrash.conf(key.path & key.password)",
                Strings.isNullOrEmpty(keyFilePath));
        log.debug("Private key: " + keyFilePath);

        String password = "Aa1234567890!";

        // check whether the key file exists
        Path path = Paths.get(keyFilePath);
        String keyPath = path.getParent().toString();
        String keyName = path.getFileName().toString();

        Wallet wallet = null;
        try {
            wallet = new Wallet(keyFilePath, password);
        } catch (Exception e) {
            log.debug("Key file is not exist");

            try {
                wallet = new Wallet(null, keyPath, keyName, password);
            } catch (Exception ex) {
                ex.printStackTrace();
                fail();
            }
        }

        log.debug("wallet= " + wallet.toString());
    }

    @Test
    public void testWalletSignAndVerify() throws IOException, InvalidCipherTextException {
        Wallet wallet = new Wallet(null, "tmp/temp", "temp.key", "Aa1234567890!");

        byte[] plain = "test data 1111".getBytes();
        log.debug("Plain data: " + new String(plain));

        byte[] signature = wallet.sign(plain);
        log.debug("Signature: " + Hex.toHexString(signature));

        boolean verifyResult = wallet.verify(plain, signature);
        log.debug("Verify Result: " + verifyResult);
        assertTrue(verifyResult);

        byte[] plain2 = "test data 2222".getBytes();
        verifyResult = wallet.verify(plain2, signature);
        log.debug("Verify Result: " + verifyResult);
        assertFalse(verifyResult);

        verifyResult = wallet.verifyHashedData(HashUtil.sha3(plain), signature);
        log.debug("Verify Result: " + verifyResult);
        assertTrue(verifyResult);

        verifyResult = Wallet.verify(HashUtil.sha3(plain), signature, true, wallet.getPubicKey());
        log.debug("Verify Result: " + verifyResult);
        assertTrue(verifyResult);

        verifyResult = Wallet.verify(plain, signature, false, wallet.getPubicKey());
        log.debug("Verify Result: " + verifyResult);
        assertTrue(verifyResult);

        verifyResult = Wallet.verify(plain, signature, false, wallet.getPubicKey());
        log.debug("Verify Result: " + verifyResult);
        assertTrue(verifyResult);

        verifyResult = Wallet.verify(HashUtil.sha3(plain2), signature, true, wallet.getPubicKey());
        log.debug("Verify Result: " + verifyResult);
        assertFalse(verifyResult);
    }

    @Test
    public void testWalletConstructor() throws IOException, InvalidCipherTextException {
        DefaultConfig config = new DefaultConfig();

        Wallet wallet = new Wallet(config, "Aa1234567890!");

        log.debug(wallet.toString());
        assertNotNull(wallet);

        byte[] data = "sign data".getBytes();
        byte[] signature = wallet.sign(data);
        boolean verifyResult = wallet.verify(data, signature);

        assertTrue(verifyResult);
    }

    @Test
    public void testWalletConstructorWithConsole() throws IOException, InvalidCipherTextException {
        TestConstants.ConsoleTest.apply();

        DefaultConfig config = new DefaultConfig();

        Wallet wallet = new Wallet(config);

        log.debug(wallet.toString());
        assertNotNull(wallet);

        byte[] data = "sign data".getBytes();
        byte[] signature = wallet.sign(data);
        boolean verifyResult = wallet.verify(data, signature);

        assertTrue(verifyResult);
    }

    @Test
    public void testWalletAndConfig() throws IOException, InvalidCipherTextException {
        DefaultConfig config = new DefaultConfig();

        Wallet wallet = new Wallet(config, "Aa1234567890!");

        Path path = Paths.get(config.getKeyPath());
        String keyPath = path.getParent().toString();
        String keyName = path.getFileName().toString();

        log.debug("walletKeyPath: " + wallet.getKeyPath());
        log.debug("walletKeyName: " + wallet.getKeyName());

        log.debug("configKeyPath: " + keyPath);
        log.debug("configKeyName: " + keyName);

        assertEquals(wallet.getKeyPath(), keyPath);
        assertEquals(wallet.getKeyName(), keyName);
    }

    @Test
    public void createKeyFile() throws IOException, InvalidCipherTextException {
        TestConstants.PerformanceTest.apply();

        int count = 30;
        int passwordLen = 20;
        String keyPath = "newKeystore";
        String keyFilePath = "/nodePri";
        String passwordFilePath = "/password";

        for (int i = 1; i <= count; i++) {
            String password = generatePassword(passwordLen, ALPHA_CAPS + ALPHA + NUMERIC + SPECIAL_CHARS);

            Wallet wallet = new Wallet((ECKey) null, keyPath + keyFilePath + i + ".key", password);
            FileUtil.writeFile(keyPath, passwordFilePath + i + ".txt", password.getBytes());
        }
    }

    private String generatePassword(int len, String dic) {
        SecureRandom random = new SecureRandom();
        String result = "";
        result += ALPHA_CAPS.charAt(random.nextInt(1));
        result += ALPHA.charAt(random.nextInt(1));
        result += NUMERIC.charAt(random.nextInt(1));
        result += SPECIAL_CHARS.charAt(random.nextInt(1));

        for (int i = 0; i < len - 4; i++) {
            int index = random.nextInt(dic.length());
            result += dic.charAt(index);
        }
        return result;
    }


    @Test
    public void shoudNotBeRewriteWalletbyWrongPassword() throws IOException, InvalidCipherTextException {
        String keyPath = "./tmp/";
        String keyName = new SimpleDateFormat("yyyyMMdd-hhmmss.SSS'.key'").format(new Date());
        String keyPathName = keyPath + new Object() {
        }.getClass().getEnclosingMethod().getName() + keyName;
        String password1 = "Aa1234567890!";
        String password2 = "Aa1234567890@";

        // create new wallet
        Wallet wallet1 = new Wallet((ECKey) null, keyPathName, password1);

        // load wallet by wrong password
        try {
            Wallet wallet2 = new Wallet(keyPathName, password2);
        } catch (InvalidCipherTextException ie) {
            assert true;
            return;
        } catch (Exception e) {
            assert false;
        }

        assert false;
    }

    @Test
    public void shoudBeOkWalletbyCorrectPassword() throws IOException, InvalidCipherTextException {
        String keyPath = "./tmp/";
        String keyName = new SimpleDateFormat("yyyyMMdd-hhmmss.SSS'.key'").format(new Date());
        String keyPathName = keyPath + new Object() {
        }.getClass().getEnclosingMethod().getName() + keyName;
        String password1 = "Aa1234567890!";
        String password2 = "Aa1234567890@";

        // create new wallet
        Wallet wallet1 = new Wallet((ECKey) null, keyPathName, password1);

        // load wallet by correct password
        Wallet wallet2 = null;
        try {
            wallet2 = new Wallet(keyPathName, password1);
        } catch (Exception e) {
            assert false;
        }

        if (wallet2 != null) {
            log.debug("Wallet: " + wallet2.toString());
            assert true;
        } else {
            assert false;
        }
    }

    @Test
    public void shoudNotBeRewriteWalletbyWrongPasswordInDefaultConfig() throws IOException, InvalidCipherTextException {
        String keyPath = "./tmp/";
        String keyName = new SimpleDateFormat("yyyyMMdd-hhmmss.SSS'.key'").format(new Date());
        String keyPathName = keyPath + keyName;
        String password1 = "Aa1234567890$";
        String password2 = "Aa1234567890@";

        DefaultConfig newConfig1 = new DefaultConfig();
        newConfig1.setConfig(
                newConfig1.getConfig()
                        .withValue(Constants.YGGDRASH_KEY_PATH, ConfigValueFactory.fromAnyRef(keyPathName))
                        .withValue(Constants.YGGDRASH_KEY_PASSWORD, ConfigValueFactory.fromAnyRef(password1)));
        log.debug(newConfig1.getString(Constants.YGGDRASH_KEY_PASSWORD));

        // create new wallet
        Wallet wallet1 = new Wallet(newConfig1);
        log.debug("Wallet1: " + wallet1.toString());

        DefaultConfig newConfig2 = new DefaultConfig();
        newConfig2.setConfig(
                newConfig2.getConfig()
                        .withValue(Constants.YGGDRASH_KEY_PATH, ConfigValueFactory.fromAnyRef(keyPathName))
                        .withValue(Constants.YGGDRASH_KEY_PASSWORD, ConfigValueFactory.fromAnyRef(password2)));
        log.debug(newConfig2.getString(Constants.YGGDRASH_KEY_PASSWORD));

        // load wallet by wrong password
        Wallet wallet2 = null;
        try {
            wallet2 = new Wallet(newConfig2);
            log.debug("Wallet2: " + wallet2.toString());

        } catch (InvalidCipherTextException ie) {
            assert true;
            return;
        } catch (Exception e) {
            log.debug(e.getMessage());
            assert false;
        }

        if (wallet2 != null) {
            log.debug("Wallet: " + wallet2.toString());
            assert false;
        } else {
            assert true;
        }

    }

    @Test
    public void shoudBeOkWalletbyCorrectPasswordInDefaultConfig() throws IOException, InvalidCipherTextException {
        String keyPath = "./tmp/";
        String keyName = new SimpleDateFormat("yyyyMMdd-hhmmss.SSS'.key'").format(new Date());
        String keyPathName = keyPath + new Object() {
        }.getClass().getEnclosingMethod().getName() + keyName;
        String password1 = "Aa1234567890!";

        String keyParsingText = Constants.YGGDRASH_KEY_PATH + "=" + keyPathName;
        String keyPasswordParsingText1 = Constants.YGGDRASH_KEY_PASSWORD + "=" + "\"" + password1 + "\"";

        DefaultConfig defaultConfig = new DefaultConfig();
        DefaultConfig newConfig1 =
                new DefaultConfig(
                        ConfigFactory.parseString(keyParsingText).withFallback(
                                ConfigFactory.parseString(keyPasswordParsingText1)
                                        .withFallback(defaultConfig.getConfig()))
                                .resolve());

        // create new wallet
        Wallet wallet1 = new Wallet(newConfig1);

        // load wallet by correct password
        Wallet wallet2 = null;
        try {
            wallet2 = new Wallet(newConfig1, password1);
        } catch (Exception e) {
            assert false;
        }

        if (wallet2 != null) {
            log.debug("Wallet: " + wallet2.toString());
            assert true;
        } else {
            assert false;
        }
    }
}
