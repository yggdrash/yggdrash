package io.yggdrash.node;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.yggdrash.TestUtils;
import io.yggdrash.contract.ContractQry;
import io.yggdrash.core.Address;
import io.yggdrash.core.BranchId;
import io.yggdrash.core.TransactionHusk;
import io.yggdrash.core.Wallet;
import io.yggdrash.core.exception.FailedOperationException;
import io.yggdrash.node.api.AccountApi;
import io.yggdrash.node.api.ContractApi;
import io.yggdrash.node.api.JsonRpcConfig;
import io.yggdrash.node.api.TransactionApi;
import io.yggdrash.node.controller.TransactionDto;
import org.apache.commons.codec.binary.Hex;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static io.yggdrash.contract.ContractTx.createBranch;
import static io.yggdrash.contract.ContractTx.createStemTxByBranch;
import static io.yggdrash.contract.ContractTx.createYeedTx;

public class NodeContractDemoClient {

    private static Scanner scan = new Scanner(System.in);
    private static Wallet wallet;
    private static final String server = "10.10.10.100";
    private static final TransactionApi transactionApiLocal = new JsonRpcConfig().transactionApi();
    private static final TransactionApi transactionApiServer = new JsonRpcConfig().transactionApi(server);
    private static final ContractApi contractApiLocal = new JsonRpcConfig().contractApi();
    private static final ContractApi contractApiServer = new JsonRpcConfig().contractApi(server);
    private static final AccountApi accountApiLocal = new JsonRpcConfig().accountApi();
    private static final AccountApi accountApiServer = new JsonRpcConfig().accountApi(server);

    public static void main(String[] args) throws Exception {
        while (true) {
            run();
        }
    }

    private static void run() throws Exception {
        wallet = new Wallet();

        System.out.print("===============\n");
        System.out.print("[1] 트랜잭션 전송\n[2] 트랜잭션 조회\n[3] 브랜치 수정\n[4] 브랜치 조회\n[5] 발란스 조회\n[6] 종료\n>");

        String num = scan.nextLine();

        switch (num) {
            case "1" :
                sendTx();
                break;
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
            System.out.println("전송할 주소를 입력해주세요");
            System.out.println("(기본값 : " + new Address(TestUtils.TRANSFER_TO).toString() + ")");
            System.out.println(">");
            String address = scan.nextLine();
            TransactionHusk tx;
            if (address.length() > 0) {
                tx = createYeedTx(wallet, new Address(Hex.decodeHex(address)), 100);
            } else {
                tx = createYeedTx(wallet, new Address(TestUtils.TRANSFER_TO), 100);
            }
            send(toServer(), tx);

        } else {
            System.out.print("사용할 .json 파일명을 입력하세요 (기본값: yeed.seed.json)\n> ");
            String json = scan.nextLine();

            if ("".equals(json)) {
                json = "yeed.seed.json";
            }

            Boolean toServer = toServer();
            JsonObject branch;

            if (!json.contains("seed")) {
                branch = getBranchFile(json);
            } else {
                JsonObject seed = getSeedFile(json);
                branch = createBranch(seed, wallet.getHexAddress());
            }

            System.out.print("전송할 횟수를 입력하세요 기본값(1)\n> ");
            String times = scan.nextLine();

            if ("".equals(times)) {
                times = "1";
            }

            for (int i = Integer.parseInt(times); i > 0; i--) {
                TransactionHusk tx = createStemTxByBranch(wallet, branch, "create");
                send(toServer, tx);
            }
        }
    }

    private static void view() {
        System.out.println("브랜치 아이디\n>");
        String branchId = scan.nextLine();
        try {
            JsonObject qry = ContractQry.createQuery(BranchId.STEM,
                    "view",
                    ContractQry.createParams("branchId", branchId));

            if (toServer()) {
                contractApiServer.query(qry.toString());
            } else {
                contractApiLocal.query(qry.toString());
            }
        } catch (Exception e) {
            throw new FailedOperationException("[ERR] view failed");
        }
    }

    private static void update() throws Exception {
        System.out.println("수정할 .json 파일명을 입력하세요 (기본값: sample1.json)\n>");
        String json = scan.nextLine();
        if ("".equals(json)) {
            json = "sample1.json";
        }
        JsonObject branch = getBranchFile(json);
        System.out.println("수정할 description 의 내용을 적어주세요\n>");
        branch.addProperty("description", scan.nextLine());
        saveBranchAsFile(json, branch);
        TransactionHusk tx = createStemTxByBranch(wallet, branch, "update");
        send(toServer(), tx);
    }

    private static void txReceipt() {
        String branchId = "";
        System.out.println("조회할 트랜잭션의 브랜치 : [1] STEM [2] YEED [3] etc\n>");
        String num = scan.nextLine();

        if ("1".equals(num)) {
            branchId = "fe7b7c93dd23f78e12ad42650595bc0f874c88f7";
        } else if ("2".equals(num)) {
            branchId = "a08ee962cd8b2bd0edbfee989c1a9f7884d26532";
        } else if ("3".equals(num)) {
            System.out.println("조회할 트랜잭션의 브랜치 아이디를 적어주세요\n>");
            branchId = scan.nextLine();
        }
        System.out.println("조회할 트랜잭션 해시를 적어주세요\n>");
        String txHash = scan.nextLine();

        if (toServer()) {
            transactionApiServer.getTransactionReceipt(branchId, txHash);
        } else {
            transactionApiLocal.getTransactionReceipt(branchId, txHash);
        }
    }

    private static void balance() throws Exception {
        System.out.println("조회할 주소를 적어주세요\n>");
        JsonObject qry = ContractQry.createQuery(BranchId.YEED,
                "balanceOf",
                ContractQry.createParams("address", scan.nextLine()));

        if (toServer()) {
            accountApiServer.balanceOf(qry.toString());
        } else {
            accountApiLocal.balanceOf(qry.toString());
        }
    }

    private static Boolean toServer() {
        System.out.println("전송할 노드 : [1] 로컬 [2] 서버(10.10.10.100)\n>");

        return scan.nextLine().equals("2");
    }

    private static void send(Boolean toServer, TransactionHusk tx) {
        if (toServer) {
            transactionApiServer.sendTransaction(TransactionDto.createBy(tx));
        } else {
            transactionApiLocal.sendTransaction(TransactionDto.createBy(tx));
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

    private static void saveBranchAsFile(String fileName, JsonObject branch) throws IOException {
        String userDir = System.getProperty("user.dir");
        userDir += "/yggdrash-node/src/test/resources/branch/%s";

        File file = new File(String.format(userDir, fileName));
        FileWriter fileWriter = new FileWriter(file); //overwritten

        fileWriter.write(new GsonBuilder().setPrettyPrinting().create().toJson(branch));
        fileWriter.flush();
        fileWriter.close();
    }
}
