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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.ContractTestUtils;
import io.yggdrash.TestConstants;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.node.CoinContractTestUtils;
import io.yggdrash.node.api.dto.TransactionDto;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;

import static io.yggdrash.node.api.JsonRpcConfig.CONTRACT_API;
import static io.yggdrash.node.api.JsonRpcConfig.TX_API;
import static org.assertj.core.api.Assertions.assertThat;

public class ContractApiImplTest {
    private static final Logger log = LoggerFactory.getLogger(ContractApiImplTest.class);

    private static BranchId branchId = TestConstants.YEED;

    @Test
    public void contractApiIsNotNull() {
        assertThat(CONTRACT_API).isNotNull();
    }

    @Test
    public void TransactionApiIsNotNull() {
        assertThat(TX_API).isNotNull();
    }

    @Test
    public void totalSupply() {
        queryAndAssert("totalSupply", new JsonObject(), BigDecimal.valueOf(1000000000000L));
    }

    @Test
    public void balanceOf() {
        JsonObject params = ContractTestUtils.createParams("address",
                "cee3d4755e47055b530deeba062c5bd0c17eb00f");
        queryAndAssert("balanceOf", params, BigDecimal.valueOf(998000000000L));
    }

    @Test
    public void allowance() {
        JsonObject params = new JsonObject();
        params.addProperty("owner", "cee3d4755e47055b530deeba062c5bd0c17eb00f");
        params.addProperty("spender", "1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e");

        queryAndAssert("allowance", params, BigDecimal.ZERO);
    }

    @Test
    public void specification() {
        try {
            JsonObject query =
                    ContractTestUtils.createQuery(branchId, "specification", new JsonObject());
            List<String> methods = (List<String>)CONTRACT_API.query(query.toString());
            assertThat(methods).isNotEmpty();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Test
    public void transfer() {
        JsonArray txBody = CoinContractTestUtils.createTransferBody(
                "1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e", new BigDecimal("1000"));
        sendTransaction(txBody);
    }

    @Test
    public void approve() {
        JsonArray txBody = CoinContractTestUtils.createApproveBody(
                "1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e", new BigDecimal("1000"));
        sendTransaction(txBody);
    }

    @Test
    public void transferFrom() {
        JsonArray txBody = CoinContractTestUtils.createTransferFromBody(
                "cee3d4755e47055b530deeba062c5bd0c17eb00f",
                "1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e",
                new BigDecimal("1000"));
        sendTransaction(txBody);
    }

    private void queryAndAssert(String method, JsonObject params, BigDecimal expected) {
        JsonObject query = ContractTestUtils.createQuery(branchId, method, params);
        try {
            BigDecimal value = (BigDecimal)CONTRACT_API.query(query.toString());
            assertThat(value).isEqualTo(expected);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private void sendTransaction(JsonArray txBody) {
        TransactionHusk tx = BlockChainTestUtils.createTxHusk(TestConstants.YEED, txBody);
        try {
            TX_API.sendTransaction(TransactionDto.createBy(tx));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}