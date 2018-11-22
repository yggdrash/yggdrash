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

package io.yggdrash.node;

import io.yggdrash.common.crypto.AESEncrypt;
import io.yggdrash.common.crypto.ECKey;
import io.yggdrash.common.crypto.Password;
import io.yggdrash.common.util.ByteUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.util.encoders.Hex;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.context.junit4.SpringRunner;

import static io.yggdrash.common.crypto.HashUtil.sha3;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@IfProfileValue(name = "spring.profiles.active", value = "ci")
public class IntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(IntegrationTest.class);

    /**
     * test encryption/decryption with large data 100 MByte.
     * <p>
     * throws InvalidCipherTextException
     */
    @Test
    public void testEncryptDecrypt3() throws InvalidCipherTextException {

        // password generation using KDF
        String password = "Aa1234567890#";
        byte[] kdf = Password.generateKeyDerivation(password.getBytes(), 32);

        byte[] plain = "0123456789".getBytes();
        byte[] plainBytes = new byte[10000000];
        for (int i = 0; i < plainBytes.length / plain.length; i++) {
            System.arraycopy(plain, 0, plainBytes, i * plain.length, plain.length);
        }

        byte[] encData = AESEncrypt.encrypt(plainBytes, kdf);
        byte[] plainData = AESEncrypt.decrypt(encData, kdf);

        assertArrayEquals(plainBytes, plainData);

    }

    @Test   /* performance test */
    public void test6() {

        long firstTime = System.currentTimeMillis();
        for (int i = 0; i < 1000; ++i) {

            byte[] horseBytes = sha3("horse".getBytes());
            byte[] addr = ECKey.fromPrivate(horseBytes).getAddress();
            assertEquals("13978AEE95F38490E9769C39B2773ED763D9CD5F",
                    Hex.toHexString(addr).toUpperCase());
        }

        long secondTime = System.currentTimeMillis();
        log.debug(Hex.toHexString(ByteUtil.longToBytes(secondTime - firstTime)) + " millisec");
        // 1) result: ~52 address calculation every second
    }

}