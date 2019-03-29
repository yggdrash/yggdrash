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
import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.TestConstants;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.gateway.dto.TransactionDto;
import io.yggdrash.node.CoinContractTestUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import static io.yggdrash.node.api.JsonRpcConfig.CONTRACT_API;
import static io.yggdrash.node.api.JsonRpcConfig.TX_API;
import static org.assertj.core.api.Assertions.assertThat;

public class ContractApiImplTest {
    private static final Logger log = LoggerFactory.getLogger(ContractApiImplTest.class);

    private final BranchId branchId = TestConstants.yggdrash();

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
        queryAndAssert("totalSupply", null, BigInteger.valueOf(1000000000000L));
    }

    @Test
    public void balanceOf() {
        Map params = createParams("address",
                "cee3d4755e47055b530deeba062c5bd0c17eb00f");
        queryAndAssert("balanceOf", params, BigInteger.valueOf(998000000000L));
    }

    @Test
    public void allowance() {
        Map<String, String> params = createParams("owner", "cee3d4755e47055b530deeba062c5bd0c17eb00f");
        params.put("spender", "1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e");

        queryAndAssert("allowance", params, BigInteger.ZERO);
    }

    @Test
    public void transfer() {
        JsonArray txBody = CoinContractTestUtils.createTransferBody(
                "1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e", new BigInteger("1000"));
        sendTransaction(txBody);
    }

    @Test
    public void approve() {
        JsonArray txBody = CoinContractTestUtils.createApproveBody(
                "1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e", new BigInteger("1000"));
        sendTransaction(txBody);
    }

    @Test
    public void transferFrom() {
        JsonArray txBody = CoinContractTestUtils.createTransferFromBody(
                "cee3d4755e47055b530deeba062c5bd0c17eb00f",
                "1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e",
                new BigInteger("1000"));
        sendTransaction(txBody);
    }

    private void queryAndAssert(String method, Map params, BigInteger expected) {
        try {
            // TODO Change CoinContract Address by Name
            BigInteger value = (BigInteger)CONTRACT_API
                    .query(branchId.toString(), TestConstants.YEED_CONTRACT.toString(),
                            method, params);
            log.debug("query : {}", value);
            log.debug("expected {}", expected);
            assertThat(value).isEqualTo(expected);
        } catch (Exception e) {
            // TODO exception is test fail
            //fail(e.getMessage());
            log.error(e.getMessage());
        }
    }

    private void sendTransaction(JsonArray txBody) {
        TransactionHusk tx = BlockChainTestUtils.createTxHusk(TestConstants.yggdrash(), txBody);
        Assert.assertTrue(tx.verify());
        try {
            TX_API.sendTransaction(TransactionDto.createBy(tx));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public static Map<String, String> createParams(String key, String value) {
        Map<String, String> params = new HashMap<>();
        params.put(key, value);
        return params;
    }
}
