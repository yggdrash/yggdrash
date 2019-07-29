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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.ContractTestUtils;
import io.yggdrash.common.contract.ContractVersion;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.gateway.dto.TransactionReceiptDto;
import io.yggdrash.node.api.ContractApi;
import io.yggdrash.node.api.ContractApiImplTest;
import io.yggdrash.node.api.JsonRpcConfig;

import java.math.BigInteger;
import java.util.Map;
import java.util.Scanner;

import static io.yggdrash.common.config.Constants.BRANCH_ID;

public class StemContractDemoClient {
    private static final JsonRpcConfig rpc = new JsonRpcConfig();
    private static final Scanner scan = new Scanner(System.in);

    private static String TARGET_SERVER;
    private static BranchId yggdrash;
    private static ContractVersion stemContract;
    private static ContractVersion yeedContract;
    private static Wallet validatorWallet;

    private static String lastTxId = "";
    private static String lastBranchId = "";

    private static void setUp() throws Exception {
        ContractDemoClientUtils utils = new ContractDemoClientUtils();
        TARGET_SERVER = utils.getTargetServer();
        yggdrash = utils.getYggdrash();
        stemContract = utils.getStemContract();
        yeedContract = utils.getYeedContract();
        validatorWallet = setValidatorWallet();
    }

    private static Wallet setValidatorWallet() throws Exception {
        String validatorWalletFile = StemContractDemoClient.class.getClassLoader()
                .getResource("keys/047269a50640ed2b0d45d461488c13abad1e0fac.json")
                .getFile();
        String password = "Aa1234567890!";

        return new Wallet(validatorWalletFile, password);
    }

    public static void main(String[] args) throws Exception {
        setUp();

        while (true) {
            run();
        }

    }

    private static void run() throws Exception {
        System.out.println("============================================================");
        System.out.println("* YGGDRASH BRANCH : " + yggdrash.toString());
        System.out.println("* STEM CONTRACT : " + stemContract.toString());
        System.out.println("============================================================");
        System.out.println("[1] 트랜잭션 조회");
        System.out.println("[2] 브랜치 배포");
        System.out.println("[3] 브랜치 수정");
        System.out.println("[4] 브랜치 조회");
        System.out.println("[5] 브랜치 메타데이터 조회");
        System.out.println("[6] 브랜치 컨트랙트 조회");
        System.out.println("[7] 브랜치 서비스 수수료 조회");
        System.out.println(">");

        String num = scan.nextLine();

        switch (num) {
            case "2":
                checkBalanceAndTransferYeedToValidator(); // Require yeed to create a branch
                createBranch();
                break;
            case "3":
                checkBalanceAndTransferYeedToValidator(); // Require yeed to update a branch
                updateBranch();
                break;
            case "4":
                getBranch();
                break;
            case "5":
                getBranchMeta();
                break;
            case "6":
                getContract();
                break;
            case "7":
                getFeeState();
                break;
            default:
                setLastCreatedBranch();
                break;
        }
    }

    private static void checkBalanceAndTransferYeedToValidator() {
        getBalance(validatorWallet.getHexAddress());
        System.out.println("Transfer Yeed to Validator : [Y]/[N]");
        String transfer = scan.nextLine();
        if (transfer.equals("Y")) {
            transferYeedToValidator();
        }
    }

    private static void getBalance(String address) {
        Map params = ContractApiImplTest.createParams("address", address);
        rpc.proxyOf(TARGET_SERVER, ContractApi.class)
                .query(yggdrash.toString(), yeedContract.toString(), "balanceOf", params);
    }

    private static void transferYeedToValidator() {
        String to = validatorWallet.getHexAddress();
        JsonObject txBody = ContractTestUtils.transferTxBodyJson(to, BigInteger.valueOf(100000000L));
        lastTxId = ContractDemoClientUtils.sendTx(txBody);
    }

    private static void createBranch() {
        JsonObject branchObj = createParamForCreateBranch(createContractMock().mock);
        JsonObject txBody = ContractTestUtils.createTxBodyJson(branchObj);
        sendTx(txBody);
    }

    private static ContractMock createContractMock() {
        System.out.println("Branch Name (default MOCK) : ");
        String input = scan.nextLine();
        String branchName = input.isEmpty() ? "MOCK" : input;

        System.out.println("Branch Symbol (default MOCK) : ");
        input = scan.nextLine();
        String branchSymbol = input.isEmpty() ? "MOCK" : input;

        System.out.println("Branch Property (default MOCK) : ");
        input = scan.nextLine();
        String branchProperty = input.isEmpty() ? "MOCK" : input;

        System.out.println("Branch Description (default MOCK) : ");
        input = scan.nextLine();
        String branchDescription = input.isEmpty() ? "MOCK" : input;

        System.out.println("Contract Name (default MOCK) : ");
        input = scan.nextLine();
        String contractName = input.isEmpty() ? "MOCK" : input;

        System.out.println("Contract Description (default MOCK) : ");
        input = scan.nextLine();
        String contractDescription = input.isEmpty() ? "MOCK" : input;

        return ContractMock.builder()
                .setName(branchName)
                .setSymbol(branchSymbol)
                .setProperty(branchProperty)
                .setDescription(branchDescription)
                .setContractName(contractName)
                .setContractDescription(contractDescription)
                .build();
    }

    private static void updateBranch() {
        String branchId = scanBranchId();
        JsonObject branchObjToUpdate = new JsonObject();
        System.out.println("Branch Description to Update : ");
        String branchDescription = scan.nextLine();
        branchObjToUpdate.addProperty("description", branchDescription); // Only description can be modified

        JsonObject branchObj = createParamForUpdateBranch(branchId, branchObjToUpdate);
        JsonObject txBody = ContractTestUtils.updateTxBodyJson(branchObj);
        sendTx(txBody);
    }

    private static JsonObject createParamForUpdateBranch(String branchId, JsonObject branchObj) {
        JsonObject param = new JsonObject();
        param.addProperty("branchId", branchId);
        param.add("branch", branchObj);
        param.addProperty("serviceFee", BigInteger.valueOf(100));
        return param;
    }

    private static JsonObject createParamForCreateBranch(JsonObject branchObj) {
        JsonObject param = new JsonObject();
        param.add("branch", branchObj);
        param.addProperty("serviceFee", BigInteger.valueOf(100));
        return param;
    }

    private static void setLastCreatedBranch() {
        try {
            TransactionReceiptDto receipt = ContractDemoClientUtils.getTxReceipt(lastTxId);
            int start = "Branch".length(); // i.e "Branch ef6f16a69326e8b2b5d64f0c9d11a0dad4f3d58b is created"
            receipt.txLog.stream()
                    .filter(log -> log.contains("created"))
                    .forEach(log -> lastBranchId = log.replaceAll(" ", "").substring(start, start + 40));
            System.out.println("LastBranchId = " + lastBranchId); // branchId is null if created is not exists
        } catch (Exception e) {
            System.out.println("트랜잭션 조회 종료 [Y]/[N]");
            if (!scan.nextLine().equals("N")) {
                return;
            }
            setLastCreatedBranch();
        }
    }

    private static void getBranch() {
        Map param = createParamsForQuery();
        Object branch = query("getBranch", param); // return type : class java.util.LinkedHashMap
    }

    private static void getBranchMeta() {
        Map param = createParamsForQuery();
        Object branchMetaData = query("getBranchMeta", param); // return type : class java.util.LinkedHashMap
    }

    private static void getContract() {
        Map param = createParamsForQuery();
        Object contracts = query("getContract", param); // return type : Set<JsonObject>
    }

    private static void getFeeState() {
        Map param = createParamsForQuery();
        Object feeState = query("feeState", param); // return type : String
    }

    private static String scanBranchId() {
        System.out.println(String.format("BranchId : (default %s)", lastBranchId));
        String input = scan.nextLine();
        if (!input.isEmpty()) {
            lastBranchId = input;
            return input;
        } else {
            return lastBranchId;
        }
    }

    private static Map createParamsForQuery() {
        return ContractApiImplTest.createParams(BRANCH_ID, scanBranchId());
    }

    private static Object query(String method, Map param) {
        return rpc.proxyOf(TARGET_SERVER, ContractApi.class)
                .query(yggdrash.toString(), stemContract.toString(), method, param);
    }

    private static void sendTx(JsonObject txBody) {
        lastTxId = ContractDemoClientUtils.sendTx(validatorWallet, txBody);
        System.out.println("lastIndex => " + lastTxId);
    }

    private static class ContractMock {
        JsonObject mock = new JsonObject();

        String name = "MOCK";
        String symbol = "MOCK";
        String property = "MOCK";
        String description = "MOCK";
        String timeStamp = "000001674dc56231";
        JsonObject consensus = new JsonObject();
        String governanceContract = "DpoA";

        JsonArray contracts = new JsonArray();

        JsonObject sampleContract = new JsonObject();
        JsonObject governanceSampleContract = new JsonObject();
        String contractVersion = "178b44b22d8c6d5bb08175fa2fcab15122ca8d1e";
        JsonObject contractInit = new JsonObject();
        String contractDescription = "MOCK";
        String contractName = "MOCK";
        boolean contractIsSystem = false;

        ContractMock setName(String name) {
            this.name = name;
            return this;
        }

        ContractMock setSymbol(String symbol) {
            this.symbol = symbol;
            return this;
        }

        ContractMock setProperty(String property) {
            this.property = property;
            return this;
        }

        ContractMock setDescription(String description) {
            this.description = description;
            return this;
        }

        ContractMock setGovernanceContract(String governance) {
            this.governanceContract = governance;
            return this;
        }

        ContractMock setContractVersion(String contractVersion) {
            this.contractVersion = contractVersion;
            return this;
        }

        ContractMock setContractInit(JsonObject init) {
            this.contractInit = init;
            return this;
        }

        ContractMock setContractDescription(String description) {
            this.contractDescription = description;
            return this;
        }

        ContractMock setContractName(String name) {
            this.contractName = name;
            return this;
        }

        ContractMock setContractIsSystem(boolean isSystem) {
            this.contractIsSystem = isSystem;
            return this;
        }

        ContractMock setTimeStamp(String timeStamp) {
            this.timeStamp = timeStamp;
            return this;
        }

        ContractMock build() {
            createSampleContract();
            createGovernanceSampleContract();
            createContracts();
            createConsensus();
            createMock();
            return this;
        }

        private void createSampleContract() {
            sampleContract.addProperty("contractVersion", contractVersion);
            sampleContract.add("init", contractInit);
            sampleContract.addProperty("description", contractDescription);
            sampleContract.addProperty("name", contractName);
            sampleContract.addProperty("isSystem", contractIsSystem);
        }

        private void createGovernanceSampleContract() {
            JsonObject initObj = new JsonObject();
            JsonArray validators = new JsonArray();
            validators.add(validatorWallet.getHexAddress());
            initObj.add("validators", validators);
            governanceSampleContract.addProperty("contractVersion", "30783a1311b9c68dd3a92596d650ae6914b01658");
            governanceSampleContract.add("init", initObj);
            governanceSampleContract.addProperty("description", "This contract is for a validator.");
            governanceSampleContract.addProperty("name", governanceContract);
            governanceSampleContract.addProperty("isSystem", true);
        }

        private void createContracts() {
            contracts.add(sampleContract);
            contracts.add(governanceSampleContract);
        }

        private void createConsensus() {
            consensus.addProperty("algorithm", "pbft");
            consensus.addProperty("period", "* * * * * *");
        }

        private void createMock() {
            mock.addProperty("name", name);
            mock.addProperty("symbol", symbol);
            mock.addProperty("property", property);
            mock.addProperty("description", description);
            mock.add("contracts", contracts);
            mock.addProperty("timestamp", timeStamp);
            mock.add("consensus", consensus);
            mock.addProperty("governanceContract", governanceContract);
        }

        public static ContractMock builder() {
            return new ContractMock();
        }
    }
}
