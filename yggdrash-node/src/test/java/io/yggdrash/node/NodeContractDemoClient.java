package io.yggdrash.node;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.yggdrash.TestUtils;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.common.util.TimeUtils;
import io.yggdrash.common.util.Utils;
import io.yggdrash.core.blockchain.Branch;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.core.blockchain.genesis.BranchLoader;
import io.yggdrash.core.contract.ContractQry;
import io.yggdrash.core.contract.ContractTx;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.node.api.JsonRpcConfig;
import io.yggdrash.node.api.dto.TransactionDto;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class NodeContractDemoClient {

    private static final JsonRpcConfig rpc = new JsonRpcConfig();
    private static final Wallet wallet = TestUtils.wallet();
    private static final Scanner scan = new Scanner(System.in);

    private static final String SERVER_PROD = "10.10.10.100";
    private static final String SERVER_STG = "10.10.20.100";
    private static final String TRANSFER_TO = "1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e";
    private static final int TRANSFER_AMOUNT = 1;

    public static void main(String[] args) throws Exception {
        while (true) {
            run();
        }
    }

    private static void run() throws Exception {
        System.out.print("===============\n");
        System.out.print("[1] 트랜잭션 전송\n[2] 트랜잭션 조회\n[3] 브랜치 배포\n[4] 브랜치 수정\n"
                + "[5] 브랜치 조회\n[6] 발란스 조회\n[9] 종료\n>");

        String num = scan.nextLine();

        switch (num) {
            case "2":
                txReceipt();
                break;
            case "3":
                deployBranch();
                break;
            case "4":
                update();
                break;
            case "5":
                view();
                break;
            case "6":
                balance();
                break;
            case "9":
                System.exit(0);
                break;
            default:
                sendTx();
                break;
        }
    }

    private static void sendTx() throws Exception {
        System.out.print("[1] STEM  [2] YEED [3] NONE [4] GENERAL\n> ");
        String num = scan.nextLine();

        switch (num) {
            case "2":
                sendYeedTx();
                break;
            case "3":
                sendNoneTx();
                break;
            case "4":
                sendGeneralTxOrQuery();
                break;
            default:
                sendStemTx();
                break;
        }
    }

    private static void sendStemTx() {
        JsonObject branch = getBranch();
        sendStemTx(branch, "create");
    }

    private static void sendStemTx(JsonObject branch, String method) {
        int times = getSendTimes();
        String serverAddress = getServerAddress();
        for (int i = 0; i < times; i++) {
            TransactionHusk tx = ContractTx.createStemTx(wallet, branch, method);
            rpc.transactionApi(serverAddress).sendTransaction(TransactionDto.createBy(tx));
        }
    }

    private static void sendNoneTx() {
        String branchId = getBranchId();
        int times = getSendTimes();
        String serverAddress = getServerAddress();
        int amount = 1;
        for (int i = 0; i < times; i++) {
            TransactionHusk tx = ContractTx.createTx(BranchId.of(branchId), wallet, "", amount);
            rpc.transactionApi(serverAddress).sendTransaction(TransactionDto.createBy(tx));
        }
    }

    private static void sendGeneralTxOrQuery() throws Exception {
        String branchId = getBranchId();
        String serverAddress = getServerAddress();
        List<String> methodList = specificationOf(branchId, serverAddress);
        System.out.println("\n해당 컨트랙트의 메소드 스펙입니다.");
        methodList.forEach(System.out::println);

        int index = getMethodIndex(methodList);
        String selectedMethod = getMethodName(index, methodList);
        if (methodList.get(index).contains("TransactionReceipt")) {
            TransactionHusk tx = createTx(branchId, selectedMethod);
            System.out.println("tx => " + tx);
            rpc.transactionApi(serverAddress).sendTransaction(TransactionDto.createBy(tx));
        } else {
            JsonObject query = createQueryObj(branchId, selectedMethod);
            System.out.println("query => " + query);
            rpc.contractApi(serverAddress).query(query.toString());
        }
    }

    private static List<String> specificationOf(String branchId, String serverAddress) {
        JsonObject queryObj
                = ContractQry.createQuery(branchId, "specification", new JsonArray());
        List<String> methods = new ArrayList<>();
        try {
            JsonParser jsonParser = new JsonParser();
            JsonObject jsonObject = (JsonObject) jsonParser.parse(
                    rpc.contractApi(serverAddress).query(queryObj.toString()));
            String result = jsonObject.get("result").getAsString();

            methods = Arrays.asList(result.split(","));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return methods;
    }

    private static int getMethodIndex(List<String> methodList) {
        System.out.println("\n실행할 메소드의 번호를 입력하세요.");
        for (int i = 0; i < methodList.size(); i++) {
            System.out.println("[" + i + "] " + getMethodName(i, methodList));
        }
        System.out.println(">");
        String input = scan.nextLine();

        return input.length() > 0 ? Integer.parseInt(input) : 0;
    }

    private static String getMethodName(int index, List<String> methodList) {
        String method = methodList.get(index);
        String[] element = method.split(" ");
        String[] func = element[2].split("\\.");

        return func[5].substring(0, func[5].indexOf("("));
    }

    private static JsonObject createQueryObj(String branchId, String method) {
        System.out.println("\n파라미터의 갯수를 선택해주세요.");
        System.out.println("[0] No parameter {}");
        System.out.println("[1] 1 parameter  {key : value}");
        System.out.println("[2] 2 parameters {key : value, key : value}");
        System.out.println("[3] 직접 입력      {key : value, key : value ...}");
        System.out.println(">");

        JsonObject queryObj;
        Map<String, String> properties = new HashMap<>();
        switch (scan.nextLine()) {
            case "1":
                System.out.println("key => ");
                String key = scan.nextLine();
                System.out.println("value => ");
                String value = scan.nextLine();
                properties.put(key, value);
                break;
            case "2":
                System.out.println("key1 => ");
                String key1 = scan.nextLine();
                System.out.println("value1 => ");
                String value1 = scan.nextLine();
                properties.put(key1, value1);

                System.out.println("key2 => ");
                String key2 = scan.nextLine();
                System.out.println("value2 => ");
                String value2 = scan.nextLine();
                properties.put(key2, value2);
                break;
            case "3":
                try {
                    System.out.println("=> ");
                    ObjectMapper objectMapper = new ObjectMapper();
                    String input = scan.nextLine();
                    properties = objectMapper.readValue(
                            input, new TypeReference<HashMap<String, String>>() {
                            });
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            default:
                break;
        }
        queryObj = ContractQry.createQuery(
                branchId, method, ContractQry.createParams(properties));
        return queryObj;
    }

    private static TransactionHusk createTx(String branchId, String method) {
        System.out.println("파라미터 직접 입력하시겠습니가? 예, {key : value, key : value ...} (Y/N)");
        JsonArray txBody = new JsonArray();
        String from;
        String to;
        BigDecimal amount;

        if (scan.nextLine().equals("Y")) {
            try {
                System.out.println("=> ");
                Map<String, String> properties = new HashMap<>();
                ObjectMapper objectMapper = new ObjectMapper();
                String input = scan.nextLine();
                properties = objectMapper.readValue(
                        input, new TypeReference<HashMap<String, String>>() {
                        });

                txBody = ContractTx.createTxBody(method, properties);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            switch (method) {
                case "approve":
                    System.out.println("spender => ");
                    String spender = scan.nextLine();
                    System.out.println("amount => ");
                    BigDecimal approvedAmount = new BigDecimal(scan.nextLine());

                    txBody = ContractTx.createApproveBody(spender, approvedAmount);
                    break;
                case "transfer":
                    System.out.println("to => ");
                    to = scan.nextLine();
                    System.out.println("amount => ");
                    amount = new BigDecimal(scan.nextLine());

                    txBody = ContractTx.createTransferBody(to, amount);
                    break;
                case "transferfrom":
                    System.out.println("from => ");
                    from = scan.nextLine();
                    System.out.println("to => ");
                    to = scan.nextLine();
                    System.out.println("amount => ");
                    amount = new BigDecimal(scan.nextLine());

                    txBody = ContractTx.createTransferFromBody(from, to, amount);
                    break;
                default:
                    break;
            }
        }
        return ContractTx.createTx(wallet, BranchId.of(branchId), txBody);
    }

    private static void sendYeedTx() {
        System.out.println("전송할 주소를 입력해주세요 (기본값 : " + TRANSFER_TO + ")");
        System.out.println(">");

        String address = scan.nextLine();
        address = address.length() > 0 ? address : TRANSFER_TO;

        sendYeedTx(address, TRANSFER_AMOUNT);
    }

    private static void sendYeedTx(String address, int amount) {
        int times = getSendTimes();
        String serverAddress = getServerAddress();
        for (int i = 0; i < times; i++) {
            JsonArray txBody = ContractTx.createTransferBody(
                    "1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e", new BigDecimal(amount));
            TransactionHusk tx =
                    ContractTx.createTx(wallet, TestUtils.YEED, txBody);

            rpc.transactionApi(serverAddress).sendTransaction(TransactionDto.createBy(tx));
        }
    }

    private static void view() throws Exception {
        String branchId = getBranchId();
        JsonArray params = ContractQry.createParams("branchId", branchId);
        JsonObject qry = ContractQry.createQuery(TestUtils.STEM.toString(), "view", params);

        String serverAddress = getServerAddress();
        rpc.contractApi(serverAddress).query(qry.toString());
    }

    private static void update() {
        System.out.println("수정할 .json 파일명을 입력하세요 (기본값: yeed.json)\n>");
        String json = scan.nextLine();
        if ("".equals(json)) {
            json = "yeed.json";
        }
        JsonObject branch = getJsonObjectFromFile("branch", json);

        System.out.println("수정할 description 의 내용을 적어주세요\n>");
        branch.addProperty("description", scan.nextLine());

        sendStemTx(branch, "update");
    }

    private static void txReceipt() {
        String branchId = getBranchId();

        System.out.println("조회할 트랜잭션 해시를 적어주세요\n>");
        String txHash = scan.nextLine();

        String serverAddress = getServerAddress();
        rpc.transactionApi(serverAddress).getTransactionReceipt(branchId, txHash);
    }

    private static void deployBranch() throws Exception {
        JsonObject json = getBranch();
        System.out.print("Contract Id를 입력하세요\n> ");
        String contractId = scan.nextLine();

        if ("".equals(contractId)) {
            contractId = json.get("contractId").getAsString();
        }
        json.addProperty("contractId", contractId);

        Branch.signBranch(wallet, json);
        Branch branch = Branch.of(json);
        saveBranchAsFile(branch);
    }

    private static void balance() throws Exception {
        System.out.println("조회할 주소를 적어주세요\n>");
        JsonObject qry = ContractQry.createQuery(TestUtils.YEED.toString(),
                "balanceOf", ContractQry.createParams("address", scan.nextLine()));

        String serverAddress = getServerAddress();
        rpc.contractApi(serverAddress).query(qry.toString());
    }

    private static JsonObject getBranch() {
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

    private static String getServerAddress() {
        System.out.println(String.format("전송할 노드 : [1] 로컬 [2] 스테이지(%s) [3] 운영(%s) \n>",
                SERVER_STG, SERVER_PROD));

        String num = scan.nextLine();
        switch (num) {
            case "2":
                return SERVER_STG;
            case "3":
                return SERVER_PROD;
            default:
                return "localhost";
        }
    }

    private static String getBranchId() {
        System.out.println("트랜잭션의 브랜치 아이디 : [1] STEM [2] YEED [3] etc\n>");

        String branchId = scan.nextLine();
        switch (branchId) {
            case "1":
                return TestUtils.STEM.toString();
            case "2":
                return TestUtils.YEED.toString();
            default:
                return branchId;
        }
    }

    private static int getSendTimes() {
        System.out.print("전송할 횟수를 입력하세요 기본값(1)\n> ");
        String times = scan.nextLine();

        if ("".equals(times)) {
            return 1;
        } else {
            return Integer.valueOf(times);
        }
    }

    private static JsonObject getJsonObjectFromFile(String dir, String fileName) {
        String seedPath = String.format("classpath:/%s/%s", dir, fileName);
        Resource resource = new DefaultResourceLoader().getResource(seedPath);
        try (InputStream is = resource.getInputStream()) {
            Reader json = new InputStreamReader(is, StandardCharsets.UTF_8);
            JsonObject jsonObject = Utils.parseJsonObject(json);
            if (!jsonObject.has("timestamp")) {
                jsonObject.addProperty("timestamp", TimeUtils.hexTime());
            }
            return jsonObject;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
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
        if (!branchDir.exists()) {
            branchDir.mkdirs();
        }
        File file = new File(branchDir, BranchLoader.BRANCH_FILE);
        FileWriter fileWriter = new FileWriter(file); //overwritten

        fileWriter.write(json);
        fileWriter.flush();
        fileWriter.close();
        System.out.println("created at " + file.getAbsolutePath());
    }
}

