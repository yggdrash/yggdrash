package io.yggdrash.node;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.yggdrash.TestUtils;
import io.yggdrash.core.BranchId;
import io.yggdrash.core.TransactionHusk;
import io.yggdrash.core.account.Address;
import io.yggdrash.core.account.Wallet;
import io.yggdrash.core.contract.ContractQry;
import io.yggdrash.node.api.JsonRpcConfig;
import io.yggdrash.node.controller.TransactionDto;
import org.apache.commons.codec.binary.Hex;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static io.yggdrash.core.contract.ContractTx.createBranch;
import static io.yggdrash.core.contract.ContractTx.createStemTxByBranch;
import static io.yggdrash.core.contract.ContractTx.createYeedTx;

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
        System.out.print("[1] 트랜잭션 전송\n[2] 트랜잭션 조회\n[3] 브랜치 수정\n[4] 브랜치 조회\n[5] 발란스 조회\n[6] 종료\n>");

        String num = scan.nextLine();

        switch (num) {
            case "2" :
                txReceipt();
                break;
            case "3" :
                update();
                break;
            case "4" :
                view();
                break;
            case "5" :
                balance();
                break;
            case "6" :
                System.exit(0);
                break;
            default :
                sendTx();
                break;
        }
    }

    private static void sendTx() throws Exception {
        System.out.print("[1] STEM  [2] YEED\n> ");
        if (scan.nextLine().equals("2")) {
            sendYeedTx();
        } else {
            sendStemTx();
        }
    }

    private static void sendStemTx() throws Exception {
        System.out.print("사용할 .json 파일명을 입력하세요 (기본값: yeed.seed.json)\n> ");
        String json = scan.nextLine();

        if ("".equals(json)) {
            json = "yeed.seed.json";
        }

        JsonObject branch;

        if (!json.contains("seed")) {
            branch = getBranchFile(json);
        } else {
            JsonObject seed = getSeedFile(json);
            branch = createBranch(seed, wallet.getHexAddress());
        }

        sendStemTx(branch, "create");
    }

    private static void sendStemTx(JsonObject branch, String method) {
        int times = getSendTimes();
        String serverAddress = getServerAddress();
        for (int i = 0; i < times; i++) {
            TransactionHusk tx = createStemTxByBranch(wallet, branch, method);
            rpc.transactionApi(serverAddress).sendTransaction(TransactionDto.createBy(tx));
        }
    }

    private static void sendYeedTx() throws Exception {
        System.out.println("전송할 주소를 입력해주세요 (기본값 : " + TRANSFER_TO + ")");
        System.out.println(">");

        String addressHex = scan.nextLine();
        addressHex = addressHex.length() > 0 ? addressHex : TRANSFER_TO;
        Address address = new Address(Hex.decodeHex(addressHex));

        sendYeedTx(address, TRANSFER_AMOUNT);
    }

    private static void sendYeedTx(Address address, int amount) {
        int times = getSendTimes();
        String serverAddress = getServerAddress();
        for (int i = 0; i < times; i++) {
            TransactionHusk tx = createYeedTx(wallet, address, amount);
            rpc.transactionApi(serverAddress).sendTransaction(TransactionDto.createBy(tx));
        }
    }

    private static void view() throws Exception {
        String branchId = getBranchId();
        JsonArray params = ContractQry.createParams("branchId", branchId);
        JsonObject qry = ContractQry.createQuery(BranchId.STEM, "view", params);

        String serverAddress = getServerAddress();
        rpc.contractApi(serverAddress).query(qry.toString());
    }

    private static void update() throws Exception {
        System.out.println("수정할 .json 파일명을 입력하세요 (기본값: yeed.json)\n>");
        String json = scan.nextLine();
        if ("".equals(json)) {
            json = "yeed.json";
        }
        JsonObject branch = getBranchFile(json);

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

    private static void balance() {
        System.out.println("조회할 주소를 적어주세요\n>");
        JsonObject qry = ContractQry.createQuery(BranchId.YEED,
                "balanceOf",
                ContractQry.createParams("address", scan.nextLine()));

        String serverAddress = getServerAddress();
        rpc.accountApi(serverAddress).balanceOf(qry.toString());
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
        System.out.println("조회할 트랜잭션의 브랜치 아이디 : [1] STEM [2] YEED [3] etc\n>");

        String branchId = scan.nextLine();
        switch (branchId) {
            case "1":
                return BranchId.stem().toString();
            case "2":
                return BranchId.yeed().toString();
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

    private static JsonObject getSeedFile(String seed) throws Exception {
        String seedPath = String.format("classpath:/seed/%s", seed);
        ResourceLoader resourceLoader = new DefaultResourceLoader();
        Resource resource = resourceLoader.getResource(seedPath);
        JsonParser jsonParser = new JsonParser();

        return (JsonObject) jsonParser.parse(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));
    }

    private static JsonObject getBranchFile(String fileName) throws FileNotFoundException {
        String userDir = System.getProperty("user.dir");
        userDir += "/yggdrash-node/src/test/resources/branch/%s";

        JsonParser jsonParser = new JsonParser();

        return (JsonObject) jsonParser.parse(
                new FileReader(String.format(userDir, fileName)));
    }
}
