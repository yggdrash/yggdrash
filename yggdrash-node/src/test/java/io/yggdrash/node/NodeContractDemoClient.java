package io.yggdrash.node;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.yggdrash.TestUtils;
import io.yggdrash.contract.ContractQry;
import io.yggdrash.core.Address;
import io.yggdrash.core.TransactionHusk;
import io.yggdrash.core.Wallet;
import io.yggdrash.core.exception.FailedOperationException;
import io.yggdrash.node.api.ContractApi;
import io.yggdrash.node.api.JsonRpcConfig;
import io.yggdrash.node.api.TransactionApi;
import io.yggdrash.node.controller.TransactionDto;
import org.apache.commons.codec.binary.Hex;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static io.yggdrash.contract.ContractTx.createStemTx;
import static io.yggdrash.contract.ContractTx.createYeedTx;

public class NodeContractDemoClient {

    private static Scanner scan = new Scanner(System.in);
    private static Wallet wallet;
    private static final ContractApi contractApi = new JsonRpcConfig().contractApi();

    public static void main(String[] args) throws Exception {
        wallet = new Wallet();

        System.out.print("============\n\n");
        System.out.print("[1] 로컬에 트랜잭션 전송(기본값)\n[2] 배포서버(10.10.10.100)[서버 주소] 에 트랜잭션 전송\n[3] 브랜치 아이디로 STEM 에서 조회하기\n> ");
        String num = scan.nextLine();

        switch (num) {
            case "1" :
                sendTx("");
                break;
            case "2" :
                sendTx("10.10.10.100");
                break;
            case "3" :
                System.out.println("브랜치 아이디\n>");
                view(scan.nextLine());
                break;
            default :
                sendTx("");
                break;
        }
    }

    private static void sendTx(String server) throws Exception {
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
            send(tx, server);

        } else {
            System.out.print("사용할 .json 파일명을 입력하세요 (기본값: yeed.seed.json)\n> ");
            String json = scan.nextLine();
            if ("".equals(json)) {
                json = "yeed.seed.json";
            }
            JsonObject seed = getSampleBranch(json);
            System.out.print("전송할 횟수를 입력하세요 기본값(1)\n> ");
            String times = scan.nextLine();

            if ("".equals(times)) {
                times = "1";
            }
            for (int i = Integer.parseInt(times); i > 0; i--) {
                TransactionHusk tx = createStemTx(wallet, seed, "create");
                send(tx, server);
            }
        }
    }

    private static JsonObject getSampleBranch(String path) throws Exception {
        String sampleSeedPath = "classpath:/seed/%s";
        ResourceLoader resourceLoader = new DefaultResourceLoader();
        Resource resource = resourceLoader.getResource(String.format(sampleSeedPath, path));
        JsonParser jsonParser = new JsonParser();

        return (JsonObject) jsonParser.parse(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));
    }

    private static void send(TransactionHusk tx, String server) {
        TransactionApi txApi;
        if (server.contains(".")) {
            txApi = new JsonRpcConfig().transactionApi(server);
        } else {
            txApi = new JsonRpcConfig().transactionApi();
        }
        txApi.sendTransaction(TransactionDto.createBy(tx));
    }

    private static void view(String branchId) {
        try {
            JsonObject qry = ContractQry.createQuery(
                    Hex.encodeHexString(TestUtils.STEM_CHAIN),
                    "view",
                    ContractQry.createParams("branchId", branchId));
            contractApi.query(qry.toString());
        } catch (Exception e) {
            throw new FailedOperationException("[ERR] view failed");
        }
    }
}
