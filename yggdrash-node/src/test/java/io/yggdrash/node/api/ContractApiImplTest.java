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
import java.util.*;

import static io.yggdrash.node.api.JsonRpcConfig.CONTRACT_API;
import static io.yggdrash.node.api.JsonRpcConfig.TX_API;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

public class ContractApiImplTest {
    private static Branch branch;
    private static BranchId branchId;
    private static BranchId stem = TestUtils.STEM;
    private static BranchId yeed = TestUtils.YEED;


    @Before
    public void setUp() {
        beforeStemTest();
    }

    private static void beforeStemTest() {
        branch = Branch.of(TestUtils.createSampleBranchJson());
        branchId = branch.getBranchId();

        try {
            TransactionHusk tx =
                    ContractTx.createStemTx(TestUtils.wallet(), branch.getJson(), "create");
            TX_API.sendTransaction(TransactionDto.createBy(tx));
            Thread.sleep(10000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void contractApiIsNotNull() {
        assertThat(CONTRACT_API).isNotNull();
    }

    @Test
    public void TransactionApiIsNotNull() {
        assertThat(TX_API).isNotNull();
    }

    /* StemContract Test */
    @Test
    public void update() {
        try {
            String description = "hello world!";
            JsonObject updatedBranch = TestUtils.createSampleBranchJson(description);

            TransactionHusk tx =
                    ContractTx.createStemTx(TestUtils.wallet(), updatedBranch, "update");
            TX_API.sendTransaction(TransactionDto.createBy(tx));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void search() {
        try {
            Map<String, String> properties = new LinkedHashMap<>();
            properties.put("key", "symbol");
            properties.put("value", "STEM");
            JsonObject queryObj = ContractQry.createQuery(stem.toString(),
                    "search", ContractQry.createParams(properties));

            CONTRACT_API.query(queryObj.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void view() {
        try {
            Map<String, String> properties = new LinkedHashMap<>();
            properties.put("branchId", branchId.toString());
            JsonObject queryObj = ContractQry.createQuery(stem.toString(),
                    "view", ContractQry.createParams(properties));

            CONTRACT_API.query(queryObj.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void getCurrentVersion() {
        try {
            Map<String, String> properties = new LinkedHashMap<>();
            properties.put("branchId", branchId.toString());
            JsonObject queryObj = ContractQry.createQuery(stem.toString(),
                    "getcurrentversion", ContractQry.createParams(properties));

            CONTRACT_API.query(queryObj.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void getVersionHistory() {
        try {
            Map<String, String> properties = new LinkedHashMap<>();
            properties.put("branchId", branchId.toString());
            JsonObject queryObj = ContractQry.createQuery(stem.toString(),
                    "getversionhistory", ContractQry.createParams(properties));

            CONTRACT_API.query(queryObj.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void getAllBranchId() {
        try {
            JsonObject queryObj = ContractQry.createQuery(stem.toString(),
                    "getallbranchid", new JsonArray());
            CONTRACT_API.query(queryObj.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* CoinContract Test */
    @Test
    public void totalSupply() {
        try {
            JsonObject queryObj = ContractQry.createQuery(yeed.toString(),
                    "totalSupply", ContractQry.createParams(new HashMap<>()));
            JsonParser jsonParser = new JsonParser();
            JsonObject jsonObject = (JsonObject) jsonParser.parse(
                    CONTRACT_API.query(queryObj.toString()));
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

            JsonObject queryObj = ContractQry.createQuery(yeed.toString(),
                    "balanceOf", ContractQry.createParams(properties));
            JsonParser jsonParser = new JsonParser();
            JsonObject jsonObject = (JsonObject) jsonParser.parse(
                    CONTRACT_API.query(queryObj.toString()));
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

            JsonObject queryObj = ContractQry.createQuery(yeed.toString(),
                    "allowance", ContractQry.createParams(properties));
            JsonParser jsonParser = new JsonParser();
            JsonObject jsonObject = (JsonObject) jsonParser.parse(
                    CONTRACT_API.query(queryObj.toString()));
            BigDecimal result = new BigDecimal(jsonObject.get("result").getAsString());

            assertEquals(result, BigDecimal.ZERO);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void specification() {
        try {
            JsonObject queryObj = ContractQry.createQuery(yeed.toString(),
                    "specification", ContractQry.createParams(new HashMap<>()));
            JsonParser jsonParser = new JsonParser();
            JsonObject jsonObject = (JsonObject) jsonParser.parse(
                    CONTRACT_API.query(queryObj.toString()));
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
                    ContractTx.createTx(TestUtils.wallet(), yeed, params);

            TX_API.sendTransaction(TransactionDto.createBy(tx));
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
                    ContractTx.createTx(TestUtils.wallet(), yeed, params);
            TX_API.sendTransaction(TransactionDto.createBy(tx));
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
                    ContractTx.createTx(TestUtils.wallet(), yeed, params);
            TX_API.sendTransaction(TransactionDto.createBy(tx));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}