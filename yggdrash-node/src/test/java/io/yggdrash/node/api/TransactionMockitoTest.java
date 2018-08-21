package io.yggdrash.node.api;

import io.yggdrash.core.BlockHusk;
import io.yggdrash.core.NodeManager;
import io.yggdrash.core.TransactionHusk;
import io.yggdrash.core.TransactionReceipt;
import io.yggdrash.core.Wallet;
import io.yggdrash.node.TestUtils;
import org.apache.commons.codec.binary.Hex;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TransactionMockitoTest {

    @Mock
    private NodeManager nodeManagerMock;
    private TransactionHusk tx;
    private BlockHusk block;
    private Wallet wallet;

    private TransactionApiImpl txApiImpl;
    private String hashOfTx;
    private String hashOfBlock;

    @Before
    public void setup() throws Exception {
        wallet = new Wallet();
        txApiImpl = new TransactionApiImpl(nodeManagerMock);

        tx = TestUtils.createTxHusk(wallet);
        hashOfTx = tx.getHash().toString();
        List<TransactionHusk> txList = new ArrayList<>();
        txList.add(tx);
        txList.add(tx);
        txList.add(tx);
        block = TestUtils.createBlockHuskByTxList(wallet, txList);
        hashOfBlock = block.getHash().toString();
    }

    private static final Logger log = LoggerFactory.getLogger(TransactionApi.class);

    @Test
    public void getTransactionCountTest() {
        when(nodeManagerMock.getBlockByIndexOrHash(any())).thenReturn(block);
        Integer res = txApiImpl.getTransactionCount(wallet.getHexAddress(), 1);
        Integer res2 = txApiImpl.getTransactionCount(wallet.getHexAddress(), "latest");
        Integer sizeOfTxList = 3;
        assertThat(res).isEqualTo(sizeOfTxList);
        assertThat(res2).isEqualTo(res);
    }

    @Test
    public void hexEncodeAndDecodeByteArray() throws Exception {
        byte[] origin = tx.getAddress().getBytes();
        String encoded = Hex.encodeHexString(origin);
        byte[] decoded = Hex.decodeHex(encoded);

        assertArrayEquals(decoded, origin);
    }

    @Test
    public void getTransactionByHash() {
        when(nodeManagerMock.getTxByHash(hashOfTx)).thenReturn(tx);
        TransactionHusk res = txApiImpl.getTransactionByHash(hashOfTx);
        assertThat(res).isNotNull();
        assertEquals(res.getHash().toString(), hashOfTx);
    }

    @Test
    public void getTransactionByBlockHashAndIndexTest() {
        when(nodeManagerMock.getBlockByIndexOrHash(hashOfBlock)).thenReturn(block);
        TransactionHusk res = txApiImpl.getTransactionByBlockHashAndIndex(hashOfBlock, 0);
        assertEquals(res.getHash().toString(), hashOfTx);
    }

    @Test
    public void getTransactionByBlockNumberAndIndexTest() {
        when(nodeManagerMock.getBlockByIndexOrHash(anyString())).thenReturn(block);
        TransactionHusk res = txApiImpl.getTransactionByBlockNumberAndIndex(0, 0);
        TransactionHusk res2 = txApiImpl.getTransactionByBlockNumberAndIndex("latest", 0);
        assertEquals(res.getHash(), res2.getHash());
    }

    @Test
    public void getTransactionReceiptTest() {
        String transactionHash =
                "0xb903239f8543d04b5dc1ba6579132b143087c68db1b2168786408fcbce568238";
        String blockHash =
                "0xc6ef2fc5426d6ad6fd9e2a26abeab0aa2411b7ab17f30a99d3cb96aed1d1055b";
        TransactionReceipt txRecipt = txApiImpl.getTransactionReceipt(transactionHash);
        assertEquals(txRecipt.blockHash, blockHash);
    }

    @Test
    public void sendTransactionTest() {
        when(nodeManagerMock.addTransaction(tx)).thenReturn(tx);
        String res = txApiImpl.sendTransaction(TransactionDto.createBy(tx));
        assertEquals(res, hashOfTx);
    }

    @Test
    public void sendRawTransaction() throws Exception {
        when(nodeManagerMock.addTransaction(any(TransactionHusk.class))).thenReturn(tx);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = new ObjectOutputStream(bos);
        out.writeObject(tx);
        out.flush();
        byte[] txBytes = bos.toByteArray();
        bos.close();
        byte[] res = txApiImpl.sendRawTransaction(txBytes);
        log.debug("\n\nres :: " + Hex.encodeHexString(res));
        assertThat(res).isNotEmpty();
    }
}