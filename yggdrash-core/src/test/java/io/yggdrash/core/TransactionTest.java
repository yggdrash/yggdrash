package io.yggdrash.core;

import com.google.gson.JsonObject;
import io.yggdrash.TestUtils;
import io.yggdrash.core.exception.InvalidSignatureException;
import io.yggdrash.proto.Proto;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.InvalidCipherTextException;

import java.io.IOException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class TransactionTest {

    private static final Logger log = LoggerFactory.getLogger(TransactionTest.class);
    private static Wallet wallet;

    private TransactionHusk tx;

    static {
        try {
            wallet = new Wallet();
        } catch (Exception e) {
            throw new InvalidSignatureException(e);
        }
    }

    @Before
    public void setUp() {
        this.tx = TestUtils.createTxHusk();

        log.debug("Before Transaction: " + tx.toString());
        log.debug("Before Transaction address: " + tx.getAddress().toString() + "\n");
    }

    @Test
    public void transactionTest() {
        assert tx.getHash() != null;
    }

    @Test
    public void deserializeTransactionFromProtoTest() {
        Proto.Transaction protoTx = tx.getInstance();
        TransactionHusk deserializeTx = new TransactionHusk(protoTx);
        assert tx.getHash().equals(deserializeTx.getHash());
    }

    @Test
    public void testMakeTransaction() {
        TransactionHusk tx2 = TestUtils.createTxHusk();

        log.debug("Transaction 2: " + tx2.toString());
        log.debug("Transaction 2 address: " + tx2.getAddress().toString());

        assertEquals(tx.getAddress().toString(), tx2.getAddress().toString());
    }

    @Test
    public void testGetAddressWithWallet() {
        TransactionHusk tx1 = TestUtils.createTxHusk();
        TransactionHusk tx2 = TestUtils.createTxHusk();

        log.debug("Test Transaction1: " + tx1.toString());
        log.debug("Test Transaction1 Address: " + tx1.getAddress());

        log.debug("Test Transaction2: " + tx2.toString());
        log.debug("Test Transaction2 Address: " + tx2.getAddress());

        log.debug("Test Transaction1: " + tx1.toString());
        log.debug("Test Transaction1 Address: " + tx1.getAddress());

        log.debug("Test Transaction2: " + tx2.toString());
        log.debug("Test Transaction2 Address: " + tx2.getAddress());

        assertArrayEquals(wallet.getAddress(), tx1.getAddress().getBytes());
        assertEquals(tx1.getAddress(), tx2.getAddress());
        assertArrayEquals(wallet.getAddress(), tx2.getAddress().getBytes());
    }

    @Test
    public void testGetAddressWithWalletAccount() throws IOException, InvalidCipherTextException {
        Account account = new Account();
        log.debug("Account: " + account.toString());

        Wallet wallet = new Wallet(account.getKey(), "tmp/path", "nodePri.key", "Aa1234567890!");
        log.debug("Wallet: " + wallet.toString());

        TransactionHusk tx1 = TestUtils.createTxHusk(wallet);
        TransactionHusk tx2 = TestUtils.createTxHusk(wallet);

        log.debug("Test Transaction1: " + tx1.toString());
        log.debug("Test Transaction1 Address: " + tx1.getAddress());

        log.debug("Test Transaction2: " + tx2.toString());
        log.debug("Test Transaction2 Address: " + tx2.getAddress());

        log.debug("Test Transaction1: " + tx1.toString());
        log.debug("Test Transaction1 Address: " + tx1.getAddress());

        log.debug("Test Transaction2: " + tx2.toString());
        log.debug("Test Transaction2 Address: " + tx2.getAddress());

        assertArrayEquals(wallet.getAddress(), account.getAddress());
        assertEquals(tx1.getAddress(), tx2.getAddress());
        assertArrayEquals(wallet.getAddress(), tx2.getAddress().getBytes());
    }

    @Test
    public void testToJsonObject() {

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("operator", "transfer");
        jsonObject.addProperty("to", "056a8143fdc7416a9b8d59cb4196930588731e9b");
        jsonObject.addProperty("balance", "100000000");

        log.debug(jsonObject.toString());

        TransactionHusk tx1 = new TransactionHusk(jsonObject).sign(wallet);
        log.debug(tx1.toJsonObject().toString());
    }

}
