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
import com.google.gson.JsonParser;
import io.yggdrash.TestUtils;
import io.yggdrash.core.blockchain.Branch;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.core.contract.ContractQry;
import io.yggdrash.core.contract.ContractTx;
import io.yggdrash.node.api.dto.TransactionDto;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

public class ContractApiImplTest {
    private static final ContractApi contractApi = new JsonRpcConfig().contractApi();
    private static final TransactionApi txApi = new JsonRpcConfig().transactionApi();
    private static Branch branch;
    private static BranchId branchId;

    @Before
    public void setUp() {
        boolean isStemTest = false;

        if (isStemTest) {
            beforeStemTest();
        } else {
            branchId = BranchId.of("275830946a84bc13ac44cca1e48570002917a02d");
        }
    }

    private static void beforeStemTest() {
        branch = Branch.of(TestUtils.createSampleBranchJson());
        branchId = branch.getBranchId();

        try {
            TransactionHusk tx =
                    ContractTx.createStemTx(TestUtils.wallet(), branch.getJson(), "create");
            txApi.sendTransaction(TransactionDto.createBy(tx));
            Thread.sleep(10000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void contractApiIsNotNull() {
        assertThat(contractApi).isNotNull();
    }

    @Test
    public void TransactionApiIsNotNull() {
        assertThat(txApi).isNotNull();
    }

    /* StemContract Test */
    @Test
    public void update() {
        try {
            String description = "hello world!";
            JsonObject updatedBranch = TestUtils.createSampleBranchJson(description);

            TransactionHusk tx =
                    ContractTx.createStemTx(TestUtils.wallet(), updatedBranch, "update");
            txApi.sendTransaction(TransactionDto.createBy(tx));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void search() {
        try {
            JsonObject queryObj = ContractQry.createQuery(branchId.toString(),
                    "search", ContractQry
                    .createParams("key", "type", "value", "immunity"));
            contractApi.query(queryObj.toString());

            queryObj = ContractQry.createQuery(branchId.toString(),
                    "search", ContractQry.createParams(
                    "key", "name", "value", "TEST1"));
            contractApi.query(queryObj.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void view() {
        try {
            JsonObject queryObj = ContractQry.createQuery(branchId.toString(),
                    "view", ContractQry.createParams(
                    "branchId", branchId.toString()));
            contractApi.query(queryObj.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void getCurrentVersion() {
        try {
            JsonObject queryObj = ContractQry.createQuery(branchId.toString(),
                    "getcurrentversion",
                    ContractQry.createParams("branchId", branchId.toString()));

            contractApi.query(queryObj.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void getVersionHistory() {
        try {
            JsonObject queryObj = ContractQry.createQuery(branchId.toString(),
                    "getversionhistory",
                    ContractQry.createParams("branchId", branchId.toString()));
            contractApi.query(queryObj.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void getAllBranchId() {
        try {
            JsonObject queryObj = ContractQry.createQuery(branchId.toString(),
                    "getallbranchid",
                    new JsonArray());
            contractApi.query(queryObj.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* CoinContract Test */
    @Test
    public void totalSupply() {
        try {
            JsonObject queryObj = ContractQry.createQuery(branchId.toString(),
                    "totalSupply", ContractQry.createParams(new HashMap<>()));
            JsonParser jsonParser = new JsonParser();
            JsonObject jsonObject = (JsonObject) jsonParser.parse(
                    contractApi.query(queryObj.toString()));
            BigDecimal result = new BigDecimal(jsonObject.get("result").getAsString());

            assertEquals(result, new BigDecimal("1000000000000"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void balanceOf() {
        try {
            Map<String, String> properties = new HashMap<>();
            properties.put("address", "cee3d4755e47055b530deeba062c5bd0c17eb00f");

            JsonObject queryObj = ContractQry.createQuery(branchId.toString(),
                    "balanceOf", ContractQry.createParams(properties));
            JsonParser jsonParser = new JsonParser();
            JsonObject jsonObject = (JsonObject) jsonParser.parse(
                    contractApi.query(queryObj.toString()));
            BigDecimal result = new BigDecimal(jsonObject.get("result").getAsString());

            assertEquals(result, new BigDecimal("998000000000"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void allowance() {
        try {
            Map<String, String> properties = new HashMap<>();
            properties.put("owner", "cee3d4755e47055b530deeba062c5bd0c17eb00f");
            properties.put("spender", "1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e");

            JsonObject queryObj = ContractQry.createQuery(branchId.toString(),
                    "allowance", ContractQry.createParams(properties));
            JsonParser jsonParser = new JsonParser();
            JsonObject jsonObject = (JsonObject) jsonParser.parse(
                    contractApi.query(queryObj.toString()));
            BigDecimal result = new BigDecimal(jsonObject.get("result").getAsString());

            assertEquals(result, BigDecimal.ZERO);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void specification() {
        try {
            JsonObject queryObj = ContractQry.createQuery(branchId.toString(),
                    "specification", ContractQry.createParams(new HashMap<>()));
            JsonParser jsonParser = new JsonParser();
            JsonObject jsonObject = (JsonObject) jsonParser.parse(
                    contractApi.query(queryObj.toString()));
            List<String> methods =
                    Collections.singletonList(jsonObject.get("result").getAsString());

            assertThat(methods.size()).isNotZero();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void transfer() {
        try {
            JsonArray params = ContractTx.createTransferBody(
                    "1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e", new BigDecimal("1000"));
            TransactionHusk tx =
                    ContractTx.createTx(TestUtils.wallet(), branchId, params);

            txApi.sendTransaction(TransactionDto.createBy(tx));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void approve() {
        try {
            JsonArray params = ContractTx.createApproveBody(
                    "1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e", new BigDecimal("1000"));
            TransactionHusk tx =
                    ContractTx.createTx(TestUtils.wallet(), branchId, params);
            txApi.sendTransaction(TransactionDto.createBy(tx));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void transferFrom() {
        try {
            JsonArray params = ContractTx.createTransferFromBody(
                    "cee3d4755e47055b530deeba062c5bd0c17eb00f",
                    "1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e",
                    new BigDecimal("1000"));
            TransactionHusk tx =
                    ContractTx.createTx(TestUtils.wallet(), branchId, params);
            txApi.sendTransaction(TransactionDto.createBy(tx));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}