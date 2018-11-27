/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.node.api;

import io.yggdrash.TestUtils;
import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.core.contract.TransactionReceipt;
import io.yggdrash.core.store.TransactionReceiptStore;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.node.api.dto.TransactionDto;
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
    private BranchId branchId;

    @Before
    public void setup() throws Exception {
        txReceiptStore = new HashMap<>();
        Wallet wallet = new Wallet();
        txApiImpl = new TransactionApiImpl(branchGroupMock);

        tx = TestUtils.createBranchTxHusk(wallet);
        branchId = tx.getBranchId();
        hashOfTx = tx.getHash().toString();
        List<TransactionHusk> txList = new ArrayList<>();
        txList.add(tx);
        txList.add(tx);
        txList.add(tx);
        txReceipt = new TransactionReceipt();
        txReceipt.setTransactionHash(hashOfTx);
        txReceiptStore.put(hashOfTx, txReceipt);
        block = TestUtils.createBlockHuskByTxList(wallet, txList);
        hashOfBlock = block.getHash().toString();
        when(branchGroupMock.getTransactionReceiptStore(branchId))
                .thenReturn(txReceiptStoreMock);
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
        when(branchGroupMock.getTxByHash(tx.getBranchId(), hashOfTx)).thenReturn(tx);
        TransactionDto res = txApiImpl.getTransactionByHash(tx.getBranchId().toString(), hashOfTx);
        assertThat(res).isNotNull();
        assertEquals(res.hash, hashOfTx);
    }

    @Test
    public void getTransactionByBlockHashTest() {
        when(branchGroupMock.getBlockByHash(tx.getBranchId(), hashOfBlock)).thenReturn(block);
        TransactionDto res = txApiImpl.getTransactionByBlockHash(
                tx.getBranchId().toString(), hashOfBlock, 0);
        assertEquals(res.hash, hashOfTx);
    }

    @Test
    public void getTransactionByLatestBlockTest() {
        when(branchGroupMock.getBlockByIndex(branchId, 0L)).thenReturn(block);
        when(branchGroupMock.getLastIndex(branchId)).thenReturn(0L);
        TransactionDto res = txApiImpl.getTransactionByBlockNumber(
                branchId.toString(), 0, 0);
        TransactionDto res2 = txApiImpl.getTransactionByBlockNumber(
                branchId.toString(), "latest", 0);
        assertEquals(res.hash, res2.hash);
    }

    @Test
    public void getTransactionReceiptTest() {
        when(txReceiptStoreMock.get(hashOfTx)).thenReturn(txReceipt);
        TransactionReceipt res = txApiImpl.getTransactionReceipt(branchId.toString(), hashOfTx);
        assertEquals(res.getTransactionHash(), hashOfTx);
    }

    @Test
    public void getAllTransactionReceiptTest() {
        when(txReceiptStoreMock.getTxReceiptStore()).thenReturn(txReceiptStore);
        Map<String, TransactionReceipt> res =
                txApiImpl.getAllTransactionReceipt(branchId.toString());
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
