/*
 * Copyright 2019 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.node.e2e;

import com.google.gson.JsonObject;
import io.yggdrash.ContractTestUtils;
import io.yggdrash.TestConstants;
import io.yggdrash.core.blockchain.BlockChain;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.blockchain.TransactionBuilder;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.gateway.dto.TransactionDto;
import io.yggdrash.node.ContractDemoClientUtils;
import io.yggdrash.node.YggdrashNodeApp;
import io.yggdrash.node.api.BlockApi;
import io.yggdrash.node.api.ContractApi;
import io.yggdrash.node.api.ContractApiImplTest;
import io.yggdrash.node.api.JsonRpcConfig;
import io.yggdrash.node.api.TransactionApi;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.math.BigInteger;
import java.util.Map;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = YggdrashNodeApp.class, webEnvironment = RANDOM_PORT,
        properties = {"yggdrash.node.chain.gen=true"})
@ActiveProfiles("debug")
public class YeedContractE2ETest extends TestConstants.SlowTest {

    private static final Logger log = LoggerFactory.getLogger(YeedContractE2ETest.class);

    private static BranchId branchId = TestConstants.yggdrash();
    private static Wallet wallet = ContractDemoClientUtils.getWallet();

    private BlockChain bc;
    private long blockIndex;

    private TransactionApi txJsonRpc;
    private ContractApi contractJsonRpc;
    private BlockApi blockJsonRpc;

    @LocalServerPort
    private int randomServerPort;

    @Autowired
    private BranchGroup branchGroup;

    @BeforeClass
    public static void init() throws Exception {
        // copy contract to yggdrash-node/.yggdrash/contract
        File srcDir = new File("../resources");
        File destDir = new File(".yggdrash");
        FileUtils.copyDirectory(srcDir, destDir);
    }

    @Before
    public void setUp() {
        String server = String.format("http://localhost:%d/api", randomServerPort);
        txJsonRpc = new JsonRpcConfig().proxyOf(server, TransactionApi.class);
        contractJsonRpc = new JsonRpcConfig().proxyOf(server, ContractApi.class);
        blockJsonRpc = new JsonRpcConfig().proxyOf(server, BlockApi.class);

        bc = branchGroup.getBranch(branchId);
    }

    @Test
    public void shouldGetFrontierBalance() {
        // act
        BigInteger balance = balanceOf(wallet.getHexAddress());

        // assert
        Assert.assertEquals(new BigInteger("1000000000000000000000"), balance);
    }

    @Test
    public void shouldTransferredYeed() {
        // arrange
        int txSendCount = 700;
        TransactionBuilder builder = new TransactionBuilder();

        // act
        for (int i = 0; i < txSendCount; i++) {
            JsonObject txBody = ContractTestUtils.transferTxBodyJson(TestConstants.TRANSFER_TO, BigInteger.ONE);
            Transaction tx = builder.setTxBody(txBody)
                    .setWallet(wallet)
                    .setBranchId(branchId)
                    .build();
            txJsonRpc.sendTransaction(TransactionDto.createBy(tx));
            log.debug("Send Transaction > Hash =  {}, index = {}", tx.getHash(), i);
        }
        // TODO wait for generating blocks by scheduler (every 10 seconds)

        //Utils.sleep(20000);

        //blockJsonRpc.blockNumber(branchId.toString());

        // assert
        BigInteger frontierExpected = new BigInteger("1000000000000000000000");
        frontierExpected = frontierExpected.subtract(BigInteger.valueOf(txSendCount));

        // TODO assert frontier and transferredAddress
        //Assert.assertEquals(frontierExpected, balanceOf(wallet.getHexAddress()));
        //Assert.assertEquals(BigInteger.valueOf(txSendCount), balanceOf(transferredAddress));
    }

    private BigInteger balanceOf(String address) {
        Map params = ContractApiImplTest.createParams("address", address);
        return (BigInteger) contractJsonRpc.query(branchId.toString(),
                TestConstants.YEED_CONTRACT.toString(), "balanceOf", params);
    }
}
