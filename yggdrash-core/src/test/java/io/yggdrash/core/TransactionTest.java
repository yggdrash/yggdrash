package io.yggdrash.core;

import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import io.yggdrash.config.DefaultConfig;
import io.yggdrash.core.mapper.TransactionMapper;
import io.yggdrash.proto.BlockChainProto;
import org.junit.Before;
import org.junit.Test;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.springframework.util.SerializationUtils;

import java.io.IOException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class TransactionTest {

    private Transaction tx;

    @Before
    public void setUp() {
        JsonObject json = new JsonObject();
        json.addProperty("data", "TEST");
        this.tx = new Transaction(json);
        WalletMock.sign(tx);
        System.out.println("Before Transaction: " + tx.toString());
        System.out.println("Before Transaction address: " + tx.getHeader().getAddressToString());
        System.out.println("\n");
    }

    @Test
    public void transactionTest() {
        assert !tx.getHashString().isEmpty();
    }

    @Test
    public void deserializeTransactionFromSerializerTest() {
        byte[] bytes = SerializationUtils.serialize(tx);
        ByteString byteString = ByteString.copyFrom(bytes);
        byte[] byteStringBytes = byteString.toByteArray();
        assert bytes.length == byteStringBytes.length;
        Transaction deserializeTx = (Transaction) SerializationUtils.deserialize(byteStringBytes);
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
        Transaction tx = new Transaction(json);
        WalletMock.sign(tx);

        System.out.println("Transaction 2: " + tx.toString());
        System.out.println("Transaction 2 address: " + tx.getHeader().getAddressToString());

        assertEquals(this.tx.getHeader().getAddressToString(), tx.getHeader().getAddressToString());
    }

    @Test
    public void testGetAddressWithAccount() throws IOException, InvalidCipherTextException {
        Wallet wallet = new Wallet(new DefaultConfig());
        JsonObject json = new JsonObject();
        json.addProperty("data", "TEST");

        Transaction tx1 = new Transaction(json);
        WalletMock.sign(tx1);
        Transaction tx2 = new Transaction(json);
        WalletMock.sign(tx2);

        System.out.println("Test Transaction1: " + tx1.toString());
        System.out.println("Test Transaction1 Address: " + tx1.getHeader().getAddressToString());

        System.out.println("Test Transaction2: " + tx2.toString());
        System.out.println("Test Transaction2 Address: " + tx2.getHeader().getAddressToString());

        assertEquals(tx1.getHeader().getAddressToString(), tx2.getHeader().getAddressToString());
    }

    @Test
    public void testGetAddressWithWallet()
            throws IOException, InvalidCipherTextException {
        Wallet wallet = new Wallet(new DefaultConfig());
        JsonObject json = new JsonObject();
        json.addProperty("data", "TEST");

        Transaction tx1 = new Transaction(json);
        WalletMock.sign(tx1);
        Transaction tx2 = new Transaction(json);
        WalletMock.sign(tx2);


        System.out.println("Test Transaction1: " + tx1.toString());
        System.out.println("Test Transaction1 Address: " + tx1.getHeader().getAddressToString());

        System.out.println("Test Transaction2: " + tx2.toString());
        System.out.println("Test Transaction2 Address: " + tx2.getHeader().getAddressToString());

        System.out.println("Test Transaction1: " + tx1.toString());
        System.out.println("Test Transaction1 Address: " + tx1.getHeader().getAddressToString());

        System.out.println("Test Transaction2: " + tx2.toString());
        System.out.println("Test Transaction2 Address: " + tx2.getHeader().getAddressToString());

        assertArrayEquals(wallet.getAddress(), tx1.getHeader().getAddress());
        assertArrayEquals(tx1.getHeader().getAddress(), tx2.getHeader().getAddress());
        assertArrayEquals(wallet.getAddress(), tx2.getHeader().getAddress());
    }

    @Test
    public void testGetAddressWithWalletAccount()
            throws IOException, InvalidCipherTextException {
        Account account = new Account();
        System.out.println("Account: " + account.toString());

        Wallet wallet = new Wallet(account.getKey(), "tmp/path", "nodePri.key", "Aa1234567890!");
        System.out.println("Wallet: " + wallet.toString());

        JsonObject json = new JsonObject();
        json.addProperty("data", "TEST");
        Transaction tx1 = new Transaction(json);
        WalletMock.sign(tx1, wallet);
        Transaction tx2 = new Transaction(json);
        WalletMock.sign(tx2, wallet);

        System.out.println("Test Transaction1: " + tx1.toString());
        System.out.println("Test Transaction1 Address: " + tx1.getHeader().getAddressToString());

        System.out.println("Test Transaction2: " + tx2.toString());
        System.out.println("Test Transaction2 Address: " + tx2.getHeader().getAddressToString());

        System.out.println("Test Transaction1: " + tx1.toString());
        System.out.println("Test Transaction1 Address: " + tx1.getHeader().getAddressToString());

        System.out.println("Test Transaction2: " + tx2.toString());
        System.out.println("Test Transaction2 Address: " + tx2.getHeader().getAddressToString());

        assertArrayEquals(wallet.getAddress(), account.getAddress());
        assertArrayEquals(tx1.getHeader().getAddress(), tx2.getHeader().getAddress());
        assertArrayEquals(wallet.getAddress(), tx2.getHeader().getAddress());
    }
}
