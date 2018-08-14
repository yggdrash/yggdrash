package io.yggdrash.core;

import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import io.yggdrash.config.DefaultConfig;
import io.yggdrash.core.mapper.TransactionMapper;
import io.yggdrash.crypto.ECKeyTest;
import io.yggdrash.proto.BlockChainProto;
import io.yggdrash.util.SerializeUtils;
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

    private Transaction tx;
    private Wallet wallet;

    @Before
    public void setUp() throws IOException, InvalidCipherTextException {
        wallet = new Wallet();
        JsonObject json = new JsonObject();
        json.addProperty("data", "TEST");
        this.tx = new Transaction(wallet, json);

        log.debug("Before Transaction: " + tx.toString());
        log.debug("Before Transaction address: " + tx.getHeader().getAddressToString() + "\n");
    }

    @Test
    public void transactionTest() {
        assert !tx.getHashString().isEmpty();
    }

    @Test
    public void deserializeTransactionFromSerializerTest() throws IOException,
            ClassNotFoundException {
        byte[] bytes = SerializeUtils.convertToBytes(tx);
        ByteString byteString = ByteString.copyFrom(bytes);
        byte[] byteStringBytes = byteString.toByteArray();
        assert bytes.length == byteStringBytes.length;
        Transaction deserializeTx = (Transaction) SerializeUtils.convertFromBytes(byteStringBytes);
        assert tx.getHashString().equals(deserializeTx.getHashString());
    }

    @Test
    public void deserializeTransactionFromProtoTest() {
        BlockChainProto.Transaction protoTx = TransactionMapper.transactionToProtoTransaction(tx);
        Transaction deserializeTx = TransactionMapper.protoTransactionToTransaction(protoTx);
        assert tx.getHashString().equals(deserializeTx.getHashString());
    }

    @Test
    public void testMakeTransaction() {
        JsonObject json = new JsonObject();
        json.addProperty("data", "TEST");
        Transaction tx = new Transaction(wallet, json);

        log.debug("Transaction 2: " + tx.toString());
        log.debug("Transaction 2 address: " + tx.getHeader().getAddressToString());

        assertEquals(this.tx.getHeader().getAddressToString(), tx.getHeader().getAddressToString());
    }

    @Test
    public void testGetAddressWithAccount() throws IOException, InvalidCipherTextException {
        Wallet wallet = new Wallet(new DefaultConfig());
        JsonObject json = new JsonObject();
        json.addProperty("data", "TEST");

        Transaction tx1 = new Transaction(wallet, json);
        Transaction tx2 = new Transaction(wallet, json);

        log.debug("Test Transaction1: " + tx1.toString());
        log.debug("Test Transaction1 Address: " + tx1.getHeader().getAddressToString());

        log.debug("Test Transaction2: " + tx2.toString());
        log.debug("Test Transaction2 Address: " + tx2.getHeader().getAddressToString());

        assertEquals(tx1.getHeader().getAddressToString(), tx2.getHeader().getAddressToString());
    }

    @Test
    public void testGetAddressWithWallet() throws IOException, InvalidCipherTextException {
        Wallet wallet = new Wallet(new DefaultConfig());
        JsonObject json = new JsonObject();
        json.addProperty("data", "TEST");

        Transaction tx1 = new Transaction(wallet, json);
        Transaction tx2 = new Transaction(wallet, json);

        log.debug("Test Transaction1: " + tx1.toString());
        log.debug("Test Transaction1 Address: " + tx1.getHeader().getAddressToString());

        log.debug("Test Transaction2: " + tx2.toString());
        log.debug("Test Transaction2 Address: " + tx2.getHeader().getAddressToString());

        log.debug("Test Transaction1: " + tx1.toString());
        log.debug("Test Transaction1 Address: " + tx1.getHeader().getAddressToString());

        log.debug("Test Transaction2: " + tx2.toString());
        log.debug("Test Transaction2 Address: " + tx2.getHeader().getAddressToString());

        assertArrayEquals(wallet.getAddress(), tx1.getHeader().getAddress());
        assertArrayEquals(tx1.getHeader().getAddress(), tx2.getHeader().getAddress());
        assertArrayEquals(wallet.getAddress(), tx2.getHeader().getAddress());
    }

    @Test
    public void testGetAddressWithWalletAccount() throws IOException, InvalidCipherTextException {
        Account account = new Account();
        log.debug("Account: " + account.toString());

        Wallet wallet = new Wallet(account.getKey(), "tmp/path", "nodePri.key", "Aa1234567890!");
        log.debug("Wallet: " + wallet.toString());

        JsonObject json = new JsonObject();
        json.addProperty("data", "TEST");
        Transaction tx1 = new Transaction(wallet, json);
        Transaction tx2 = new Transaction(wallet, json);

        log.debug("Test Transaction1: " + tx1.toString());
        log.debug("Test Transaction1 Address: " + tx1.getHeader().getAddressToString());

        log.debug("Test Transaction2: " + tx2.toString());
        log.debug("Test Transaction2 Address: " + tx2.getHeader().getAddressToString());

        log.debug("Test Transaction1: " + tx1.toString());
        log.debug("Test Transaction1 Address: " + tx1.getHeader().getAddressToString());

        log.debug("Test Transaction2: " + tx2.toString());
        log.debug("Test Transaction2 Address: " + tx2.getHeader().getAddressToString());

        assertArrayEquals(wallet.getAddress(), account.getAddress());
        assertArrayEquals(tx1.getHeader().getAddress(), tx2.getHeader().getAddress());
        assertArrayEquals(wallet.getAddress(), tx2.getHeader().getAddress());
    }

    @Test
    public void testToJsonObject() {

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("operator", "transfer");
        jsonObject.addProperty("to", "0x5186a0EF662DFA89Ed44b52a55EC5Cf0B4b59bb7");
        jsonObject.addProperty("balance", "100000000");

        log.debug(jsonObject.toString());

        Wallet wallet = null;
        try {
            wallet = new Wallet();

            Transaction tx1 = new Transaction(wallet, jsonObject);
            log.debug(tx1.toJsonObject().toString());
        } catch (Exception e) {
            log.error(e.getMessage());
            assert false;
        }
    }

}
