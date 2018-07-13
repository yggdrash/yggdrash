package io.yggdrash.node.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.primitives.Longs;
import com.google.gson.JsonObject;
import io.yggdrash.core.Account;
import io.yggdrash.core.Transaction;
import io.yggdrash.node.mock.TransactionMock;
import io.yggdrash.node.mock.TransactionReceiptMock;
import org.json.simple.parser.ParseException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Base64;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.nio.ByteBuffer;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@Import(ApplicationConfig.class)
public class TransactionApiTest {
    private static final Logger log = LoggerFactory.getLogger(TransactionApi.class);

    @Test
    public void checkTransactionJsonFormat() throws IOException {
        Account from = new Account();
        JsonObject data = new JsonObject();
        Transaction tx = new Transaction(from, data);
        ObjectMapper objectMapper = new ObjectMapper();
        log.debug("\n\nTransaction Format : " + objectMapper.writeValueAsString(tx));
    }

    @Test
    public void jsonStringToTxTest() throws ParseException,IOException {
        // Get Transaction of JsonString as Param
        String jsonStr = "{\"header\":{\"type\":\"0000\",\"version\":\"0000\",\"dataHash\":\"de7e5e6375a46028a23357fa4a51404bc88cec132642ded5372554dc87b5091c\",\"timestamp\":\"155810745733540\",\"dataSize\":\"10\",\"signature\":\"1c05560a9fdc9c25edfefe5a348db182ed9b283e734b39afb827785a7f7922232d75304b93d5c4774ab2d889df06789adb0fd1fa2409bc42eeb4d5e724022377e8\"},\"data\":{\"id\":\"0\",\"name\":\"Rachael\",\"age\":\"27\"}}";

        // Create Transaction by transactionDto
        TransactionDto transactionDto = new TransactionDto();
        Transaction transaction = transactionDto.jsonStringToTx(jsonStr);

        // Create Transaction JsonObject
        ObjectMapper objectMapper = new ObjectMapper();
        log.debug("\n\nTransaction : " + objectMapper.writeValueAsString(transaction) + "\n");
    }

    @Test
    public void jsonByteArrToTxTest() throws ParseException,JsonProcessingException {
        // Get Transaction of JsonString which contains byteArray as param.
        String jsonByteArr = " {\"header\":{\"type\":\"AAAAAA==\",\"version\":\"AAAAAA==\",\"dataHash\":\"RBNvo1WzZ4oRRq0W9+hknpT7T8If536DEMBg9hyq/4o=\",\"timestamp\":76623948013441,\"dataSize\":2,\"signature\":\"GyKLQPLLuzKBFmzQHtyc6nIUJmi/kV99/Al+XYcLiKw5GM/5wnMAb43x9joVdGyRhS1lfxzZqody5LKEcaBau9w=\"},\"data\":{\"id\":\"0\",\"name\":\"Rachael\",\"age\":\"27\"}}";

        // Create Transaction by transactionDto
        TransactionDto transactionDto = new TransactionDto();
        Transaction transaction = transactionDto.jsonByteArrToTx(jsonByteArr);

        // Create Transaction JsonObject
        ObjectMapper objectMapper = new ObjectMapper();
        log.debug("\n\nTransaction : " + objectMapper.writeValueAsString(transaction) + "\n");
    }

    @Test
    public void byteArrToTxTest() throws IOException {
        // Create an input parameter
        byte[] type = new byte[4];
        byte[] version = new byte[4];
        byte[] dataHash = new byte[32];
        byte[] timestamp;
        byte[] dataSize;
        byte[] signature = new byte[65];
        byte[] data;

        type = "0000".getBytes();
        version= "0000".getBytes();
        dataHash = Base64.decode("3n5eY3WkYCiiM1f6SlFAS8iM7BMmQt7VNyVU3Ie1CRw=");
        timestamp = Longs.toByteArray(Long.parseLong("155810745733540"));
        dataSize = Longs.toByteArray((long) 2);
        signature = Base64.decode("HAVWCp/cnCXt/v5aNI2xgu2bKD5zSzmvuCd4Wn95IiMtdTBLk9XEd0qy2InfBnia2w/R+iQJvELutNXnJAIjd+g=");
        data = "{\"id\":\"0\",\"name\":\"Rachael\",\"age\":\"27\"}".getBytes();

        int totalLength = type.length + version.length + dataHash.length + timestamp.length
                        + dataSize.length + signature.length + data.length;

        ByteBuffer bb = ByteBuffer.allocate(totalLength);
        bb.put(type);
        bb.put(version);
        bb.put(dataHash);
        bb.put(timestamp);
        bb.put(dataSize);
        bb.put(signature);
        bb.put(data);

        byte[] input = bb.array();

        // Create Transaction by transactionDto
        TransactionDto transactionDto = new TransactionDto();
        Transaction tx = transactionDto.byteArrToTx(input);

        // Create Transaction JsonObject
        ObjectMapper objectMapper = new ObjectMapper();
        log.debug("\n\nTransaction : " + objectMapper.writeValueAsString(tx) + "\n");
    }

    @Test
    public void createTransactionReceiptMock() throws IOException {
        TransactionReceiptMock txReceiptMock = new TransactionReceiptMock();
        log.debug("txReceiptMock : " + txReceiptMock.retTxReceiptMock());
    }

    @Test
    public void createTransactionMock() throws IOException {
        TransactionMock txMock = new TransactionMock();
        log.debug("txMock : " + txMock.retTxMock());
    }

    @Test
    public void transactionAPIImplTest() throws Exception {
        TransactionApiImpl txapi = new TransactionApiImpl();

        String address = "0x407d73d8a49eeb85d32cf465507dd71d507100c1";
        String tag = "latest";
        String hashOfBlock = "0x76a9fa4681a8abf94618543872444ba079d5302203ac6a5b5b2087a9f56ea8bf";
        String hashOfTx = "0x76a9fa4681a8abf94618543872444ba079d5302203ac6a5b5b2087a9f56ea8bf";
        int blockNumber = 1;
        int txIndexPosition = 1;

        assertThat(1).isEqualTo(txapi.getTransactionCount(address, tag));
        assertThat(2).isEqualTo(txapi.getTransactionCount(address, blockNumber));
        assertThat(3).isEqualTo(txapi.getBlockTransactionCountByHash(hashOfBlock));
        assertThat(4).isEqualTo(txapi.getBlockTransactionCountByNumber(blockNumber));
        assertThat(5).isEqualTo(txapi.getBlockTransactionCountByNumber(tag));
        assertThat(6).isEqualTo(txapi.newPendingTransactionFilter());


        JsonObject test = new JsonObject();

        test.addProperty("version", "0");
        test.addProperty("type", "00000000000000");
        test.addProperty("timestamp", "155810745733540");
        test.addProperty("from", "hello");
        test.addProperty("dataHash", "ba5f3ea40e95f49bce11942f375ebd3882eb837976eda5c0cb78b9b99ca7b485");
        test.addProperty("dataSize", "13");
        test.addProperty("signature", "b86e02880e12c575e56c5d15e1f491595219295076721a5bfb6042463d6a2d768331691db0b8de852390305c0f2b218e596e4a59bf54029cf6a8b9afdbb274104");
        test.addProperty("transactionHash", "c6b5e583ec18891e9de0e29c3f0358a5c99c474bc3ee78e90c618db72193c0");
    }

}



