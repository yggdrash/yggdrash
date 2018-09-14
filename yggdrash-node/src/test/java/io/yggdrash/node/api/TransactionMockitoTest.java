package io.yggdrash.node.api;

import io.yggdrash.TestUtils;
import io.yggdrash.core.BlockHusk;
import io.yggdrash.core.BranchGroup;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TransactionMockitoTest {

    @Mock
    private BranchGroup branchGroupMock;
    @Mock
    private TransactionReceiptStore txReceiptStoreMock;
    private TransactionHusk tx;
    private BlockHusk block;
    private Wallet wallet;

    private TransactionApiImpl txApiImpl;
    private String hashOfTx;
    private String hashOfBlock;
    private TransactionReceipt txRecipt;

    private HashMap<String, TransactionReceipt> txReceiptStore;

    @Before
    public void setup() throws Exception {
        txReceiptStore = new HashMap<>();
        wallet = new Wallet();
        txApiImpl = new TransactionApiImpl(branchGroupMock);

        tx = TestUtils.createTxHusk(wallet);
        hashOfTx = tx.getHash().toString();
        List<TransactionHusk> txList = new ArrayList<>();
        txList.add(tx);
        txList.add(tx);
        txList.add(tx);
        txRecipt = new TransactionReceipt();
        txRecipt.setTransactionHash(tx.getHash().toString());
        txReceiptStore.put(tx.getHash().toString(), txRecipt);
        block = TestUtils.createBlockHuskByTxList(wallet, txList);
        hashOfBlock = block.getHash().toString();
        when(branchGroupMock.getTransactionReceiptStore()).thenReturn(txReceiptStoreMock);

    }

    private static final Logger log = LoggerFactory.getLogger(TransactionApi.class);

    @Test
    public void getTransactionCountTest() {
        when(branchGroupMock.getBlockByIndex(anyLong())).thenReturn(block);
        Integer res = txApiImpl.getTransactionCount(wallet.getHexAddress(), 1);
        Integer res2 = txApiImpl.getTransactionCount(wallet.getHexAddress(), "latest");
        Integer sizeOfTxList = 3;
        assertThat(res).isEqualTo(sizeOfTxList);
        assertThat(res2).isEqualTo(res);
    }

    @Test
    public void hexEncodeAndDecodeByteArrayTest() throws Exception {
        byte[] origin = tx.getAddress().getBytes();
        String encoded = Hex.encodeHexString(origin);
        byte[] decoded = Hex.decodeHex(encoded);

        assertArrayEquals(decoded, origin);
    }

    @Test
    public void getTransactionByHashTest() {
        when(branchGroupMock.getTxByHash(hashOfTx)).thenReturn(tx);
        TransactionHusk res = txApiImpl.getTransactionByHash(hashOfTx);
        assertThat(res).isNotNull();
        assertEquals(res.getHash().toString(), hashOfTx);
    }

    @Test
    public void getTransactionByBlockHashTest() {
        when(branchGroupMock.getBlockByHash(hashOfBlock)).thenReturn(block);
        TransactionHusk res = txApiImpl.getTransactionByBlockHash(hashOfBlock, 0);
        assertEquals(res.getHash().toString(), hashOfTx);
    }

    @Test
    public void getTransactionByLatestBlockTest() {
        when(branchGroupMock.getBlockByIndex(0L)).thenReturn(block);
        when(branchGroupMock.getLastIndex()).thenReturn(0L);
        TransactionHusk res = txApiImpl.getTransactionByBlockNumber(0, 0);
        TransactionHusk res2 = txApiImpl.getTransactionByLatestBlock("latest", 0);
        assertEquals(res.getHash(), res2.getHash());
    }

    @Test
    public void getTransactionReceiptTest() {
        when(txReceiptStoreMock.get(hashOfTx)).thenReturn(txRecipt);
        TransactionReceipt res = txApiImpl.getTransactionReceipt(hashOfTx);
        assertEquals(res.getTransactionHash(), hashOfTx);
    }

    @Test
    public void getAllTransactionReceiptTest() {
        when(txReceiptStoreMock.getTxReceiptStore()).thenReturn(txReceiptStore);
        Map<String, TransactionReceipt> res = txApiImpl.getAllTransactionReceipt();
        assertThat(res.containsKey(hashOfTx)).isTrue();
    }

    @Test
    public void sendTransactionTest() {
        when(branchGroupMock.addTransaction(tx)).thenReturn(tx);
        String res = txApiImpl.sendTransaction(TransactionDto.createBy(tx));
        assertEquals(res, hashOfTx);
    }

    @Test
    public void sendRawTransaction() {
        when(branchGroupMock.addTransaction(any(TransactionHusk.class))).thenReturn(tx);
        byte[] res = txApiImpl.sendRawTransaction(tx.toBinary());
        log.debug("\n\nres :: " + Hex.encodeHexString(res));
        assertThat(res).isNotEmpty();
    }
}
