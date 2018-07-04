package io.yggdrash.core;

import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import io.yggdrash.proto.BlockChainProto;
import io.yggdrash.util.SerializeUtils;
import org.apache.commons.codec.binary.Hex;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.SerializationUtils;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;

@RunWith(SpringRunner.class)
public class TransactionTest {
    private static final Logger log = LoggerFactory.getLogger(TransactionTest.class);

    @Test
    public void transactionTest() throws IOException {
        Account account1 = new Account();
        JsonObject json = new JsonObject();
        json.addProperty("data", "TEST");
        Transaction t = new Transaction(account1, json);
        log.debug(t.toString());

        assert !t.getHashString().isEmpty();
    }

    @Test
    public void makeTransactionTest() throws IOException {
        // check transaction
        int makeTransaction = 10;

        HashMap<byte[], Transaction> hash = new HashMap<>();
        for (int i = 0; i < makeTransaction; i++) {
            Transaction tx = newTransaction();
            log.debug("hashcode : " + new String(Hex.encodeHex(tx.getHash(), true)));
            assert hash.get(tx.getHash()) == null;
            hash.put(tx.getHash(), tx);
        }
    }

    @Test
    public void serializeTransactionTest() throws IOException {
        Transaction tx = newTransaction();
        byte[] bytes = SerializationUtils.serialize(tx);
        ByteString byteString = ByteString.copyFrom(bytes);
        byte[] byteStringBytes = byteString.toByteArray();
        assert bytes.length == byteStringBytes.length;
        Transaction deserializedTx = (Transaction)SerializationUtils.deserialize(byteStringBytes);
        assert tx.getHashString().equals(deserializedTx.getHashString());
    }

    @Test
    public void sereializeByUtilTest() throws IOException {
        Transaction tx = newTransaction();
        Transaction deserializedTx = Transaction.valueOf(Transaction.of(tx));
        assert tx.getHashString().equals(deserializedTx.getHashString());
    }

    private Transaction newTransaction() throws IOException {
        Account account = new Account();
        JsonObject json = new JsonObject();
        return new Transaction(account, json);
    }
}
