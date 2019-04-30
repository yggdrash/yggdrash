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

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.yggdrash.ContractTestUtils;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.common.contract.ContractVersion;
import io.yggdrash.common.crypto.HexUtil;
import io.yggdrash.common.util.TimeUtils;
import io.yggdrash.common.utils.FileUtil;
import io.yggdrash.common.utils.JsonUtil;
import io.yggdrash.core.blockchain.Branch;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.genesis.BranchLoader;
import io.yggdrash.core.exception.NonExistObjectException;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.node.api.BranchApi;
import io.yggdrash.node.api.ContractApi;
import io.yggdrash.node.api.ContractApiImplTest;
import io.yggdrash.node.api.JsonRpcConfig;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;
import java.util.Scanner;

import static io.yggdrash.common.config.Constants.BRANCH_ID;
import static io.yggdrash.common.config.Constants.CONTRACT_VERSION;

public class StemContractDemoClient {
    private static final JsonRpcConfig rpc = new JsonRpcConfig();
    private static final Scanner scan = new Scanner(System.in);

    private static String TARGET_SERVER;
    private static BranchId yggdrash;
    private static ContractVersion stemContract;
    private static Wallet wallet;

    private static String lastTxId;

    private static void setUp() throws Exception {
        ContractDemoClientUtils utils = new ContractDemoClientUtils();
        TARGET_SERVER = utils.getTargetServer();
        yggdrash = utils.getYggdrash();
        stemContract = utils.getStemContract();
        wallet = utils.getWallet();
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
        System.out.println("[4] 브랜치 목록");
        System.out.println("[5] 브랜치 조회");
        System.out.println(">");

        String num = scan.nextLine();

        switch (num) {
            case "2":
                deployBranch();
                break;
            case "3":
                updateBranch();
                break;
            case "4":
                getBranches();
                break;
            case "5":
                viewBranch();
                break;
            default:
                ContractDemoClientUtils.getTxReceipt(lastTxId);
                break;
        }
    }

    private static void getBranches() {
        rpc.proxyOf(TARGET_SERVER, BranchApi.class).getBranches();
    }

    private static void viewBranch() {
        Map params = ContractApiImplTest.createParams(BRANCH_ID, yggdrash.toString());

        rpc.proxyOf(TARGET_SERVER, ContractApi.class).query(yggdrash.toString(),
                stemContract.toString(), "view", params);
    }

    private static void deployBranch() throws Exception {
        JsonObject json = getSeedJson();
        System.out.print("Contract Id를 입력하세요.\n> ");
        String contractId = scan.nextLine();

        if ("".equals(contractId)) {
            contractId = json.get(CONTRACT_VERSION).getAsString();
        }
        json.addProperty(CONTRACT_VERSION, contractId);

        ContractTestUtils.signBranch(wallet, json);
        Branch branch = Branch.of(json);
        saveBranchAsFile(branch);
    }

    private static JsonObject getSeedJson() {
        System.out.print("사용할 .json 파일명을 입력하세요 (기본값: yeed.seed.json)\n> ");
        String json = scan.nextLine();

        if ("".equals(json)) {
            json = "yeed.seed.json";
        }

        if (!json.contains("seed")) {
            return getJsonObjectFromFile("branch", json);
        } else {
            return getJsonObjectFromFile("seed", json);
        }
    }

    private static JsonObject getJsonObjectFromFile(String dir, String fileName) {
        String seedPath = String.format("classpath:/%s/%s", dir, fileName);
        Resource resource = new DefaultResourceLoader().getResource(seedPath);
        try (InputStream is = resource.getInputStream()) {
            Reader json = new InputStreamReader(is, FileUtil.DEFAULT_CHARSET);
            JsonObject jsonObject = JsonUtil.parseJsonObject(json);
            if (!jsonObject.has("timestamp")) {
                long timestamp = TimeUtils.time();
                jsonObject.addProperty("timestamp", HexUtil.toHexString(timestamp));
            }
            return jsonObject;
        } catch (Exception e) {
            throw new NonExistObjectException(seedPath);
        }
    }

    private static void saveBranchAsFile(Branch branch) throws IOException {
        String json = new GsonBuilder().setPrettyPrinting().create().toJson(branch.getJson());
        saveFile(branch.getBranchId(), json);
    }

    private static void saveFile(BranchId branchId, String json)
            throws IOException {
        String branchPath = new DefaultConfig().getBranchPath();
        File branchDir = new File(branchPath, branchId.toString());
        if (!branchDir.exists() && !branchDir.mkdirs()) {
            System.err.println("can't create branch dir at " + branchDir);
            return;
        }
        File file = new File(branchDir, BranchLoader.BRANCH_FILE);
        FileWriter fileWriter = new FileWriter(file); //overwritten

        fileWriter.write(json);
        fileWriter.flush();
        fileWriter.close();
        System.out.println("created at " + file.getAbsolutePath());
    }

    private static void updateBranch() {
        System.out.println("수정할 .json 파일명을 입력하세요 (기본값: yeed.json)\n>");
        String json = scan.nextLine();
        if ("".equals(json)) {
            json = "yeed.json";
        }
        JsonObject branch = getJsonObjectFromFile("branch", json);

        System.out.println("수정할 description 의 내용을 적어주세요\n>");
        branch.addProperty("description", scan.nextLine());

        JsonObject txBody = ContractTestUtils.updateTxBodyJson(branch);
        lastTxId = ContractDemoClientUtils.sendTx(txBody);
    }

}
