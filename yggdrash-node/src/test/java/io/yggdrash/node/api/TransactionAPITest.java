package io.yggdrash.node.api;

import io.yggdrash.core.Block;
import io.yggdrash.node.BlockBuilder;
import io.yggdrash.node.mock.BlockBuilderMock;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.LinkedHashMap;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@Import(ApplicationConfig.class)
public class TransactionAPITest {
    private static final Logger log = LoggerFactory.getLogger(TransactionAPI.class);

    @Test
    public void test() throws Exception {

        TransactionMock txMock = new TransactionMock();
        String txObj = txMock.retTxMock();

        TransactionReceiptMock txReceiptMock = new TransactionReceiptMock();
        String txReceiptObj = txReceiptMock.retTxReceiptMock();

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

        assertThat(txObj).isEqualTo(txapi.getTransactionByHash(hashOfTx));
        assertThat(txObj).isEqualTo(txapi.getTransactionByBlockHashAndIndex(hashOfBlock, txIndexPosition));
        assertThat(txObj).isEqualTo(txapi.getTransactionByBlockNumberAndIndex(blockNumber, txIndexPosition));
        assertThat(txReceiptObj).isEqualTo(txapi.getTransactionReceipt(hashOfTx));

        String tx = "hello";
        String rawTx = "hahahahaha";
        String txHash = "0x76a9fa4681a8abf94618543872444ba079d5302203ac6a5b5b2087a9f56ea8bf";
        String zeroHash = "0x0000000000000000000000000000000000000000";
        String resTx = "{[get : " + tx + "][result : {txhash : " + txHash + "}]}";
        String resRawTx = "{[get : " + rawTx + "][result : {txhash : " + zeroHash + "}]}";

        assertThat(resTx).isEqualTo(txapi.sendTransaction(tx));
        assertThat(resRawTx).isEqualTo(txapi.sendRawTransaction(rawTx));

        AccountAPIImpl accapi = new AccountAPIImpl();
        String account = accapi.createAccount();
        assertThat(account).isNotEmpty();

        log.debug("=============================[Transaction Test]=================================");
        log.debug("[res] txObj : " + txObj);
        log.debug("[res] txReceiptObj : " + txReceiptObj);
        log.debug("=============================[Account Test]=====================================");
        log.debug("[res] accountStr : " + account);
        log.debug("================================================================================");

        BlockMock blockMock = new BlockMock();
        log.debug("blockMock" + blockMock.retBlockMock());

        BlockBuilderMock blockBuilderMock = new BlockBuilderMock();
        Block block = blockBuilderMock.build("test");
        log.debug(block.getBlockHash());
        log.debug(block.getPrevBlockHash());
        log.debug("blockBuilderMock : " + block.toString());

    }
}



