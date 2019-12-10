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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.ContractTestUtils;
import io.yggdrash.TestConstants;
import io.yggdrash.common.contract.ContractVersion;
import io.yggdrash.common.util.VerifierUtils;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.blockchain.TransactionBuilder;
import io.yggdrash.core.blockchain.osgi.ContractConstants;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.gateway.dto.BlockDto;
import io.yggdrash.gateway.dto.TransactionDto;
import io.yggdrash.gateway.dto.TransactionResponseDto;
import io.yggdrash.node.config.RabbitMQProperties;
import org.apache.commons.codec.binary.Hex;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static io.yggdrash.node.api.JsonRpcConfig.BLOCK_API;
import static io.yggdrash.node.api.JsonRpcConfig.TX_API;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Ignore
public class TransactionApiImplTest {
    // TODO fix this test class all test is exception, Remove or

    private static final Logger log = LoggerFactory.getLogger(TransactionApiImplTest.class);

    private final String yggdrashBranch = TestConstants.yggdrash().toString();
    private final BranchGroup branchGroup = BlockChainTestUtils.createBranchGroup();
    private final TransactionApiImpl txApi = new TransactionApiImpl(branchGroup, new RabbitMQProperties());
    private final BlockApiImpl blockApi = new BlockApiImpl(branchGroup);
    private String testTransactionHash = null;
    private String testBlockHash = null;

    @Before
    public void setUp() {
        // TODO check TX_API is READY
        //sendTransactionTest();
        BlockDto genesisBlock = blockApi.getBlockByNumber(yggdrashBranch, 0, true);
        testBlockHash = genesisBlock.blockId;
        testTransactionHash = genesisBlock.body.get(0).txId; // exist tx
    }

    @Test
    public void blockApiIsNotNull() {
        assertThat(BLOCK_API).isNotNull();
    }

    @Test
    public void txApiIsNotNull() {
        assertThat(TX_API).isNotNull();
    }

    @Test
    public void getBlockTransactionCountByHashTest() {
        assertThat(txApi.getTransactionCountByBlockHash(yggdrashBranch, testBlockHash))
                .isNotZero();
    }

    @Test
    public void getBlockTransactionCountByNumberTest() {
        // Genesis Block Transaction
        assertThat(txApi.getTransactionCountByBlockNumber(yggdrashBranch, 0))
                .isNotZero();
    }

    @Test
    public void getTransactionByHashTest() {
        assertThat(txApi.getTransactionByHash(yggdrashBranch, testTransactionHash))
                .isNotNull();
    }

    @Test
    public void getTransactionByBlockHashTest() {
        // Get genesis Block and check Hash
        assertThat(txApi.getTransactionByBlockHash(yggdrashBranch,
                testBlockHash,
                0)).isNotNull();
    }

    @Test
    public void getTransactionByBlockNumberTest() {
        assertThat(txApi.getTransactionByBlockNumber(yggdrashBranch, 0, 0))
                .isNotNull();
    }

    @Test
    public void getTransactionByBlockNumberWithTagTest() {
        try {
            String tag = "latest";
            Assert.assertNotNull(txApi.getTransactionByBlockNumber(yggdrashBranch, tag, 0));
        } catch (Exception e) {
            log.debug("\n\ngetTransactionByBlockNumberWithTagTest :: exception => " + e);
        }
    }

    @Test
    public void checkTransactionJsonFormat() throws IOException {
        Transaction tx = createTx();
        String txString = new ObjectMapper().writeValueAsString(TransactionDto.createBy(tx));
        assertTrue(txString.contains(tx.getBranchId().toString()));
    }

    @Test
    @Ignore
    public void sendTransactionTest() {
        Transaction tx = createTx();
        try {
            TransactionResponseDto res = TX_API.sendTransaction(TransactionDto.createBy(tx));
            assertTrue(res.status);
        } catch (Exception exception) {
            log.debug("\n\nsendTransactionTest :: exception => " + exception);
        }
    }

    @Test
    public void invalidTransactionTest() {
        // invalid contractVersion (non existed)
        Transaction sysErrTx = BlockChainTestUtils.createInvalidTransferTx(ContractVersion.of("696e76616c6964"));
        // timeout, invalid format, untrusted
        Transaction busErrTx = BlockChainTestUtils.createInvalidTransferTx();
        try {
            TransactionResponseDto res = TX_API.sendTransaction(TransactionDto.createBy(sysErrTx));
            assertFalse(res.status);
            res = TX_API.sendTransaction(TransactionDto.createBy(busErrTx));
            assertFalse(res.status);
        } catch (Exception e) {
            log.debug("\n\ninvalidTransactionTest :: ERR => {}", e.getMessage());
        }
    }

    @Test
    @Ignore
    public void sendRawTransactionTest() {
        // TODO remove
        // Request Transaction with byteArr
        //success tx
        try {
            byte[] input = createTx().toRawTransaction();
            // Convert byteArray to Transaction
            TransactionResponseDto res = TX_API.sendRawTransaction(input);
            assertTrue(res.status);
        } catch (Exception e) {
            log.debug("\n\ninvalidTransactionTest :: ERR => {}", e.getMessage());
            e.printStackTrace();
        }
    }


    @Test
    @Ignore
    public void newPendingTransactionFilterTest() {
        // TODO remove
        try {
            assertThat(TX_API.newPendingTransactionFilter(yggdrashBranch))
                    .isGreaterThanOrEqualTo(0);
        } catch (Exception e) {
            log.debug("\n\nnewPendingTransactionFilterTest :: exception => " + e);
        }
    }

    @Test
    public void getPendingTransactionListTest() {
        assertThat(txApi.getPendingTransactionList(yggdrashBranch)).isNotNull();
    }

    @Test
    public void getPendingTransactionCountTest() {
        assertTrue(txApi.getPendingTransactionCount(yggdrashBranch) > -1);
    }

    @Test
    public void txSigValidateTest() throws IOException {
        // Create Transaction
        Transaction tx = createTx();

        ObjectMapper mapper = new ObjectMapper();
        String jsonStr = mapper.writeValueAsString(TransactionDto.createBy(tx));

        // Receive Transaction
        TransactionDto resDto = mapper.readValue(jsonStr, TransactionDto.class);

        // Signature Validation
        assertTrue(VerifierUtils.verify(TransactionDto.of(resDto)));
    }

    private Transaction createTx() {
        return BlockChainTestUtils.createTransferTx();
    }

    @Test
    public void getTransactionReceiptTest() {
        assertThat(txApi.getTransactionReceipt(yggdrashBranch,
                testTransactionHash))
                .isNotNull();
    }

    @Test
    public void getNullTransactionReceiptTest() {
        String invalidTxHash = "bce7bce793985a1cde7791acbbeb16037d7b86b967df4213329b2e3cc45995fe";
        assertThat(txApi.getTransactionReceipt(yggdrashBranch, invalidTxHash).txId)
                .isNull();
    }


    @Test
    public void getRawTransactionTest() {
        assertThat(txApi.getRawTransaction(yggdrashBranch, testTransactionHash))
                    .isNotNull();
    }

    @Test
    public void getRawTransactionHeaderTest() {
        assertThat(txApi.getRawTransactionHeader(yggdrashBranch, testTransactionHash))
                .isNotNull();
    }

    @Test
    public void sendInvalidDataFormatOfRawTx() {
        byte[] input = "Invalid Raw Transaction Input Data".getBytes();
        TransactionResponseDto transactionResponseDto = txApi.sendRawTransaction(input);
        assertTrue(!transactionResponseDto.logs.isEmpty());
    }

    @Ignore
    @Test
    public void versioningProposeTxTest() throws Exception {
        BranchId branchId = BranchId.of(yggdrashBranch);
        Wallet wallet = ContractTestUtils.createTestWallet("77283a04b3410fe21ba5ed04c7bd3ba89e70b78c.json");
        JsonObject txBody = ContractTestUtils.contractProposeTxBodyJson(
                ContractConstants.VERSIONING_CONTRACT.toString(), "activate");

        Transaction tx = new TransactionBuilder()
                .setType(Hex.decodeHex(ContractConstants.VERSIONING_TRANSACTION))
                .setTxBody(txBody)
                .setWallet(wallet)
                .setBranchId(branchId)
                .build();

        //when(branchGroup.getBranch(new BranchId(new Sha3Hash(yggdrashBranch))).isFullSynced()).thenReturn(true);
        TransactionResponseDto responseDto = txApi.sendTransaction(TransactionDto.createBy(tx));
        assertTrue(responseDto.status);
    }
}
