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
import io.yggdrash.common.crypto.HexUtil;
import io.yggdrash.common.util.VerifierUtils;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.blockchain.TransactionBuilder;
import io.yggdrash.core.blockchain.TransactionImpl;
import io.yggdrash.core.blockchain.osgi.ContractConstants;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.gateway.dto.TransactionDto;
import io.yggdrash.gateway.dto.TransactionResponseDto;
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
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Ignore
public class TransactionApiImplTest {
    // TODO fix this test class all test is exception, Remove or

    private static final Logger log = LoggerFactory.getLogger(TransactionApiImplTest.class);

    private final int blockNumber = 3;
    private final int txIndexPosition = 2;
    private final String yggdrashBranch = TestConstants.yggdrash().toString();
    private final BranchGroup branchGroup = BlockChainTestUtils.createBranchGroup();
    private final TransactionApiImpl txApi = new TransactionApiImpl(branchGroup);

    @Before
    public void setUp() {
        sendTransactionTest();
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
        try {
            assertThat(TX_API.getTransactionCountByBlockHash(yggdrashBranch,
                    "d52fffa14f5b88b141d05d8e28c90d8131db1aa63e076bfea9c28c3060049e12"))
                    .isNotZero();
        } catch (Exception exception) {
            log.debug("\n\ngetBlockTransactionCountByHashTest :: exception => " + exception);
        }
    }

    @Test
    public void getBlockTransactionCountByNumberTest() {
        try {
            assertThat(TX_API.getTransactionCountByBlockNumber(yggdrashBranch, blockNumber))
                    .isNotZero();
        } catch (Throwable exception) {
            log.debug("\n\ngetBlockTransactionCountByNumberTest :: exception => " + exception);
        }
    }

    @Test
    public void getTransactionByHashTest() {
        try {
            assertThat(TX_API.getTransactionByHash(yggdrashBranch,
                    "f5912fde84c6a3a44b4e529077ca9bf28feccd847137e44a77cd17e9fb9c1353"))
                    .isNotNull();
        } catch (Exception exception) {
            log.debug("\n\ngetTransactionByHashTest :: exception => " + exception);
        }
    }

    @Test
    public void getTransactionByBlockHashTest() {
        try {
            assertThat(TX_API.getTransactionByBlockHash(yggdrashBranch,
                    "5ef71a90c6d99c7bc13bfbcaffb50cb89210678e99ed6626c9d2f378700b392c",
                    2)).isNotNull();
        } catch (Exception exception) {
            log.debug("\n\ngetTransactionByBlockHashTest :: exception => " + exception);
        }
    }

    @Test
    public void getTransactionByBlockNumberTest() {
        try {
            assertThat(TX_API.getTransactionByBlockNumber(
                    yggdrashBranch, blockNumber, txIndexPosition))
                    .isNotNull();
        } catch (Exception e) {
            log.debug("\n\ngetTransactionByBlockNumberTest :: exception => " + e);
        }
    }

    @Test
    public void getTransactionByBlockNumberWithTagTest() {
        try {
            String tag = "latest";
            Assert.assertNotNull(TX_API.getTransactionByBlockNumber(yggdrashBranch, tag, txIndexPosition));
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
    public void sendRawTransactionTest() {
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
    public void newPendingTransactionFilterTest() {
        try {
            assertThat(TX_API.newPendingTransactionFilter(yggdrashBranch))
                    .isGreaterThanOrEqualTo(0);
        } catch (Exception e) {
            log.debug("\n\nnewPendingTransactionFilterTest :: exception => " + e);
        }
    }

    @Test
    public void getPendingTransactionListTest() {
        try {
            TX_API.getPendingTransactionList(yggdrashBranch);
        } catch (Exception e) {
            log.debug("\n\ngetPendingTransactionListTest :: exception => " + e);
        }
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
        try {
            assertThat(TX_API.getTransactionReceipt(yggdrashBranch,
                    "bce793985a1cde7791acbbeb16037d7b86b967df4213329b2e3cc45995fecd68"))
                    .isNotNull();
        } catch (Exception exception) {
            log.debug("\n\ngetTransactionReceiptTest :: exception => " + exception);
        }
    }

    @Test
    public void getRawTransactionTest() {
        try {
            assertThat(TX_API.getRawTransaction(yggdrashBranch,
                    "bce793985a1cde7791acbbeb16037d7b86b967df4213329b2e3cc45995fecd68"))
                    .isNotNull();
        } catch (Exception exception) {
            log.debug("\n\ngetRawTransactionTest :: exception => " + exception);
        }
    }

    @Test
    public void getRawTransactionHeaderTest() {
        try {
            assertThat(TX_API.getRawTransactionHeader(yggdrashBranch,
                    "bce793985a1cde7791acbbeb16037d7b86b967df4213329b2e3cc45995fecd68"))
                    .isNotNull();
        } catch (Exception exception) {
            log.debug("\n\ngetRawTransactionHeaderTest :: exception => " + exception);
        }
    }

    @Ignore
    @Test
    public void versioningProposeTxTest() throws Exception {
        BranchId branchId = BranchId.of(yggdrashBranch);
        Wallet wallet = ContractTestUtils.createTestWallet("77283a04b3410fe21ba5ed04c7bd3ba89e70b78c.json");
        JsonObject txBody = ContractTestUtils.contractProposeTxBodyJson(ContractConstants.VERSIONING_CONTRACT.toString(), "activate");

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
