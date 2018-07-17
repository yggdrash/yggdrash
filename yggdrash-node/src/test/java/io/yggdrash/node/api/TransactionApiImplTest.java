package io.yggdrash.node.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.primitives.Longs;
import com.google.gson.JsonObject;
import com.googlecode.jsonrpc4j.JsonRpcHttpClient;
import com.googlecode.jsonrpc4j.ProxyUtil;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.nio.ByteBuffer;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@Import(ApplicationConfig.class)
public class TransactionApiImplTest {
    private static final Logger log = LoggerFactory.getLogger(TransactionApi.class);

    @Autowired
    JsonRpcHttpClient jsonRpcHttpClient;

    @Test
    public void setJsonRpcHttpClient() {
        TransactionApi api = ProxyUtil.createClientProxy(getClass().getClassLoader(), TransactionApi.class, jsonRpcHttpClient);
        assertThat(api).isNotNull();
    }

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
        ObjectMapper objectMapper = new ObjectMapper();
        Account from = new Account();
        JsonObject json = new JsonObject();
        json.addProperty("id", "0");
        json.addProperty("name", "Rachael");
        json.addProperty("age", "27");
        Transaction transaction = new Transaction(from, json);
        String jsonStr = objectMapper.writeValueAsString(transaction);

        // Create Transaction by transactionDto
        TransactionDto transactionDto = new TransactionDto();
        Transaction tx = transactionDto.jsonStringToTx(jsonStr);

        // Request Transaction with jsonStr
        TransactionApi api = ProxyUtil.createClientProxy(getClass().getClassLoader(), TransactionApi.class, jsonRpcHttpClient);
        assertThat(api).isNotNull();
        Transaction tx2 = api.sendTransaction(jsonStr);

        assertThat(objectMapper.writeValueAsString(tx).equals(objectMapper.writeValueAsString(tx2)));
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

        // Request Transaction with byteArr
        TransactionApi api = ProxyUtil.createClientProxy(getClass().getClassLoader(), TransactionApi.class, jsonRpcHttpClient);
        assertThat(api).isNotNull();
        Transaction tx2 = api.sendRawTransaction(input);

        // Create Transaction JsonObject
        ObjectMapper objectMapper = new ObjectMapper();
        assertThat(objectMapper.writeValueAsString(tx).equals(objectMapper.writeValueAsString(tx2)));
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
        int blockNumber = 1;

        assertThat(1).isEqualTo(txapi.getTransactionCount(address, tag));
        assertThat(2).isEqualTo(txapi.getTransactionCount(address, blockNumber));
        assertThat(3).isEqualTo(txapi.getBlockTransactionCountByHash(hashOfBlock));
        assertThat(4).isEqualTo(txapi.getBlockTransactionCountByNumber(blockNumber));
        assertThat(5).isEqualTo(txapi.getBlockTransactionCountByNumber(tag));
        assertThat(6).isEqualTo(txapi.newPendingTransactionFilter());
    }

}



