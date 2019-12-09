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

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.yggdrash.ContractTestUtils;
import io.yggdrash.TestConstants;
import io.yggdrash.TestConstants.SlowTest;
import io.yggdrash.common.RawTransaction;
import io.yggdrash.common.config.Constants;
import io.yggdrash.common.crypto.ECKey;
import io.yggdrash.common.util.TimeUtils;
import io.yggdrash.common.util.VerifierUtils;
import io.yggdrash.common.utils.ByteUtil;
import io.yggdrash.core.wallet.Account;
import io.yggdrash.core.wallet.Wallet;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SignatureException;

import static io.yggdrash.TestConstants.TRANSFER_TO;
import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class TransactionTest extends SlowTest {

    private static final Logger log = LoggerFactory.getLogger(TransactionTest.class);

    private TransactionBody txBody;
    private TransactionHeader txHeader;
    private Wallet wallet = TestConstants.wallet();
    private Transaction tx1;

    @Before
    public void setUp() throws Exception {
        TestConstants.yggdrash();

        JsonObject jsonParam = new JsonObject();
        jsonParam.addProperty("address", "5db10750e8caff27f906b41c71b3471057dd2000");
        jsonParam.addProperty("amount", "10000000");

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("method", "transfer");
        jsonObject.addProperty("contractVersion", TestConstants.YEED_CONTRACT.toString());
        jsonObject.add("params", jsonParam);

        txBody = new TransactionBody(jsonObject);
        txHeader = new TransactionHeader(Constants.EMPTY_BRANCH, Constants.EMPTY_BYTE8, Constants.EMPTY_BYTE8,
                TimeUtils.time(), txBody);

        log.debug("wallet.pubKey={}", Hex.toHexString(wallet.getPubicKey()));

        wallet = new Wallet("tmp/nodePri.key", "Aa1234567890!");
        log.debug("wallet.pubKey=" + Hex.toHexString(wallet.getPubicKey()));

        tx1 = new TransactionImpl(txHeader, wallet, txBody);

        int code = VerifierUtils.verifyDataFormatCode(tx1);
        assertTrue("Transaction Verify Test", 0 == code);
        log.debug("VERIFY CODE {} ", code);

    }

    @Test
    public void testTransactionConstructor() {
        Transaction tx2 = new TransactionImpl(tx1.toJsonObject());
        assertTrue(VerifierUtils.verify(tx2));
        log.debug("tx2={}", tx2);
        assertEquals(tx1.toJsonObject(), tx2.toJsonObject());

        Transaction tx3 = new TransactionImpl(tx1.toBinary());
        assertTrue(VerifierUtils.verify(tx3));

        log.debug("tx3={}", tx3);
        assertArrayEquals(tx1.toBinary(), tx3.toBinary());
        assertEquals(tx1.toJsonObject(), tx3.toJsonObject());

        JsonObject jsonObject = tx1.toJsonObject();
        jsonObject.getAsJsonObject("header").addProperty("timestamp",
                Hex.toHexString(ByteUtil.longToBytes(TimeUtils.time() + 1)));

        Transaction tx4 = new TransactionImpl(jsonObject);
        assertTrue(VerifierUtils.verify(tx4));

        log.debug("tx1={}", tx1);
        log.debug("tx4={}", tx4);
        assertNotEquals(tx1.toJsonObject().toString(), tx4.toJsonObject().toString());

        Transaction tx5 = new TransactionImpl(tx1.getHeader(), tx1.getSignature(), tx1.getTransactionBody());
        assertTrue(VerifierUtils.verify(tx5));

        log.debug("tx1={}", tx1);
        log.debug("tx5={}", tx5);
        assertEquals(tx1.toJsonObject(), tx5.toJsonObject());

        Transaction tx6 = new TransactionImpl(tx1.getHeader(), wallet, tx1.getTransactionBody());
        assertTrue(VerifierUtils.verify(tx6));

        log.debug("tx1={}", tx1);
        log.debug("tx6={}", tx6);
        assertEquals(tx1.toJsonObject(), tx6.toJsonObject());

        Transaction tx7 = new TransactionImpl(tx1.getInstance());
        assertTrue(VerifierUtils.verify(tx7));

        log.debug("tx1={}", tx1);
        log.debug("tx7={}", tx7);
        assertEquals(tx1, tx7);

        log.debug("tx7(pretty)={}", new GsonBuilder().setPrettyPrinting().create().toJson(tx7.toJsonObject()));
    }

    @Test
    public void testTransactionField() {
        Transaction tx2 = new TransactionImpl(tx1.toBinary());
        log.debug("tx2=" + tx2.toJsonObject());

        assertEquals(txHeader.toJsonObject().toString(),
                tx2.getHeader().toJsonObject().toString());
        assertArrayEquals(tx2.getSignature(), tx1.getSignature());
    }

    @Test
    public void testTransactionGetHash() {
        Transaction tx2 = new TransactionImpl(tx1.toBinary());
        log.debug("tx2=" + tx2.toJsonObject());

        assertEquals(tx1.getHash(), tx2.getHash());

        JsonObject jsonObject = tx1.toJsonObject();
        jsonObject.getAsJsonObject("header").addProperty("timestamp",
                Hex.toHexString(ByteUtil.longToBytes(TimeUtils.time() + 1)));

        Transaction tx3 = new TransactionImpl(jsonObject);
        log.debug("tx1 hash={}", tx1.getHash());
        log.debug("tx3 hash={}", tx3.getHash());
        assertNotEquals(tx1.getHash(), tx3.getHash());
    }

    @Test
    public void testTransactionKey() {
        Transaction tx2 = new TransactionImpl(tx1.toBinary());
        log.debug("tx2 headerHash={}", Hex.toHexString(tx2.getHeader().getHashForSigning()));
        log.debug("tx2 pubKey={}", Hex.toHexString(tx2.getPubKey()));

        assertArrayEquals(tx1.getPubKey(), tx2.getPubKey());
        assertArrayEquals(tx1.getPubKey(), wallet.getPubicKey());

        log.debug("tx1 address={}", tx1.getAddress());
        log.debug("tx2 address={}", tx2.getAddress());
        log.debug("wallet address={}", wallet.getHexAddress());
        log.debug("wallet signature={}", Hex.toHexString(wallet.sign(tx1.getHeader().getHashForSigning(), true)));
        log.debug("wallet pubKey={}", Hex.toHexString(wallet.getPubicKey()));

        assertEquals(tx1.getAddress(), tx2.getAddress());
        assertArrayEquals(tx1.getAddress().getBytes(), wallet.getAddress());
    }

    @Test
    public void testTransactionToProto() {
        Transaction tx2 = new TransactionImpl(tx1.toBinary());

        assertArrayEquals(tx1.getPubKey(), tx2.getPubKey());
        assertArrayEquals(tx1.getPubKey(), wallet.getPubicKey());

        log.debug("tx1 address={}", tx1.getAddress());
        log.debug("tx2 address={}", tx2.getAddress());
        log.debug("wallet address={}", wallet.getHexAddress());
        assertEquals(tx1.getAddress(), tx2.getAddress());
        assertArrayEquals(tx1.getAddress().getBytes(), wallet.getAddress());

        log.debug("tx1 proto={}", Hex.toHexString(tx1.toBinary()));
        log.debug("tx2 proto={}", Hex.toHexString(tx2.toBinary()));

        assertEquals(tx1, tx2);

        Transaction tx3 = new TransactionImpl(tx1.getInstance());
        log.debug("tx1={}", tx1);
        log.debug("tx3={}", tx3);

        assertEquals(tx1.toString(), tx3.toString());
        assertEquals(tx1, tx3);
    }

    @Test
    public void shouldGetJsonObjectFromProto() {
        JsonObject jsonObj = tx1.toJsonObjectFromProto();
        assertThat(jsonObj).isNotNull();
        assertThat(jsonObj.toString()).contains(tx1.getHash().toString());
    }

    @Test
    public void testRawTransaction() {
        TransactionImpl tx = new TransactionImpl(tx1.getInstance());
        TransactionHeader header = tx.getHeader();
        RawTransaction rawTx = new RawTransaction(tx.toRawTransaction());

        assertArrayEquals(header.getChain(), rawTx.getChain());
        assertArrayEquals(header.getVersion(), rawTx.getVersion());
        assertArrayEquals(header.getType(), rawTx.getType());
        assertEquals(header.getTimestamp(), rawTx.getTimestamp());
        assertArrayEquals(header.getBodyHash(), rawTx.getBodyHash());
        assertEquals(header.getBodyLength(), rawTx.getBodyLength());
        assertArrayEquals(tx.getSignature(), rawTx.getSignature());
        assertEquals(tx.getTransactionBody().toString(), rawTx.getBody());
    }

    @Test
    public void testGetAddressWithWalletAccount() throws IOException, InvalidCipherTextException {
        Account account = new Account();
        log.debug("Account={}", account);
        log.debug("Account.address={}", Hex.toHexString(account.getAddress()));

        Wallet wallet = new Wallet(account.getKey(), "tmp/path", "nodePri.key", "Aa1234567890!");
        log.debug("Wallet={}", wallet);
        log.debug("Wallet.address={}", Hex.toHexString(wallet.getAddress()));

        Transaction tx1 = createTx(wallet);
        Transaction tx2 = createTx(wallet);

        log.debug("Test Transaction1={}", tx1);
        log.debug("Test Transaction1 Address={}", tx1.getAddress());

        log.debug("Test Transaction2={}", tx2);
        log.debug("Test Transaction2 Address={}", tx2.getAddress());


        assertThat(wallet.getAddress()).isEqualTo(account.getAddress());
        assertThat(tx1.getAddress()).isEqualTo(tx2.getAddress());
        assertThat(account.getAddress()).isEqualTo(tx1.getAddress().getBytes());
    }

    @Test
    public void testGetAddressWithSig() throws IOException, InvalidCipherTextException, SignatureException {
        Account account = new Account();
        log.debug("Account: " + account.toString());
        log.debug("Account.address: " + Hex.toHexString(account.getAddress()));
        log.debug("Account.pubKey: " + Hex.toHexString(account.getKey().getPubKey()));

        Wallet wallet = new Wallet(account.getKey(), "tmp/path", "nodePri.key", "Aa1234567890!");
        log.debug("Wallet: " + wallet.toString());
        log.debug("Wallet.address: " + Hex.toHexString(wallet.getAddress()));
        log.debug("Wallet.pubKey: " + Hex.toHexString(wallet.getPubicKey()));

        Transaction tx1 = createTx(wallet);
        log.debug("Test Transaction1={}", tx1);
        log.debug("Test Transaction1 Address={}", tx1.getAddress());

        assertThat(VerifierUtils.verify(tx1)).isTrue();
        assertThat(wallet.getAddress()).isEqualTo(account.getAddress());
        assertThat(wallet.getAddress()).isEqualTo(tx1.getAddress().getBytes());

        byte[] hashedRawData = tx1.getHeader().getHashForSigning();
        log.debug("hashedRawData={}", Hex.toHexString(hashedRawData));

        byte[] signatureBin = tx1.getSignature();
        log.debug("signatureBin={}", Hex.toHexString(signatureBin));

        ECKey.ECDSASignature ecdsaSignature = new ECKey.ECDSASignature(signatureBin);
        ECKey key = ECKey.signatureToKey(hashedRawData, ecdsaSignature);

        byte[] address = key.getAddress();
        byte[] pubKey = key.getPubKey();

        log.debug("address={}", Hex.toHexString(address));
        log.debug("pubKey={}", Hex.toHexString(pubKey));

        assertThat(account.getAddress()).isEqualTo(address);
        assertThat(account.getKey().getPubKey()).isEqualTo(pubKey);
    }

    private Transaction createTx(Wallet wallet) {
        JsonObject txBody = ContractTestUtils.transferTxBodyJson(TRANSFER_TO, BigInteger.valueOf(100));
        TransactionBuilder builder = new TransactionBuilder();
        return builder.setWallet(wallet)
                .setBranchId(TestConstants.yggdrash())
                .setTxBody(txBody)
                .build();
    }
}
