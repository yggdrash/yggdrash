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

package io.yggdrash.core.contract;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.TestUtils;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.core.store.StateStore;
import io.yggdrash.core.store.TransactionReceiptStore;
import io.yggdrash.core.wallet.Wallet;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.InvalidCipherTextException;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class RuntimeTest {
    private static final Logger log = LoggerFactory.getLogger(RuntimeTest.class);
    private final TransactionReceiptStore txReceiptStore = new TransactionReceiptStore();
    private final Contract<CoinStateTable> coinContract = new CoinContract();
    private final Contract<StemBranch> stemContract = new StemContract();
    private io.yggdrash.core.contract.Runtime<StemBranch> stemRuntime;
    private io.yggdrash.core.contract.Runtime<CoinStateTable> yeedRuntime;
    private Wallet wallet;
    private BranchId branchId;

    @Before
    public void setUp() throws IOException, InvalidCipherTextException {
        stemRuntime = new io.yggdrash.core.contract.Runtime<>(new StateStore<>(), txReceiptStore);
        yeedRuntime = new io.yggdrash.core.contract.Runtime<>(new StateStore<>(), txReceiptStore);
        wallet = new Wallet();
    }

    @Test
    public void invokeFromYeedTest() {
        TransactionHusk tx =
                ContractTx.createTx(TestUtils.YEED, wallet, TestUtils.TRANSFER_TO, 100);
        yeedRuntime.invoke(coinContract, tx);
    }

    @Test
    public void invokeFromStemTest() {
        JsonObject branch = TestUtils.createSampleBranchJson();
        branchId = BranchId.of(branch);

        TransactionHusk tx = ContractTx.createStemTx(wallet, branch, "create");
        stemRuntime.invoke(stemContract, tx);

        String description = "hello world!";
        JsonObject updatedBranch = TestUtils.createSampleBranchJson(description);

        tx = ContractTx.createStemTx(wallet, updatedBranch, "update");
        stemRuntime.invoke(stemContract, tx);
    }

    @Test
    public void queryToYeedTest() throws Exception {
        JsonArray params = new JsonArray();
        JsonObject param = new JsonObject();
        param.addProperty("address", "0xe1980adeafbb9ac6c9be60955484ab1547ab0b76");
        params.add(param);

        assertThat(yeedRuntime.query(coinContract,
                TestUtils.createQuery("balanceOf", params))).isNotNull();
    }

    @Test
    public void queryToStemTest() throws Exception {
        invokeFromStemTest();

        JsonArray params = new JsonArray();
        JsonObject param = new JsonObject();
        param.addProperty("branchId", branchId.toString());
        params.add(param);

        assertThat(stemRuntime.query(stemContract,
                TestUtils.createQuery("getCurrentVersion", params))).isNotNull();
        log.debug("[getCurrentVersion] res => " + stemRuntime.query(stemContract,
                TestUtils.createQuery("getCurrentVersion", params)));

        assertThat(stemRuntime.query(stemContract,
                TestUtils.createQuery("getContractHistory", params))).isNotNull();
        log.debug("[getVersionHistory] res => " + stemRuntime.query(stemContract,
                TestUtils.createQuery("getContractHistory", params)));

        assertThat(stemRuntime.query(stemContract,
                TestUtils.createQuery("getAllBranchId", new JsonArray()))).isNotNull();
        log.debug("[getAllBranchId] res => " + stemRuntime.query(stemContract,
                TestUtils.createQuery("getAllBranchId", params)));

        params.remove(0);
        param.remove("branchId");
        param.addProperty("key", "type");
        param.addProperty("value", "immunity");
        params.add(param);
        assertThat(stemRuntime.query(stemContract, TestUtils.createQuery("search", params)))
                .isNotNull();
        log.debug("[Search | type | immunity] res => "
                + stemRuntime.query(stemContract, TestUtils.createQuery("search", params)));

        param.addProperty("key", "name");
        param.addProperty("value", "TEST1");
        assertThat(stemRuntime.query(stemContract, TestUtils.createQuery("search", params)))
                .isNotNull();
        log.debug("[Search | name | TEST1] res => "
                + stemRuntime.query(stemContract, TestUtils.createQuery("search", params)));
    }
}
