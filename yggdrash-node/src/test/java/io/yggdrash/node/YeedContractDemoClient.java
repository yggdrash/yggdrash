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

import com.google.gson.JsonObject;
import io.yggdrash.ContractTestUtils;
import io.yggdrash.common.contract.ContractVersion;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.node.api.ContractApi;
import io.yggdrash.node.api.ContractApiImplTest;
import io.yggdrash.node.api.JsonRpcConfig;

import java.util.Map;
import java.util.Scanner;

public class YeedContractDemoClient {
    private static final JsonRpcConfig rpc = new JsonRpcConfig();
    private static final Scanner scan = new Scanner(System.in);

    private static String TRANSFER_TO = ContractDemoClientUtils.TRANSFER_TO;
    private static int TRANSFER_AMOUNT = ContractDemoClientUtils.TRANSFER_AMOUNT;

    private static String TARGET_SERVER;
    private static BranchId yggdrash;
    private static ContractVersion yeedContract;

    private static String lastTxId;

    private static void setUp() throws Exception {
        ContractDemoClientUtils utils = new ContractDemoClientUtils();
        TARGET_SERVER = utils.getTargetServer();
        yggdrash = utils.getYggdrash();
        yeedContract = utils.getYeedContract();
    }

    public static void main(String[] args) throws Exception {
        setUp();

        while (true) {
            run();
        }
    }

    private static void run() {
        System.out.println("============================================================");
        System.out.println("* YGGDRASH BRANCH : " + yggdrash.toString());
        System.out.println("* YEED CONTRACT : " + yeedContract.toString());
        System.out.println("============================================================");
        System.out.println("[1] 트랜잭션 조회");
        System.out.println("[2] 트랜잭션 전송");
        System.out.println("[3] 발란스 조회");
        System.out.println("> ");

        String num = scan.nextLine();

        switch (num) {
            case "2":
                sendTx();
                break;
            case "3":
                getBalance();
                break;
            default:
                ContractDemoClientUtils.getTxReceipt(lastTxId);
                break;
        }
    }

    private static void sendTx() {
        String address = getTargetAddress();
        JsonObject txBody = ContractTestUtils.transferTxBodyJson(address, TRANSFER_AMOUNT);
        lastTxId = ContractDemoClientUtils.sendTx(txBody);
    }

    private static String getTargetAddress() {
        System.out.println("전송할 주소를 입력해주세요. (기본값 : " + TRANSFER_TO + ")\n>");
        String address = scan.nextLine();

        return address.length() > 0 ? address : TRANSFER_TO;
    }

    private static void getBalance() {
        String address = getQueryAddress();
        Map params = ContractApiImplTest.createParams("address", address);

        rpc.proxyOf(TARGET_SERVER, ContractApi.class)
                .query(yggdrash.toString(), yeedContract.toString(), "balanceOf", params);
    }

    private static String getQueryAddress() {
        System.out.println("조회할 주소를 입력해주세요. (기본값 : " + TRANSFER_TO + ")\n>");
        String address = scan.nextLine();

        return address.length() > 0 ? address : TRANSFER_TO;
    }

}
