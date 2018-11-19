package io.yggdrash.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.common.util.TimeUtils;
import io.yggdrash.core.account.Wallet;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import static junit.framework.TestCase.assertTrue;

public class TransactionSpeedTest {

    private static final Logger log = LoggerFactory.getLogger(TransactionSpeedTest.class);

    private static final long MAX = 1000L;

    private TransactionBody txBody;
    private TransactionHeader txHeader;
    private Wallet wallet;
    private TransactionSignature txSig;
    private Transaction tx1;
    private byte[] txBytes1;

    @Before
    public void setUp() throws Exception {

        JsonObject jsonParams1 = new JsonObject();
        jsonParams1.addProperty("address", "5db10750e8caff27f906b41c71b3471057dd2000");
        jsonParams1.addProperty("amount", "10000000");

        JsonObject jsonObject1 = new JsonObject();
        jsonObject1.addProperty("method", "transfer");
        jsonObject1.add("params", jsonParams1);

        JsonArray jsonArray = new JsonArray();
        jsonArray.add(jsonObject1);

        txBody = new TransactionBody(jsonArray);

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

    @Test (timeout = 1000L)
    public void testSpeedTransactionConstructor_1() {

        Transaction tx = null;
        long startTime;
        long endTime;
        long[] timeList = new long[(int)MAX];

        for (long i = 0; i < MAX; i++) {
            startTime = System.nanoTime();

            // Test method
            tx = new Transaction(txHeader, txSig.getSignature(), txBody);

            endTime = System.nanoTime();
            Arrays.fill(timeList,endTime - startTime);
        }

        long totalTime = 0;
        for (long value : timeList) {
            totalTime += value;
        }

        long averageTime = totalTime / MAX;
        log.info(" Transaction:Contructor(Header,sig,body) nanoTime:" + averageTime);
    }

    @Test (timeout = 5000L)
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
        log.info(" Transaction:Contructor(Header,wallet,body) nanoTime:" + averageTime);
    }

    @Test (timeout = 1000L)
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
        log.info(" Transaction:Contructor(jsonObject) nanoTime:" + averageTime);
    }

    @Test (timeout = 1000L)
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
        log.info(" Transaction:Contructor(byte[]) nanoTime:" + averageTime);
    }

    @Test (timeout = 50000L)
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
