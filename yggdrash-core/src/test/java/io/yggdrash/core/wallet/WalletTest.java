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
import io.yggdrash.TestConstants.SlowTest;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.common.crypto.HashUtil;
import io.yggdrash.common.crypto.Password;
import io.yggdrash.common.util.FileUtil;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class WalletTest extends SlowTest {
    private static final Logger log = LoggerFactory.getLogger(WalletTest.class);

    @Test
    public void testKeySave() throws IOException, InvalidCipherTextException {
        // generate key & save a file
        Wallet wt = new Wallet(null, "/tmp/", "nodePri.key", "Password1234!");

        byte[] encData = FileUtil.readFile(wt.getKeyPath(), wt.getKeyName());
        log.debug("path:" + wt.getKeyPath() + wt.getKeyName());
        log.debug("encData:" + Hex.toHexString(encData));
        log.debug("pubKey:" + Hex.toHexString(wt.getPubicKey()));

        // load key
        Wallet wt2 = new Wallet("/tmp/", wt.getKeyName(), "Password1234!");
        log.debug("pubKey2:" + Hex.toHexString(wt2.getPubicKey()));

        assertArrayEquals(wt.getPubicKey(), wt2.getPubicKey());
    }

    /**
     * Check a wallet generation with filepath & filename.
     */
    @Test
    public void testWalletGeneration() {
        DefaultConfig defaultConfig = new DefaultConfig();
        String keyFilePath = defaultConfig.getString("key.path");
        String password = defaultConfig.getString("key.password");

        assertFalse("Check yggdrash.conf(key.path & key.password)",
                Strings.isNullOrEmpty(keyFilePath) || Strings.isNullOrEmpty(password));
        log.debug("Private key: " + keyFilePath);
        log.debug("Password : " + password); // for debug

        // check password validation
        boolean validPassword = Password.passwordValid(password);
        assertTrue("Password is not valid", validPassword);
        log.debug("Password is valid");

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
        String keyFilePath = defaultConfig.getString("key.path");
        String password = defaultConfig.getString("key.password");

        assertFalse("Check yggdrash.conf(key.path & key.password)",
                Strings.isNullOrEmpty(keyFilePath) || Strings.isNullOrEmpty(password));
        log.debug("Private key: " + keyFilePath);
        log.debug("Password : " + password); // for debug

        // check password validation
        boolean validPassword = Password.passwordValid(password);
        assertTrue("Password is not valid", validPassword);

        log.debug("Password is valid");

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

        verifyResult = wallet.verifyHashedData(HashUtil.sha3(plain), signature);
        log.debug("Verify Result: " + verifyResult);
        assertTrue(verifyResult);

        verifyResult = Wallet.verify(HashUtil.sha3(plain), signature, true);
        log.debug("Verify Result: " + verifyResult);
        assertTrue(verifyResult);

        verifyResult = Wallet.verify(plain, signature, false);
        log.debug("Verify Result: " + verifyResult);
        assertTrue(verifyResult);

        verifyResult = Wallet.verify(plain, signature, false, wallet.getPubicKey());
        log.debug("Verify Result: " + verifyResult);
        assertTrue(verifyResult);
    }

    @Test
    public void testWalletConstructor() throws IOException, InvalidCipherTextException {
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

        Wallet wallet = new Wallet(config);

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
}
