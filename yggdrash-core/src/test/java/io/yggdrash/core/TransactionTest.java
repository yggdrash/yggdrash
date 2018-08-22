package io.yggdrash.core;

import com.google.gson.JsonObject;
import io.yggdrash.TestUtils;
import io.yggdrash.core.exception.InvalidSignatureException;
import io.yggdrash.crypto.ECKey;
import io.yggdrash.proto.Proto;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.security.SignatureException;

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

//        assertArrayEquals(wallet.getAddress(), tx1.getAddress().getBytes());
//        assertEquals(tx1.getAddress(), tx2.getAddress());
//        assertArrayEquals(wallet.getAddress(), tx2.getAddress().getBytes());
    }

    @Test
    public void testGetAddressWithWalletAccount() throws IOException, InvalidCipherTextException {
        Account account = new Account();
        log.debug("Account: " + account.toString());
        log.debug("Account.address: " + Hex.toHexString(account.getAddress()));

        Wallet wallet = new Wallet(account.getKey(), "tmp/path", "nodePri.key", "Aa1234567890!");
        log.debug("Wallet: " + wallet.toString());
        log.debug("Wallet.address: " + Hex.toHexString(wallet.getAddress()));

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

//        assertArrayEquals(wallet.getAddress(), account.getAddress());
//        assertEquals(tx1.getAddress(), tx2.getAddress());
//        assertArrayEquals(wallet.getAddress(), tx2.getAddress().getBytes());
    }

    @Test
    public void testGetAddress() throws IOException, InvalidCipherTextException, SignatureException {
        Account account = new Account();
        log.debug("Account: " + account.toString());
        log.debug("Account.address: " + Hex.toHexString(account.getAddress()));
        log.debug("Account.pubKey: " + Hex.toHexString(account.getKey().getPubKey()));

        Wallet wallet = new Wallet(account.getKey(), "tmp/path", "nodePri.key", "Aa1234567890!");
        log.debug("Wallet: " + wallet.toString());
        log.debug("Wallet.address: " + Hex.toHexString(wallet.getAddress()));
        log.debug("Wallet.pubKey: " + Hex.toHexString(wallet.getPubicKey()));

        log.debug("tx1 generate ===============>");

        TransactionHusk tx1 = TestUtils.createTxHusk(wallet);
        log.debug("Test Transaction1: " + tx1.toString());
        log.debug("Test Transaction1 Address: " + tx1.getAddress());

        log.debug("<============tx1 generate end");

        if(tx1.verify()) {
            log.debug("verify success");
        } else {
            assert false;
        }

        assertArrayEquals(wallet.getAddress(), account.getAddress());
        assertArrayEquals(wallet.getAddress(), tx1.getAddress().getBytes());

        byte[] hashedRawData = tx1.getDataHashForSigning();
        log.debug("hashedRawData: " + Hex.toHexString(hashedRawData));

        byte[] signatureBin = tx1.getInstance().getHeader().getSignature().toByteArray();
        log.debug("signatureBin: " + Hex.toHexString(signatureBin));

        ECKey.ECDSASignature ecdsaSignature = new ECKey.ECDSASignature(signatureBin);
        ECKey key = ECKey.signatureToKey(hashedRawData, ecdsaSignature);

        byte [] address = key.getAddress();
        byte [] pubKey = key.getPubKey();

        log.debug("address: " + Hex.toHexString(address));
        log.debug("pubKey: " + Hex.toHexString(pubKey));

    }

    @Test
    public void testToJsonObject() {

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("operator", "transfer");
        jsonObject.addProperty("to", "0x5186a0EF662DFA89Ed44b52a55EC5Cf0B4b59bb7");
        jsonObject.addProperty("balance", "100000000");

        log.debug(jsonObject.toString());

        TransactionHusk tx1 = new TransactionHusk(jsonObject).sign(wallet);
        log.debug(tx1.toJsonObject().toString());
    }

}
