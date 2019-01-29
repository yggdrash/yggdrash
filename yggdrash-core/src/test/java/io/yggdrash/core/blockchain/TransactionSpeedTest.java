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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.ContractTestUtils;
import io.yggdrash.TestConstants.PerformanceTest;
import io.yggdrash.common.config.Constants;
import io.yggdrash.common.util.TimeUtils;
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
    private static final long VERYFY_TIMEOUT = 2000L;

    private TransactionBody txBody;
    private TransactionHeader txHeader;
    private Wallet wallet;
    private TransactionSignature txSig;
    private Transaction tx1;
    private byte[] txBytes1;

    @Before
    public void setUp() throws Exception {

        JsonObject params = new JsonObject();
        params.addProperty("address", "5db10750e8caff27f906b41c71b3471057dd2000");
        params.addProperty("amount", "10000000");

        JsonArray txArrayBody = ContractTestUtils.txBodyJson(Constants.STEM_CONTRACT_ID,
                "tansfer", params);

        txBody = new TransactionBody(txArrayBody);

        byte[] chain = new byte[20];
        byte[] version = new byte[8];
        byte[] type = new byte[8];
        long timestamp = TimeUtils.time();

        txHeader = new TransactionHeader(chain, version, type, timestamp, txBody);

        wallet = new Wallet();

        txSig = new TransactionSignature(wallet, txHeader.getHashForSigning());
        tx1 = new Transaction(txHeader, txSig.getSignature(), txBody);
        assertTrue(tx1.verify());

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
            new Transaction(txHeader, txSig.getSignature(), txBody);

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
            new Transaction(txHeader, wallet, txBody);

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
            new Transaction(tx1.toJsonObject());

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
            new Transaction(txBytes1);

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

    @Test(timeout = VERYFY_TIMEOUT)
    public void testSpeedTransactionVerify() {

        long startTime;
        long endTime;
        long[] timeList = new long[(int)MAX];

        for (long i = 0; i < MAX; i++) {
            startTime = System.nanoTime();

            // Test method
            tx1.verify();

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
