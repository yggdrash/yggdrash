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

package io.yggdrash.core.blockchain;

import com.google.gson.JsonObject;
import io.yggdrash.ContractTestUtils;
import io.yggdrash.TestConstants;
import io.yggdrash.TestConstants.PerformanceTest;
import io.yggdrash.common.config.Constants;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.common.util.TimeUtils;
import io.yggdrash.common.util.VerifierUtils;
import io.yggdrash.core.wallet.Wallet;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import static junit.framework.TestCase.assertTrue;

public class TransactionSpeedTest extends PerformanceTest {

    private static final Logger log = LoggerFactory.getLogger(TransactionSpeedTest.class);

    private static final long MAX = 100L;
    private static final long CON_TIMEOUT = 1000L;
    private static final long VERIFY_TIMEOUT = 2000L;

    private TransactionBody txBody;
    private TransactionHeader txHeader;
    private Wallet wallet;
    private Transaction tx1;
    private byte[] txBytes1;

    @Before
    public void setUp() throws Exception {

        JsonObject params = new JsonObject();
        params.addProperty("address", "5db10750e8caff27f906b41c71b3471057dd2000");
        params.addProperty("amount", "10000000");
        TestConstants.yggdrash();
        JsonObject txObjBody = ContractTestUtils.txBodyJson(TestConstants.STEM_CONTRACT,
                "transfer", params, false);

        txBody = new TransactionBody(txObjBody);

        byte[] chain = Constants.EMPTY_BRANCH;
        byte[] version = Constants.EMPTY_BYTE8;
        byte[] type = Constants.EMPTY_BYTE8;
        long timestamp = TimeUtils.time();

        txHeader = new TransactionHeader(chain, version, type, timestamp, txBody);

        wallet = new Wallet(new DefaultConfig(), "Aa1234567890!");

        tx1 = new TransactionImpl(txHeader, wallet, txBody);
        assertTrue(VerifierUtils.verify(tx1));

        txBytes1 = tx1.toBinary();
    }

    @Test (timeout = CON_TIMEOUT)
    public void testSpeedTransactionConstructor_1() {

        long startTime;
        long endTime;
        long[] timeList = new long[(int)MAX];

        for (long i = 0; i < MAX; i++) {
            startTime = System.nanoTime();

            // Test method
            new TransactionImpl(txHeader, wallet, txBody);

            endTime = System.nanoTime();
            Arrays.fill(timeList,endTime - startTime);
        }

        long totalTime = 0;
        for (long value : timeList) {
            totalTime += value;
        }

        long averageTime = totalTime / MAX;
        log.info(" Transaction:Constructor(Header,sig,body) nanoTime:" + averageTime);
    }

    @Test(timeout = CON_TIMEOUT)
    public void testSpeedTransactionConstructor_2() {

        long startTime;
        long endTime;
        long[] timeList = new long[(int)MAX];

        for (long i = 0; i < MAX; i++) {
            startTime = System.nanoTime();

            // Test method
            new TransactionImpl(txHeader, wallet, txBody);

            endTime = System.nanoTime();
            Arrays.fill(timeList,endTime - startTime);
        }

        long totalTime = 0;
        for (long value : timeList) {
            totalTime += value;
        }

        long averageTime = totalTime / MAX;
        log.info(" Transaction:Constructor(Header,wallet,body) nanoTime:" + averageTime);
    }

    @Test (timeout = CON_TIMEOUT)
    public void testSpeedTransactionConstructor_3() {

        long startTime;
        long endTime;
        long[] timeList = new long[(int)MAX];

        for (long i = 0; i < MAX; i++) {
            startTime = System.nanoTime();

            // Test method
            new TransactionImpl(tx1.toJsonObject());

            endTime = System.nanoTime();
            Arrays.fill(timeList,endTime - startTime);
        }

        long totalTime = 0;
        for (long value : timeList) {
            totalTime += value;
        }

        long averageTime = totalTime / MAX;
        log.info(" Transaction:Constructor(jsonObject) nanoTime:" + averageTime);
    }

    @Test (timeout = CON_TIMEOUT)
    public void testSpeedTransactionConstructor_4() {

        long startTime;
        long endTime;
        long[] timeList = new long[(int)MAX];

        for (long i = 0; i < MAX; i++) {
            startTime = System.nanoTime();

            // Test method
            new TransactionImpl(txBytes1);

            endTime = System.nanoTime();
            Arrays.fill(timeList,endTime - startTime);
        }

        long totalTime = 0;
        for (long value : timeList) {
            totalTime += value;
        }

        long averageTime = totalTime / MAX;
        log.info(" Transaction:Constructor(byte[]) nanoTime:" + averageTime);
    }

    @Test(timeout = VERIFY_TIMEOUT)
    public void testSpeedTransactionVerify() {

        long startTime;
        long endTime;
        long[] timeList = new long[(int)MAX];

        for (long i = 0; i < MAX; i++) {
            startTime = System.nanoTime();

            // Test method
            VerifierUtils.verify(tx1);

            endTime = System.nanoTime();
            Arrays.fill(timeList,endTime - startTime);
        }

        long totalTime = 0;
        for (long value : timeList) {
            totalTime += value;
        }

        long averageTime = totalTime / MAX;
        log.info(" Transaction:verify() nanoTime:" + averageTime);
    }

}
