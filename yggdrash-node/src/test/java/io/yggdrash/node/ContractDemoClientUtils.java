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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.yggdrash.common.contract.ContractVersion;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.blockchain.TransactionBuilder;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.gateway.dto.BranchDto;
import io.yggdrash.gateway.dto.TransactionDto;
import io.yggdrash.gateway.dto.TransactionReceiptDto;
import io.yggdrash.gateway.dto.TransactionResponseDto;
import io.yggdrash.node.api.BranchApi;
import io.yggdrash.node.api.JsonRpcConfig;
import io.yggdrash.node.api.TransactionApi;

import java.util.Map;
import java.util.Optional;
import java.util.Scanner;

class ContractDemoClientUtils {

    static final String TRANSFER_TO = "1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e";
    static final int TRANSFER_AMOUNT = 1;
    private static final String SERVER_PROD = "10.10.10.100";
    private static final String SERVER_STG = "10.10.20.100";

    private static final JsonRpcConfig rpc = new JsonRpcConfig();
    private static final Scanner scan = new Scanner(System.in);

    private static String TARGET_SERVER;
    private static BranchId yggdrash;
    private static ContractVersion yeedContract;
    private static ContractVersion stemContract;

    private static Wallet wallet;

    ContractDemoClientUtils() throws Exception {
        setServerAddress();
        setBranchAndContract();
        setWallet();
    }

    String getTargetServer() {
        return TARGET_SERVER;
    }

    BranchId getYggdrash() {
        return yggdrash;
    }

    ContractVersion getYeedContract() {
        return yeedContract;
    }

    ContractVersion getStemContract() {
        return stemContract;
    }

    Wallet getWallet() {
        return wallet;
    }

    private void setServerAddress() {
        System.out.println(String.format("전송할 노드 : [1] 로컬 [2] 스테이지(%s) [3] 운영(%s) [4] 직접 입력\n>",
                SERVER_STG, SERVER_PROD));

        String num = scan.nextLine();

        switch (num) {
            case "2":
                TARGET_SERVER = SERVER_STG;
                break;
            case "3":
                TARGET_SERVER = SERVER_PROD;
                break;
            case "4":
                System.out.println("서버 주소 : ");
                TARGET_SERVER = scan.nextLine();
                break;
            default:
                TARGET_SERVER = "localhost";
                break;
        }
    }

    private void setBranchAndContract() {
        Map<String, BranchDto> branches = rpc.proxyOf(TARGET_SERVER, BranchApi.class)
                .getBranches();

        Optional<Map.Entry<String, BranchDto>> branch = branches.entrySet()
                .stream()
                .filter(ent -> "YGGDRASH".equals(ent.getValue().name))
                .findFirst();

        if (branch.isPresent()) {
            yggdrash = BranchId.of(branch.get().getKey());
            BranchDto branchDto = branch.get().getValue();
            branchDto.contracts.forEach(contract -> {
                if ("STEM".equals(contract.get("name"))) {
                    //stemContract = ContractVersion.ofNonHex((String) contract.get("contractVersion")); //Todo test
                    stemContract = ContractVersion.of((String) contract.get("contractVersion"));
                } else if ("YEED".equals(contract.get("name"))) {
                    yeedContract = ContractVersion.of((String) contract.get("contractVersion"));
                }
            });
        }
    }

    private void setWallet() throws Exception {
        String testWalletFile = NodeContractDemoClient.class.getClassLoader()
                .getResource("keys/101167aaf090581b91c08480f6e559acdd9a3ddd.json")
                .getFile();
        String password = "Aa1234567890!";

        wallet = new Wallet(testWalletFile, password);
    }

    static void getTxReceipt(String lastTxId) {
        String txId = getTxId(lastTxId);
        TransactionReceiptDto txrDto = rpc.proxyOf(TARGET_SERVER, TransactionApi.class)
                .getTransactionReceipt(yggdrash.toString(), txId);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String prettyJsonString = gson.toJson(txrDto);
        System.out.println(prettyJsonString);
    }

    private static String getTxId(String lastTxId) {
        System.out.println("조회할 트랜잭션 해시를 적어주세요. (기본값 : " + lastTxId + ")\n>");
        String txHash = scan.nextLine();

        return "".equals(txHash) ? lastTxId : txHash;
    }

    static String sendTx(JsonObject txBody) {
        int times = getSendTimes();
        String lastTxId = "";

        for (int i = 0; i < times; i++) {
            TransactionBuilder txBuilder = new TransactionBuilder();
            Transaction tx = txBuilder.setWallet(wallet)
                    .setBranchId(yggdrash)
                    .setTxBody(txBody)
                    .build();

            TransactionDto txDto = TransactionDto.createBy(tx);
            TransactionResponseDto res = rpc.proxyOf(TARGET_SERVER, TransactionApi.class).sendTransaction(txDto);
            lastTxId = res.txHash;
        }
        return lastTxId;
    }

    private static int getSendTimes() {
        System.out.print("전송할 횟수를 입력하세요. (기본값 : 1)\n> ");
        String tmpTimes = scan.nextLine();

        return "".equals(tmpTimes) ? 1 : Integer.valueOf(tmpTimes);
    }
}
