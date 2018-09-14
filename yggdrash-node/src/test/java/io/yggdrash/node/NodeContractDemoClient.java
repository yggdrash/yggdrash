package io.yggdrash.node;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.yggdrash.TestUtils;
import io.yggdrash.contract.ContractTx;
import io.yggdrash.core.Address;
import io.yggdrash.core.TransactionHusk;
import io.yggdrash.core.Wallet;
import io.yggdrash.node.api.JsonRpcConfig;
import io.yggdrash.node.api.TransactionApi;
import io.yggdrash.node.controller.TransactionDto;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class NodeContractDemoClient {

    private static Scanner scan = new Scanner(System.in);

    public static void main(String[] args) throws Exception {
        Wallet wallet = new Wallet();
        System.out.print("============\n\n");
        System.out.print("[1] 로컬에 트랜잭션 전송  [2] 배포서버(10.10.10.100)  [서버 주소] 에 트랜잭션 전송");
        System.out.print("(기본값: 로컬) : ");
        String server = scan.nextLine();
        if ("2".equals(server)) {
            server = "10.10.10.100";
        }
        System.out.print("[1] STEM  [2] YEED : ");
        if (scan.nextLine().equals("2")) {
            TransactionHusk tx =
                ContractTx.createYeedTx(wallet, new Address(TestUtils.TRANSFER_TO), 100);
            sendTx(tx, server);

        } else {
            System.out.print("사용할 .json 파일명을 입력하세요 (기본값: sample1.json) : ");
            String json = scan.nextLine();
            if ("".equals(json)) {
                json = "sample1.json";
            }
            JsonObject seed = getSampleBranch(json);
            System.out.print("전송할 횟수를 입력하세요 기본값(1) : ");
            String times = scan.nextLine();

            if ("".equals(json)) {
                times = "1";
            }
            for (int i = Integer.parseInt(times); i > 0; i--) {
                TransactionHusk tx = ContractTx.createStemTx(wallet, seed, "create");
                sendTx(tx, server);
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

    private static void sendTx(TransactionHusk tx, String server) {
        TransactionApi txApi;
        if (server.contains(".")) {
            txApi = new JsonRpcConfig().transactionApi();
        } else {
            txApi = new JsonRpcConfig().transactionApi();
        }
        txApi.sendTransaction(TransactionDto.createBy(tx));
    }
}
