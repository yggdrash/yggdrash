package io.yggdrash.node.api;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.primitives.Longs;
import com.google.gson.JsonObject;
import com.googlecode.jsonrpc4j.JsonRpcHttpClient;
import com.googlecode.jsonrpc4j.ProxyUtil;
import io.yggdrash.core.NodeManager;
import io.yggdrash.core.Transaction;
import io.yggdrash.core.TransactionValidator;
import io.yggdrash.node.mock.NodeManagerMock;
import io.yggdrash.node.mock.TransactionMock;
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
import java.security.SignatureException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@Import(JsonRpcConfig.class)
public class TransactionApiImplTest {
    private static final Logger log = LoggerFactory.getLogger(TransactionApi.class);

    private final NodeManager nodeManager = new NodeManagerMock();

    @Autowired
    JsonRpcHttpClient jsonRpcHttpClient;

    private final TransactionApiImpl txApiImpl = new TransactionApiImpl(nodeManager);
    private final String address = "0x407d73d8a49eeb85d32cf465507dd71d507100c1";
    private final String tag = "latest";
    private final String hashOfTx =
            "0xbd729cb4ecbcbd3fc66bedb43dbb856f5e71ebefff95fc9503b92921b8466bab";
    private final String hashOfBlock =
            "0x76a9fa4681a8abf94618543872444ba079d5302203ac6a5b5b2087a9f56ea8bf";
    private final int blockNumber = 1;
    private final int txIndexPosition = 1;

    @Test
    public void setJsonRpcHttpClient() {
        TransactionApi api = ProxyUtil.createClientProxy(getClass().getClassLoader(),
                TransactionApi.class, jsonRpcHttpClient);
        assertThat(api).isNotNull();
    }

    @Test
    public void getTransactionCountTest() {
        try {
            TransactionApi api = ProxyUtil.createClientProxy(getClass().getClassLoader(),
                    TransactionApi.class, jsonRpcHttpClient);
            assertThat(api).isNotNull();
            assertThat(api.getTransactionCount(address, tag)).isNotZero();
        } catch (Exception exception) {
            log.debug("\n\ngetTransactionCountTest :: exception => " + exception);
        }
    }

    @Test
    public void getBlockTransactionCountByHashTest() {
        try {
            TransactionApi api = ProxyUtil.createClientProxy(getClass().getClassLoader(),
                    TransactionApi.class, jsonRpcHttpClient);
            assertThat(api).isNotNull();
            assertThat(api.getBlockTransactionCountByHash(hashOfTx)).isNotZero();
        } catch (Exception exception) {
            log.debug("\n\ngetBlockTransactionCountByHashTest :: exception => " + exception);
        }
    }

    @Test
    public void getBlockTransactionCountByNumberTest() {
        try {
            TransactionApi api = ProxyUtil.createClientProxy(getClass().getClassLoader(),
                    TransactionApi.class, jsonRpcHttpClient);
            assertThat(api).isNotNull();
            assertThat(api.getBlockTransactionCountByNumber(blockNumber)).isNotZero();
        } catch (Exception exception) {
            log.debug("\n\ngetBlockTransactionCountByNumberTest :: exception => " + exception);
        }
    }

    @Test
    public void getTransactionByHashTest() {
        try {
            TransactionApi api = ProxyUtil.createClientProxy(getClass().getClassLoader(),
                    TransactionApi.class, jsonRpcHttpClient);
            assertThat(api).isNotNull();
            assertThat(api.getTransactionByHash(hashOfBlock)).isNotNull();
        } catch (Exception exception) {
            log.debug("\n\ngetTransactionByHashTest :: exception => " + exception);
        }
    }

    @Test
    public void getTransactionByBlockHashAndIndexTest() {
        try {
            TransactionApi api = ProxyUtil.createClientProxy(getClass().getClassLoader(),
                    TransactionApi.class, jsonRpcHttpClient);
            assertThat(api).isNotNull();
            assertThat(api.getTransactionByBlockHashAndIndex(hashOfBlock, txIndexPosition))
                    .isNotNull();
        } catch (Exception exception) {
            log.debug("\n\ngetTransactionByBlockHashAndIndexTest :: exception => " + exception);
        }
    }

    @Test
    public void getTransactionByBlockNumberAndIndexTest() {
        try {
            TransactionApi api = ProxyUtil.createClientProxy(getClass().getClassLoader(),
                    TransactionApi.class, jsonRpcHttpClient);
            assertThat(api).isNotNull();
            assertThat(api.getTransactionByBlockNumberAndIndex(blockNumber, txIndexPosition))
                    .isNotNull();
        } catch (Exception exception) {
            log.debug("\n\ngetTransactionByBlockNumberAndIndexTest :: exception => " + exception);
        }
    }

    @Test
    public void getTransactionReceiptTest() {
        try {
            TransactionApi api = ProxyUtil.createClientProxy(getClass().getClassLoader(),
                    TransactionApi.class, jsonRpcHttpClient);
            assertThat(api).isNotNull();
            assertThat(api.getTransactionReceipt(hashOfTx)).isNotNull();
        } catch (Exception exception) {
            log.debug("\n\ngetTransactionReceiptTest :: exception => " + exception);
        }
    }

    @Test
    public void checkTransactionJsonFormat() throws IOException {
        JsonObject data = new JsonObject();
        Transaction tx = new Transaction(this.nodeManager.getWallet(), data);
        ObjectMapper objectMapper = new ObjectMapper();
        log.debug("\n\nTransaction Format : " + objectMapper.writeValueAsString(tx));
    }

    @Test
    public void sendTransactionTest() throws IOException {
        // Get Transaction of JsonString as Param
        ObjectMapper objectMapper = new ObjectMapper();
        JsonObject json = new JsonObject();
        json.addProperty("id", "0");
        json.addProperty("name", "Rachael");
        json.addProperty("age", "27");
        Transaction transaction = new Transaction(this.nodeManager.getWallet(), json);
        String jsonStr = objectMapper.writeValueAsString(transaction);

        // Request Transaction with jsonStr
        try {
            // Convert string to Transaction
            String txHash = txApiImpl.sendTransaction(jsonStr);

            TransactionApi api = ProxyUtil.createClientProxy(getClass().getClassLoader(),
                    TransactionApi.class, jsonRpcHttpClient);
            assertThat(api).isNotNull();
            String resTxHash = api.sendTransaction(jsonStr);
            assertThat(txHash).isEqualTo(resTxHash);
        } catch (Exception exception) {
            log.debug("\n\njsonStringToTxTest :: exception => " + exception);
        }
    }

    @Test
    public void sendRawTransactionTest() {
        // Create an input parameter
        byte[] type = new byte[4];
        byte[] version = new byte[4];
        byte[] dataHash = new byte[32];
        type = "0000".getBytes();
        version = "0000".getBytes();
        dataHash = Base64.decode("3n5eY3WkYCiiM1f6SlFAS8iM7BMmQt7VNyVU3Ie1CRw=");
        byte[] timestamp = Longs.toByteArray(Long.parseLong("155810745733540"));
        byte[] dataSize = Longs.toByteArray((long) 2);

        byte[] signature = new byte[65];
        signature = Base64.decode("HAVWCp/cnCXt/v5aNI2xgu2bKD5zSzmvuCd4Wn95IiMtdTB"
                + "Lk9XEd0qy2InfBnia2w/R+iQJvELutNXnJAIjd+g=");
        byte[] data = "{\"id\":\"0\",\"name\":\"Rachael\",\"age\":\"27\"}".getBytes();

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

        // Request Transaction with byteArr
        try {
            // Convert byteArray to Transaction
            byte[] txHash = txApiImpl.sendRawTransaction(input);

            TransactionApi api = ProxyUtil.createClientProxy(getClass().getClassLoader(),
                                                             TransactionApi.class,
                                                             jsonRpcHttpClient);
            assertThat(api).isNotNull();
            byte[] resTxHash = api.sendRawTransaction(input);
            assertThat(txHash).isEqualTo(resTxHash);
        } catch (Exception exception) {
            log.debug("\n\nbyteArrToTxTest :: exception => " + exception);
        }
    }

    @Test
    public void newPendingTransactionFilterTest() {
        try {
            TransactionApi api = ProxyUtil.createClientProxy(getClass().getClassLoader(),
                    TransactionApi.class, jsonRpcHttpClient);
            assertThat(api).isNotNull();
            assertThat(api.newPendingTransactionFilter()).isNotZero();
        } catch (Exception exception) {
            log.debug("\n\njsonStringToTxTest :: exception => " + exception);
        }
    }

    @Test
    public void createTransactionMock() throws IOException {
        TransactionMock txMock = new TransactionMock(this.nodeManager);
        log.debug("txMock : " + txMock.retTxMock());
    }

    @Test
    public void transactionApiImplTest() {
        try {
            assertThat(1).isEqualTo(txApiImpl.getTransactionCount(address, tag));
            assertThat(2).isEqualTo(txApiImpl.getTransactionCount(address, blockNumber));
            assertThat(3).isEqualTo(txApiImpl.getBlockTransactionCountByHash(hashOfBlock));
            assertThat(4).isEqualTo(txApiImpl.getBlockTransactionCountByNumber(blockNumber));
            assertThat(5).isEqualTo(txApiImpl.getBlockTransactionCountByNumber(tag));
            assertThat(6).isEqualTo(txApiImpl.newPendingTransactionFilter());
        } catch (Exception exception) {
            log.debug("\n\ntransactionApiImplTest :: exception => " + exception);
        }
    }

    @Test
    public void txSigValidateTest() throws IOException,SignatureException {
        // Create Transaction
        JsonObject json = new JsonObject();
        json.addProperty("id", "0");
        json.addProperty("name", "Rachael");
        json.addProperty("age", "27");
        Transaction tx = new Transaction(this.nodeManager.getWallet(), json);

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String jsonStr = mapper.writeValueAsString(tx);

        // Receive Transaction
        Transaction resTx = mapper.readValue(jsonStr, Transaction.class);
        byte[] resSignature = resTx.getHeader().getSignature();
        byte[] resSignDataHash = resTx.getHeader().getSignDataHash();

        // Signature Validation
        TransactionValidator txValidator = new TransactionValidator();
        assertTrue(txValidator.txSigValidate(resSignDataHash, resSignature));
    }
}



