package io.yggdrash.node;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.ContractTestUtils;
import io.yggdrash.common.config.Constants;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.common.contract.ContractVersion;
import io.yggdrash.common.crypto.HexUtil;
import io.yggdrash.common.util.TimeUtils;
import io.yggdrash.common.utils.FileUtil;
import io.yggdrash.common.utils.JsonUtil;
import io.yggdrash.core.blockchain.Branch;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.TransactionBuilder;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.core.blockchain.genesis.BranchLoader;
import io.yggdrash.core.exception.NonExistObjectException;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.gateway.dto.BranchDto;
import io.yggdrash.gateway.dto.TransactionDto;
import io.yggdrash.gateway.dto.TransactionReceiptDto;
import io.yggdrash.node.api.BranchApi;
import io.yggdrash.node.api.ContractApi;
import io.yggdrash.node.api.ContractApiImplTest;
import io.yggdrash.node.api.JsonRpcConfig;
import io.yggdrash.node.api.TransactionApi;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.yggdrash.common.config.Constants.BRANCH_ID;
import static io.yggdrash.common.config.Constants.CONTRACT_VERSION;

public class NodeContractDemoClient {

    private static final JsonRpcConfig rpc = new JsonRpcConfig();
    private static final Scanner scan = new Scanner(System.in);

    private static final String SERVER_PROD = "10.10.10.100";
    private static final String SERVER_STG = "10.10.20.100";
    private static final String TRANSFER_TO = "1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e";
    private static final int TRANSFER_AMOUNT = 1;

    // TODO load by branch api
    private static String TARGET_SERVER;
    private static BranchId yggdrash;
    private static ContractVersion stemContract;
    private static ContractVersion yeedContract;

    private static String lastTransactionId;
    private static Wallet wallet;


    public static void main(String[] args) throws Exception {
        setServerAddress();
        setBranchAndContract();

        String testWalletFile = NodeContractDemoClient.class.getClassLoader()
                .getResource("keys/101167aaf090581b91c08480f6e559acdd9a3ddd.json")
                .getFile()
                ;

        String password = "Aa1234567890!";

        wallet = new Wallet(testWalletFile, password);


        while (true) {
            run();
        }
    }

    // TODO 신규추가
    // 서버 상태 조회
    // -- ping / pong
    // 브랜치 상태 조회
    // -- api/branch
    // 컨트렉트 상태 조회
    // -- api/contractManager ?


    private static void run() throws Exception {
        System.out.println("===============");
        System.out.println("YGGDRASH BRANCH : " + yggdrash.toString());
        System.out.println("STEM CONTRACT : " + stemContract.toString());
        System.out.println("YEED CONTRACT : " + yeedContract.toString());


        System.out.print("[1] 트랜잭션 전송\n[2] 트랜잭션 조회\n[3] 브랜치 배포\n[4] 브랜치 목록\n"
                        + "[5] 브랜치 수정\n[6] 브랜치 조회\n[7] 발란스 조회\n[9] 종료\n>");

        String num = scan.nextLine();

        switch (num) {
            case "2":
                txReceipt();
                break;
            case "3":
                deployBranch();
                break;
            case "4":
                getBranches();
                break;
            case "5":
                update();
                break;
            case "6":
                view();
                break;
            case "7":
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

    private static void setBranchAndContract() {
        Map<String, BranchDto> branches = rpc.proxyOf(TARGET_SERVER, BranchApi.class)
                .getBranches();

        //BranchDto yggdrash = branches.get("YGGDRASH");
        // find yggdrash branch : BranchId, Branch

        Optional<Map.Entry<String,BranchDto>> branch = branches.entrySet().stream().filter(ent ->
                "YGGDRASH".equals(ent.getValue().name))
                .findFirst();
        if (branch.isPresent()) {
            yggdrash = BranchId.of(branch.get().getKey());
            BranchDto branchDto = branch.get().getValue();
            branchDto.contracts.forEach(contract -> {
                if ("STEM".equals(contract.get("name"))) {
                    stemContract = ContractVersion.ofNonHex((String) contract.get("contractVersion"));
                } else if ("YEED".equals(contract.get("name"))) {
                    yeedContract = ContractVersion.ofNonHex((String) contract.get("contractVersion"));
                }

            });
        }
    }


    private static void sendStemTx() {
        JsonObject branch = getBranch();
        sendStemTx(branch, "create");
    }

    private static void sendStemTx(JsonObject branch, String method) {
        int times = getSendTimes();
        BranchId branchId = BranchId.of(branch);
        for (int i = 0; i < times; i++) {
            TransactionHusk tx = BlockChainTestUtils.createBranchTxHusk(branchId, method, branch);
            sendTransaction(tx);
        }
    }

    private static void sendNoneTx() {
        String branchId = getBranchId();
        int times = getSendTimes();
        int amount = 1;
        for (int i = 0; i < times; i++) {
            JsonArray txBody = ContractTestUtils.transferTxBodyJson("", amount);
            TransactionHusk tx = createTxHusk(BranchId.of(branchId), txBody);
            sendTransaction(tx);
        }
    }

    private static void sendGeneralTxOrQuery() {
        // TODO change Spec
        String branchId = getBranchId();
        List<String> methodList = (List<String>)rpc.proxyOf(TARGET_SERVER, ContractApi.class)
                .query(branchId, stemContract.toString(), "specification", null);
        System.out.println("\n해당 컨트랙트의 메소드 스펙입니다.");
        methodList.forEach(System.out::println);

        int index = getMethodIndex(methodList);
        String selectedMethod = MethodNameParser.parse(methodList.get(index));
        if (methodList.get(index).contains("TransactionReceipt")) {
            TransactionHusk tx = createTx(branchId, selectedMethod);
            System.out.println("tx => " + tx);
            sendTransaction(tx);
        } else {
            Map params = createParams();
            rpc.proxyOf(TARGET_SERVER, ContractApi.class).query(branchId,
                    stemContract.toString(), selectedMethod, params);
        }
    }

    private static int getMethodIndex(List<String> methodList) {
        System.out.println("\n실행할 메소드의 번호를 입력하세요.");
        for (int i = 0; i < methodList.size(); i++) {
            System.out.println("[" + i + "] " + MethodNameParser.parse(methodList.get(i)));
        }
        System.out.println(">");
        String input = scan.nextLine();

        return input.length() > 0 ? Integer.parseInt(input) : 0;
    }

    private static Map createParams() {
        System.out.println("\n파라미터의 갯수를 선택해주세요.");
        System.out.println("[0] No parameter {}");
        System.out.println("[1] 1 parameter  {key : value}");
        System.out.println("[2] 2 parameters {key : value, key : value}");
        System.out.println("[3] 직접 입력      {key : value, key : value ...}");
        System.out.println(">");

        Map<String, String> params = new HashMap<>();
        switch (scan.nextLine()) {
            case "1":
                System.out.println("key => ");
                String key = scan.nextLine();
                System.out.println("value => ");
                String value = scan.nextLine();
                params.put(key, value);
                break;
            case "2":
                System.out.println("key1 => ");
                String key1 = scan.nextLine();
                System.out.println("value1 => ");
                String value1 = scan.nextLine();
                params.put(key1, value1);

                System.out.println("key2 => ");
                String key2 = scan.nextLine();
                System.out.println("value2 => ");
                String value2 = scan.nextLine();
                params.put(key2, value2);
                break;
            case "3":
                try {
                    System.out.println("=> ");
                    JsonObject json = JsonUtil.parseJsonObject(scan.nextLine());
                    params = JsonUtil.convertJsonToMap(json);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            default:
                break;
        }
        System.out.println("params => " + params.toString());
        return params;
    }

    private static TransactionHusk createTx(String branchId, String method) {
        System.out.println("파라미터 직접 입력하시겠습니가? 예, {key : value, key : value ...} (Y/N)");
        JsonArray txBody = null;
        String from;
        String to;
        BigInteger amount;

        if (scan.nextLine().equals("Y")) {
            System.out.println("=> ");
            JsonObject params = JsonUtil.parseJsonObject(scan.nextLine());

            txBody = ContractTestUtils.txBodyJson(Constants.YEED_CONTRACT_VERSION, method, params);
        } else {
            switch (method) {
                case "approve":
                    System.out.println("spender => ");
                    String spender = scan.nextLine();
                    System.out.println("amount => ");
                    BigInteger approvedAmount = new BigInteger(scan.nextLine());

                    txBody = CoinContractTestUtils.createApproveBody(spender, approvedAmount);
                    break;
                case "transfer":
                    System.out.println("to => ");
                    to = scan.nextLine();
                    System.out.println("amount => ");
                    amount = new BigInteger(scan.nextLine());

                    txBody = CoinContractTestUtils.createTransferBody(to, amount);
                    break;
                case "transferFrom":
                    System.out.println("from => ");
                    from = scan.nextLine();
                    System.out.println("to => ");
                    to = scan.nextLine();
                    System.out.println("amount => ");
                    amount = new BigInteger(scan.nextLine());

                    txBody = CoinContractTestUtils.createTransferFromBody(from, to, amount);
                    break;
                default:
                    System.err.println("unknown " + method);
                    createTx(branchId, method);
                    break;
            }
        }
        return createTxHusk(BranchId.of(branchId), txBody);
    }

    private static void sendYeedTx() {
        System.out.println("전송할 주소를 입력해주세요 (기본값 : " + TRANSFER_TO + ")");
        System.out.println(">");

        String address = scan.nextLine();
        address = address.length() > 0 ? address : TRANSFER_TO;

        sendYeedTx(address, TRANSFER_AMOUNT);
    }

    private static void sendYeedTx(String address, long amount) {
        int times = getSendTimes();
        for (int i = 0; i < times; i++) {
            JsonArray txBody = ContractTestUtils.transferTxBodyJson(address, amount);
            TransactionHusk tx = createTxHusk(yggdrash, txBody);
            sendTransaction(tx);
        }
    }

    private static void view() {
        String branchId = getBranchId();
        Map params = ContractApiImplTest.createParams(BRANCH_ID, branchId);

        rpc.proxyOf(TARGET_SERVER, ContractApi.class).query(branchId,
                stemContract.toString(), "view", params);
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
        System.out.println("조회할 트랜잭션 해시를 적어주세요 \n 기본값 : " + lastTransactionId + "\n>");
        String txHash = scan.nextLine();
        if ("".equals(txHash)) {
            txHash = lastTransactionId;
        }
        TransactionReceiptDto txr = rpc.proxyOf(TARGET_SERVER, TransactionApi.class)
                .getTransactionReceipt(branchId, txHash);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String prettyJsonString = gson.toJson(txr);
        System.out.println(prettyJsonString);
    }

    private static void deployBranch() throws Exception {
        JsonObject json = getBranch();
        System.out.print("Contract Id를 입력하세요\n> ");
        String contractId = scan.nextLine();

        if ("".equals(contractId)) {
            contractId = json.get(CONTRACT_VERSION).getAsString();
        }
        json.addProperty(CONTRACT_VERSION, contractId);

        ContractTestUtils.signBranch(wallet, json);
        Branch branch = Branch.of(json);
        saveBranchAsFile(branch);
    }

    private static void getBranches() {
        rpc.proxyOf(TARGET_SERVER, BranchApi.class).getBranches();
    }

    private static void balance() {
        System.out.println("조회할 주소를 적어주세요\n>");
        Map params = ContractApiImplTest.createParams("address", scan.nextLine());

        rpc.proxyOf(TARGET_SERVER, ContractApi.class)
                .query(yggdrash.toString(),yeedContract.toString(), "balanceOf", params);
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

    private static void setServerAddress() {
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
                System.out.println("서버 주소 => ");
                TARGET_SERVER = scan.nextLine();
                break;
            default:
                TARGET_SERVER = "localhost";
                break;
        }
    }

    private static String getBranchId() {

        System.out.println("트랜잭션의 브랜치 아이디 : [1] YGGDRASH [3] etc(직접 입력)\n>");

        String branchId = scan.nextLine();
        switch (branchId) {
            case "1":
                return yggdrash.toString();
            case "3":
                System.out.println("브랜치 아이디 => ");
                return scan.nextLine();
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

    private static TransactionHusk createTxHusk(BranchId branchId, JsonArray txBody) {
        TransactionBuilder builder = new TransactionBuilder();
        return builder.addTransactionBody(txBody)
                .setWallet(wallet)
                .setBranchId(branchId)
                .build();
    }

    static class MethodNameParser {
        static final Pattern p = Pattern.compile(".*\\.([a-zA-Z]*)\\(.*\\)");

        static String parse(String name) {
            Matcher m = p.matcher(name);
            if (m.matches()) {
                return "" + m.group(1);
            }
            return "unknown";
        }
    }

    private static void sendTransaction(TransactionHusk tx) {
        TransactionDto txd = TransactionDto.createBy(tx);
        lastTransactionId = rpc.proxyOf(TARGET_SERVER, TransactionApi.class)
                .sendTransaction(txd);
    }



}

