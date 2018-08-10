package io.yggdrash.node.api;

import io.yggdrash.core.Block;
import io.yggdrash.core.BlockBody;
import io.yggdrash.core.BlockHeader;
import io.yggdrash.core.NodeManager;
import io.yggdrash.core.Transaction;
import io.yggdrash.core.TransactionReceipt;
import io.yggdrash.core.Wallet;
import io.yggdrash.node.mock.TransactionMock;
import org.apache.commons.codec.binary.Hex;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
@RunWith(MockitoJUnitRunner.class)
public class TransactionMockitoTest {
    @Mock
    private NodeManager nodeManagerMock;
    private Transaction tx;
    private Block block;
    private Wallet wallet;

    private TransactionApiImpl txApiImpl;
    private String hashOfTx;
    private String hashOfBlock;
    private String address;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);

        wallet = new Wallet();
        address = Hex.encodeHexString(wallet.getAddress());
        txApiImpl = new TransactionApiImpl(nodeManagerMock);

        TransactionMock txMock = new TransactionMock();
        tx = txMock.retTxMock(wallet);
        hashOfTx = tx.getHashString();
        List<Transaction> txList = new ArrayList<>();
        txList.add(tx);
        txList.add(tx);
        txList.add(tx);

        BlockBody sampleBody = new BlockBody(txList);
        BlockHeader genesisBlockHeader = new BlockHeader.Builder()
                .blockBody(sampleBody)
                .prevBlock(null)
                .build(wallet);

        BlockHeader blockHeader = new BlockHeader.Builder()
                .blockBody(sampleBody)
                .prevBlock(new Block(genesisBlockHeader, sampleBody)) // genesis block
                .build(wallet);
        block = new Block(blockHeader, sampleBody);
        hashOfBlock = block.getBlockHash();
    }

    private static final Logger log = LoggerFactory.getLogger(TransactionApi.class);

    @Test
    public void getTransactionCountTest() {
        when(nodeManagerMock.getBlockByIndexOrHash(any())).thenReturn(block);
        Integer res = txApiImpl.getTransactionCount(address, 1);
        Integer res2 = txApiImpl.getTransactionCount(address, "latest");
        Integer sizeOfTxList = 3;
        assertThat(res).isEqualTo(sizeOfTxList);
        assertThat(res2).isEqualTo(res);
    }

    @Test
    public void hexEndcodeAndDecodeByteArray() throws Exception {
        String str = Hex.encodeHexString(tx.getHeader().getAddress());
        byte[] arr = Hex.decodeHex(str);
        byte[] origin = tx.getHeader().getAddress();

        if (Arrays.equals(arr, origin)) {
            log.debug("\n\ntrue");
        } else {
            log.debug("\n\nfalse");
        }
    }

    @Test
    public void getTransactionByHash() {
        when(nodeManagerMock.getTxByHash(hashOfTx)).thenReturn(tx);
        Transaction res = txApiImpl.getTransactionByHash(hashOfTx);
        assertThat(res).isNotNull();
        assertEquals(res.getHashString(), hashOfTx);
    }

    @Test
    public void getTransactionByBlockHashAndIndexTest() throws IOException {
        when(nodeManagerMock.getBlockByIndexOrHash(hashOfBlock)).thenReturn(block);
        Transaction res = txApiImpl.getTransactionByBlockHashAndIndex(hashOfBlock, 0);
        assertEquals(res.getHashString(), hashOfTx);
    }

    @Test
    public void getTransactionByBlockNumberAndIndexTest() throws IOException {
        when(nodeManagerMock.getBlockByIndexOrHash(anyString())).thenReturn(block);
        Transaction res = txApiImpl.getTransactionByBlockNumberAndIndex(0, 0);
        Transaction res2 = txApiImpl.getTransactionByBlockNumberAndIndex("latest", 0);
        assertEquals(res.getHashString(), res2.getHashString());
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
    public void sendTransactionTest() throws Exception {
        when(nodeManagerMock.addTransaction(tx)).thenReturn(tx);
        String res = txApiImpl.sendTransaction(tx);
        assertEquals(res, hashOfTx);
    }

    @Test
    public void sendRawTransaction() throws Exception {
        when(nodeManagerMock.addTransaction(any(Transaction.class))).thenReturn(tx);
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