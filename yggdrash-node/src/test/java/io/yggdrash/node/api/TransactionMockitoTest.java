package io.yggdrash.node.api;

import io.yggdrash.TestUtils;
import io.yggdrash.core.BlockHusk;
import io.yggdrash.core.BranchGroup;
import io.yggdrash.core.BranchId;
import io.yggdrash.core.TransactionHusk;
import io.yggdrash.core.TransactionReceipt;
import io.yggdrash.core.Wallet;
import io.yggdrash.core.store.TransactionReceiptStore;
import io.yggdrash.node.controller.TransactionDto;
import org.apache.commons.codec.binary.Hex;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TransactionMockitoTest {

    @Mock
    private BranchGroup branchGroupMock;
    @Mock
    private TransactionReceiptStore txReceiptStoreMock;
    private TransactionHusk tx;
    private BlockHusk block;

    private TransactionApiImpl txApiImpl;
    private String hashOfTx;
    private String hashOfBlock;
    private TransactionReceipt txReceipt;

    private HashMap<String, TransactionReceipt> txReceiptStore;
    private BranchId stem = BranchId.stem();

    @Before
    public void setup() throws Exception {
        txReceiptStore = new HashMap<>();
        Wallet wallet = new Wallet();
        txApiImpl = new TransactionApiImpl(branchGroupMock);

        tx = TestUtils.createTxHusk(wallet);
        hashOfTx = tx.getHash().toString();
        List<TransactionHusk> txList = new ArrayList<>();
        txList.add(tx);
        txList.add(tx);
        txList.add(tx);
        txReceipt = new TransactionReceipt();
        txReceipt.setTransactionHash(tx.getHash().toString());
        txReceiptStore.put(tx.getHash().toString(), txReceipt);
        block = TestUtils.createBlockHuskByTxList(wallet, txList);
        hashOfBlock = block.getHash().toString();
        when(branchGroupMock.getTransactionReceiptStore(stem)).thenReturn(txReceiptStoreMock);

    }

    private static final Logger log = LoggerFactory.getLogger(TransactionApi.class);

    @Test
    public void hexEncodeAndDecodeByteArrayTest() throws Exception {
        byte[] origin = tx.getAddress().getBytes();
        String encoded = Hex.encodeHexString(origin);
        byte[] decoded = Hex.decodeHex(encoded);

        assertArrayEquals(decoded, origin);
    }

    @Test
    public void getTransactionByHashTest() {
        when(branchGroupMock.getTxByHash(stem, hashOfTx)).thenReturn(tx);
        TransactionDto res = txApiImpl.getTransactionByHash(stem.toString(), hashOfTx);
        assertThat(res).isNotNull();
        assertEquals(res.getHash(), hashOfTx);
    }

    @Test
    public void getTransactionByBlockHashTest() {
        when(branchGroupMock.getBlockByHash(stem, hashOfBlock)).thenReturn(block);
        TransactionDto res = txApiImpl.getTransactionByBlockHash(
                stem.toString(), hashOfBlock, 0);
        assertEquals(res.getHash(), hashOfTx);
    }

    @Test
    public void getTransactionByLatestBlockTest() {
        when(branchGroupMock.getBlockByIndex(stem,0L)).thenReturn(block);
        when(branchGroupMock.getLastIndex(stem)).thenReturn(0L);
        TransactionDto res = txApiImpl.getTransactionByBlockNumber(
                stem.toString(), 0, 0);
        TransactionDto res2 = txApiImpl.getTransactionByBlockNumber(
                stem.toString(), "latest", 0);
        assertEquals(res.getHash(), res2.getHash());
    }

    @Test
    public void getTransactionReceiptTest() {
        when(txReceiptStoreMock.get(hashOfTx)).thenReturn(txReceipt);
        TransactionReceipt res = txApiImpl.getTransactionReceipt(stem.toString(), hashOfTx);
        assertEquals(res.getTransactionHash(), hashOfTx);
    }

    @Test
    public void getAllTransactionReceiptTest() {
        when(txReceiptStoreMock.getTxReceiptStore()).thenReturn(txReceiptStore);
        Map<String, TransactionReceipt> res = txApiImpl.getAllTransactionReceipt(stem.toString());
        assertThat(res.containsKey(hashOfTx)).isTrue();
    }

    @Test
    public void sendTransactionTest() {
        String res = txApiImpl.sendTransaction(TransactionDto.createBy(tx));
        assertThat(res).isNotEmpty();
    }

    @Test
    public void sendRawTransaction() {
        byte[] res = txApiImpl.sendRawTransaction(tx.toBinary());
        log.debug("\n\nres :: " + Hex.encodeHexString(res));
        assertThat(res).isNotEmpty();
    }
}
