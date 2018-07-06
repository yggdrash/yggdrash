package io.yggdrash.node.api;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import io.yggdrash.core.Block;
import io.yggdrash.core.Transaction;
import io.yggdrash.node.mock.TransactionHeaderMock;
import io.yggdrash.node.mock.TransactionMock;
import io.yggdrash.node.mock.BlockMock;
import io.yggdrash.node.mock.BlockBuilderMock;
import io.yggdrash.node.mock.TransactionPoolMock;
import io.yggdrash.node.mock.TransactionReceiptMock;
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
public class TransactionAPITest {
    private static final Logger log = LoggerFactory.getLogger(TransactionAPI.class);

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
        transactionPoolMock.addTx(transaction);
    }

    @Test
    public void createBlockMock() throws IOException {
        BlockMock blockMock = new BlockMock();
        log.debug("blockMock" + blockMock.retBlockMock());
    }

    @Test
    public void blockBuildMockTest() throws IOException {
        BlockBuilderMock blockBuilderMock = new BlockBuilderMock();
        Block block = blockBuilderMock.build("test");
        log.debug("blockBuilderMock : " + block.toString());
    }

    @Test
    public void createTransaction() throws Exception {
        String json = "{\"header\":{\"type\":\"0\",\"version\":\"0000\",\"dataHash\":\"d8d998149828f80964f530405cec906db0d355aa6445b087ec7533a48aa8bc8a\",\"timestamp\":\"20395381177213\",\"dataSize\":\"463\",\"signature\":\"1b64813bc1d77f78d6b7b2ddfb10082c27a0fff3889bdcf7f9be6238187fe58b7738bc696805a5cdad89247fcd1ca8f5f9f96ca4fe7d1ddc67693c8e6ff7b72d68\"}}";
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(DeserializationFeature.UNWRAP_ROOT_VALUE);

        TransactionHeaderMock txHeaderMock = mapper.reader()
                .forType(TransactionHeaderMock.class)
                .readValue(json);
    }

    @Test
    public void transactionAPIImplTest() throws Exception {
        TransactionAPIImpl txapi = new TransactionAPIImpl();

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

    @Test
    public void accountAPIImplTest() throws Exception {
        AccountAPIImpl accapi = new AccountAPIImpl();
        String account = accapi.createAccount();
        assertThat(account).isNotEmpty();
    }
}



