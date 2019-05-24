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

import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.common.config.Constants;
import io.yggdrash.common.crypto.HexUtil;
import io.yggdrash.common.exception.FailedOperationException;
import io.yggdrash.contract.core.TransactionReceipt;
import io.yggdrash.contract.core.TransactionReceiptImpl;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.blockchain.TransactionHeader;
import io.yggdrash.core.blockchain.TransactionImpl;
import io.yggdrash.core.consensus.ConsensusBlock;
import io.yggdrash.gateway.dto.TransactionDto;
import io.yggdrash.gateway.dto.TransactionReceiptDto;
import io.yggdrash.gateway.dto.TransactionResponseDto;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TransactionMockitoTest {

    @Mock
    private BranchGroup branchGroupMock;
    private Transaction tx;
    private ConsensusBlock block;

    private TransactionApiImpl txApiImpl;
    private String txId;
    private String blockId;
    private TransactionReceipt txReceipt;

    private HashMap<String, TransactionReceipt> txReceiptStore;
    private BranchId branchId;

    @Before
    public void setup() {
        txReceiptStore = new HashMap<>();
        txApiImpl = new TransactionApiImpl(branchGroupMock);

        tx = BlockChainTestUtils.createBranchTx();
        branchId = tx.getBranchId();
        txId = tx.getHash().toString();
        List<Transaction> txList = new ArrayList<>();
        txList.add(tx);
        txList.add(tx);
        txList.add(tx);
        txReceipt = new TransactionReceiptImpl();
        txReceipt.setTxId(txId);
        txReceiptStore.put(txId, txReceipt);
        ConsensusBlock genesis = BlockChainTestUtils.genesisBlock();
        block = BlockChainTestUtils.createNextBlock(txList, genesis);
        blockId = block.getHash().toString();
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
        when(branchGroupMock.getTxByHash(tx.getBranchId(), txId)).thenReturn(tx);
        TransactionDto res = txApiImpl.getTransactionByHash(tx.getBranchId().toString(), txId);
        assertThat(res).isNotNull();
        assertEquals(res.txId, txId);
    }

    @Test
    public void getTransactionByBlockHashTest() {
        when(branchGroupMock.getBlockByHash(tx.getBranchId(), blockId)).thenReturn(block);
        TransactionDto res = txApiImpl.getTransactionByBlockHash(
                tx.getBranchId().toString(), blockId, 0);
        assertEquals(res.txId, txId);
    }

    @Test
    public void getTransactionByLatestBlockTest() {
        when(branchGroupMock.getBlockByIndex(branchId, 0L)).thenReturn(block);
        when(branchGroupMock.getLastIndex(branchId)).thenReturn(0L);
        TransactionDto res = txApiImpl.getTransactionByBlockNumber(
                branchId.toString(), 0, 0);
        TransactionDto res2 = txApiImpl.getTransactionByBlockNumber(
                branchId.toString(), "latest", 0);
        assertEquals(res.txId, res2.txId);
    }

    @Test
    public void getTransactionReceiptTest() {
        when(branchGroupMock.getTransactionReceipt(branchId, txId))
                .thenReturn(txReceipt);
        TransactionReceiptDto res = txApiImpl.getTransactionReceipt(branchId.toString(), txId);
        assertEquals(res.txId, txId);
    }

    @Test
    public void sendTransactionTest() {
        TransactionResponseDto res = txApiImpl.sendTransaction(TransactionDto.createBy(tx));
        assertThat(res.txHash).isNotEmpty();
    }

    @Test(expected = FailedOperationException.class)
    public void sendInvalidRawTransaction() {
        byte[] res = txApiImpl.sendRawTransaction(tx.toBinary());
        log.debug("\n\nres :: " + Hex.encodeHexString(res));
        assertThat(res).isNotEmpty();
    }

    @Test
    public void sendRawTransaction() {
        TransactionImpl testTx = new TransactionImpl(tx.getInstance());
        byte[] res = txApiImpl.sendRawTransaction(testTx.toRawTransaction());
        log.debug("\n\nres :: " + Hex.encodeHexString(res));
        assertThat(res).isNotEqualTo(testTx.getHash().getBytes());
    }

    @Test
    public void getRawTransaction() {
        when(branchGroupMock.getTxByHash(branchId, txId))
                .thenReturn(tx);
        String raw = txApiImpl.getRawTransaction(branchId.toString(), txId);
        assertThat(raw).isNotEmpty();
    }

    @Test
    public void getRawTransactionHeader() {
        when(branchGroupMock.getTxByHash(branchId, txId))
                .thenReturn(tx);
        String raw = txApiImpl.getRawTransactionHeader(branchId.toString(), txId);
        log.debug(raw);

        byte[] rawByteArray = HexUtil.hexStringToBytes(raw);
        log.debug("header  raw size : {} ", rawByteArray.length);
        assertThat(raw.length())
                .isEqualTo((TransactionHeader.LENGTH + Constants.SIGNATURE_LENGTH) * 2);
        assertThat(raw).isNotEmpty();
    }

}
