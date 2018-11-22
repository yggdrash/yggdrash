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

import io.yggdrash.TestUtils;
import io.yggdrash.common.crypto.AESEncrypt;
import io.yggdrash.common.crypto.ECKey;
import io.yggdrash.common.crypto.Password;
import io.yggdrash.common.util.ByteUtil;
import io.yggdrash.core.BlockChain;
import io.yggdrash.core.BlockHusk;
import io.yggdrash.core.BranchEventListener;
import io.yggdrash.core.BranchGroup;
import io.yggdrash.core.TransactionHusk;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.util.encoders.Hex;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.StopWatch;

import java.util.concurrent.TimeUnit;

import static io.yggdrash.common.crypto.HashUtil.sha3;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@IfProfileValue(name = "spring.profiles.active", value = "ci")
public class PerformanceTest {

    private static final Logger log = LoggerFactory.getLogger(PerformanceTest.class);

    /**
     * test generate block with large tx.
     */
    @Test
    public void generateBlockTest() {
        BranchGroup branchGroup = new BranchGroup();
        BlockChain blockChain = TestUtils.createBlockChain(false);
        branchGroup.addBranch(blockChain, new BranchEventListener() {
            @Override
            public void chainedBlock(BlockHusk block) {
            }

            @Override
            public void receivedTransaction(TransactionHusk tx) {
            }
        });

        StopWatch watch = new StopWatch("generateBlockTest");

        watch.start("txStart");
        for (int i = 0; i < 100; i++) {
            blockChain.addTransaction(new TransactionHusk(TestUtils.sampleTransferTx(i)));
        }
        watch.stop();

        log.debug(watch.shortSummary());

        watch.start("addBlock");
        branchGroup.generateBlock(TestUtils.wallet(), blockChain.getBranchId());
        watch.stop();

        log.debug(watch.shortSummary());

        Assertions.assertThat(watch.getTotalTimeMillis())
                .isLessThan(TimeUnit.SECONDS.toMillis(3));
    }

    /**
     * test encryption/decryption with large data 100 MByte.
     * <p>
     * throws InvalidCipherTextException
     */
    @Test
    public void encryptDecryptTest() throws InvalidCipherTextException {

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
    public void getEcKeyFromPrivateTest() {

        long firstTime = System.currentTimeMillis();
        byte[] horseBytes = sha3("horse".getBytes());
        for (int i = 0; i < 1000; ++i) {

            ECKey ecKey = ECKey.fromPrivate(horseBytes);
            byte[] addr = ecKey.getAddress();
            assertEquals("13978AEE95F38490E9769C39B2773ED763D9CD5F",
                    Hex.toHexString(addr).toUpperCase());
        }

        long secondTime = System.currentTimeMillis();
        log.debug(Hex.toHexString(ByteUtil.longToBytes(secondTime - firstTime)) + " millisec");
        // 1) result: ~52 address calculation every second
    }

}