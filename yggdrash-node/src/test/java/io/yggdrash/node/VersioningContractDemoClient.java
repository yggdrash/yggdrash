/*
 * Copyright 2019 Akashic Foundation
 * Licensed under the Apache License, Version 2.0 (the “License”);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an “AS IS” BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package io.yggdrash.node;

import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.ContractTestUtils;
import io.yggdrash.common.contract.ContractVersion;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.blockchain.osgi.ContractConstants;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.node.api.ContractApi;
import io.yggdrash.node.api.ContractApiImplTest;
import io.yggdrash.node.api.JsonRpcConfig;

import java.util.Map;
import java.util.Scanner;

public class VersioningContractDemoClient {

    private static final JsonRpcConfig rpc = new JsonRpcConfig();
    private static final Scanner scan = new Scanner(System.in);

    private static String TARGET_SERVER;
    private static BranchId yggdrash;

    private static Wallet valWallet1; // proposer
    private static Wallet valWallet2;
    private static Wallet valWallet3;
    private static Wallet valWallet4;

    private static ContractVersion versioningContract;

    private static String lastTxId = "";
    private static String proposalTxId = "";
    private static String testCoinContractVersion = "8c65bc05e107aab9ceaa872bbbb2d96d57811de4"; //coinContract
    private static String testStemContractVersion = "3a0b7e2efc5e7d5eff4c18746a3fe56c952493d3"; //stemContract 2.0.0

    private static void setUp() throws Exception {
        ContractDemoClientUtils utils = new ContractDemoClientUtils();
        TARGET_SERVER = utils.getTargetServer();
        yggdrash = utils.getYggdrash();

        versioningContract = ContractConstants.VERSIONING_CONTRACT;

        valWallet1 = ContractTestUtils.createTestWallet("77283a04b3410fe21ba5ed04c7bd3ba89e70b78c.json");
        valWallet2 = ContractTestUtils.createTestWallet("047269a50640ed2b0d45d461488c13abad1e0fac.json");
        valWallet3 = ContractTestUtils.createTestWallet("2ee2eb80c93d031147c21ba8e2e0f0f4a33f5312.json");
        valWallet4 = ContractTestUtils.createTestWallet("51e2128e8deb622c2ec6dc38f9d895f0be044eb4.json");
    }

    public static void main(String[] args) throws Exception {
        setUp();

        while (true) {
            run();
        }
    }

    private static void run() throws Exception {
        System.out.println("================================================================");
        System.out.println("* YGGDRASH BRANCH : " + yggdrash.toString());
        System.out.println("* VERSIONING CONTRACT : " + versioningContract.toString());
        System.out.println("----------------------------------------------------------------");
        System.out.println("* VALIDATOR-1 : " + valWallet1.getHexAddress() + " (PROPOSER)");
        System.out.println("* VALIDATOR-2 : " + valWallet2.getHexAddress());
        System.out.println("* VALIDATOR-3 : " + valWallet3.getHexAddress());
        System.out.println("* VALIDATOR-4 : " + valWallet4.getHexAddress());
        System.out.println("================================================================");
        System.out.println("[1] 트랜잭션 조회");
        System.out.println("[2] 컨트랙트 제안");
        System.out.println("[3] 컨트랙트 투표");
        System.out.println("[4] 컨트랙트 제안서 조회");
        System.out.println(">");

        String num = scan.nextLine();

        switch (num) {
            case "2":
                sendProposeTx();
                break;
            case "3":
                sendVoteTx();
                break;
            case "4":
                getProposalStatus();
                break;
            default:
                getTransactionReceipt();
                break;
        }
    }

    private static void getTransactionReceipt() {
        if (lastTxId.isEmpty()) {
            System.out.println("Last Transaction Id is empty");
        } else {
            ContractDemoClientUtils.getTxReceipt(lastTxId);
        }
    }

    private static void sendProposeTx() {
        System.out.println("제안한 Contract Version 선택 : [1] CoinContract(1.0.0)  [2] StemContract(2.0.0");
        String num = scan.nextLine();
        String contractVersion = num.equals("2") ? testStemContractVersion : testCoinContractVersion;
        Transaction proposeTx = BlockChainTestUtils.createContractProposeTx(valWallet1, contractVersion, "activate");
        lastTxId = ContractDemoClientUtils.sendTx(proposeTx);
        proposalTxId = lastTxId;
    }

    private static void sendVoteTx() {
        System.out.println("투표할 Validator 선택 : [1] VALIDATOR-2  [2] VALIDATOR-3 [3] VALIDATOR-4");
        Wallet wallet;
        String num = scan.nextLine();
        switch (num) {
            case "2":
                wallet = valWallet3;
                break;
            case "3":
                wallet = valWallet4;
                break;
            default:
                wallet = valWallet2;
                break;

        }
        Transaction voteTx = BlockChainTestUtils.createContractVoteTx(wallet, proposalTxId, true);
        lastTxId = ContractDemoClientUtils.sendTx(voteTx);
    }

    private static void getProposalStatus() {
        Map params = ContractApiImplTest.createParams("txId", proposalTxId);
        rpc.proxyOf(TARGET_SERVER, ContractApi.class)
                .query(yggdrash.toString(), versioningContract.toString(), "proposalStatus", params);
    }

}
