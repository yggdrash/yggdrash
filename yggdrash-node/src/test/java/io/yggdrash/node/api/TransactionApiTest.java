package io.yggdrash.node.api;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.yggdrash.core.Account;
import io.yggdrash.core.Block;
import io.yggdrash.core.Transaction;
import io.yggdrash.core.TransactionHeader;
import io.yggdrash.node.mock.TransactionHeaderMock;
import io.yggdrash.node.mock.TransactionMock;
import io.yggdrash.node.mock.BlockMock;
import io.yggdrash.node.mock.BlockBuilderMock;
import io.yggdrash.node.mock.TransactionPoolMock;
import io.yggdrash.node.mock.TransactionReceiptMock;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@Import(ApplicationConfig.class)
public class TransactionApiTest {
    private static final Logger log = LoggerFactory.getLogger(TransactionApi.class);

    @Test
    public void transactionJsonFormat() throws IOException {
        Account from = new Account();
        JsonObject data = new JsonObject();
        Transaction tx = new Transaction(from, data);
        ObjectMapper objectMapper = new ObjectMapper();
        log.debug("Transaction Format : " + objectMapper.writeValueAsString(tx));
    }

    @Test
    public void createTxFromJsonString() throws ParseException,IOException {
        // 1. Get Transaction Json String as Param
        String jsonStr = "{\"header\":{\"type\":\"0000\",\"version\":\"0000\",\"dataHash\":\"ba5f3ea40e95f49bce11942f375ebd3882eb837976eda5c0cb78b9b99ca7b485\",\"timestamp\":\"155810745733540\",\"dataSize\":\"10\",\"signature\":\"b86e02880e12c575e56c5d15e1f491595219295076721a5bfb6042463d6a2d768331691db0b8de852390305c0f2b218e596e4a59bf54029cf6a8b9afdbb274104\"},\"data\":{\"id\":\"0\",\"name\":\"Rachael\",\"age\":\"27\"}}";

        JSONParser parser = new JSONParser();
        JSONObject tx = (JSONObject) parser.parse(jsonStr);
        JSONObject header = (JSONObject) tx.get("header");
        JSONObject data = (JSONObject) tx.get("data");

        // 2. Ready to build a TransactionHeader
        byte[] version = header.get("version").toString().getBytes();
        byte[] type = header.get("type").toString().getBytes();
        byte[] dataHash = header.get("dataHash").toString().getBytes();
        long timestamp = Long.parseLong(header.get("timestamp").toString());
        long dataSize = Long.parseLong(header.get("dataSize").toString());;
        byte[] signature = header.get("signature").toString().getBytes();
        String dataStr = data.toString();

        String res = "\n\nversion=" + version + "\n"
                    + "type=" + type + "\n"
                    + "dataHash=" + dataHash + "\n"
                    + "timestamp=" + timestamp + "\n"
                    + "dataSize=" + dataSize + "\n"
                    + "signature=" + signature + "\n"
                    + "data=" + data + "\n";

        log.debug(res);

        // ** Validation **

        // 3. Create a TransactionHeader
        TransactionHeader txHeader = new TransactionHeader(type, version, dataHash, timestamp, dataSize, signature);

        // 4. Create a Transaction
        Transaction transaction = new Transaction(txHeader, dataStr);
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
    public void mappingTrnasactionMock() throws IOException {

        String tx = "{\"version\":\"0\",\"type\":\"00000000000000\",\"timestamp\":\"155810745733540\",\"from\":\"04a0cb0bc45c5889b8136127409de1ae7d3f668e5f29115730362823ed5223aff9b2c22210280af1249e27b08bdeb5c0160af74ec5237292b5ee94bd148c9aabbb\",\"dataHash\":\"ba5f3ea40e95f49bce11942f375ebd3882eb837976eda5c0cb78b9b99ca7b485\",\"dataSize\":\"13\",\"signature\":\"b86e02880e12c575e56c5d15e1f491595219295076721a5bfb6042463d6a2d768331691db0b8de852390305c0f2b218e596e4a59bf54029cf6a8b9afdbb274104\",\"transactionHash\":\"c6b5e583ec18891e9de0e29c3f0358a5c99c474bc3ee78e90c618db72193c0\",\"transactionData\":\"{}\"}";

        ObjectMapper mapper = new ObjectMapper();

        TransactionMock transaction = mapper.reader()
                .forType(TransactionMock.class).readValue(tx);

        System.out.println(transaction);

        TransactionPoolMock transactionPoolMock = new TransactionPoolMock();
        transactionPoolMock.addTx(transaction.retTxMock());
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



