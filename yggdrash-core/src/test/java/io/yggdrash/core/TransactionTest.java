package io.yggdrash.core;

import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import io.yggdrash.core.mapper.TransactionMapper;
import io.yggdrash.proto.BlockChainProto;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.SerializationUtils;

import java.io.IOException;

@RunWith(SpringRunner.class)
public class TransactionTest {

    private Transaction tx;

    @Before
    public void setUp() throws Exception {
        Account account = new Account();
        JsonObject json = new JsonObject();
        json.addProperty("data", "TEST");
        this.tx = new Transaction(account, json);
    }

    @Test
    public void transactionTest() throws IOException {
        assert !tx.getHashString().isEmpty();
    }

    @Test
    public void deserializeTransactionFromSerializerTest() throws IOException {
        byte[] bytes = SerializationUtils.serialize(tx);
        ByteString byteString = ByteString.copyFrom(bytes);
        byte[] byteStringBytes = byteString.toByteArray();
        assert bytes.length == byteStringBytes.length;
        Transaction deserializeTx = (Transaction) SerializationUtils.deserialize(byteStringBytes);
        assert tx.getHashString().equals(deserializeTx.getHashString());
    }

    @Test
    public void deserializeTransactionFromProtoTest() throws IOException {
        BlockChainProto.Transaction protoTx = TransactionMapper.transactionToProtoTransaction(tx);
        Transaction deserializeTx = TransactionMapper.protoTransactionToTransaction(protoTx);
        assert tx.getHashString().equals(deserializeTx.getHashString());
    }
}
