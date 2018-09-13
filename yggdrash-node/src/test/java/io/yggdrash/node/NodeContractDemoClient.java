package io.yggdrash.node;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import java.util.Scanner;

public class NodeContractDemoClient {

    private static Scanner scan = new Scanner(System.in);

    public static void main(String[] args) throws Exception {
        Wallet wallet = new Wallet();
        TransactionHusk tx;
        System.out.println("[1] STEM  [2] YEED");
        if (scan.nextLine().equals("2")) {
            tx = ContractTx.createYeedTx(wallet, new Address(TestUtils.TRANSFER_TO), 100);
        } else {
            System.out.println("사용할 .json 파일명을 입력하세요 (예. sample1.json) : ");
            JsonObject seed = getSampleBranch(scan.nextLine());
            tx = ContractTx.createStemTx(wallet, seed, "create");
        }

        System.out.println("============\n\n[1] 로컬에 트랜잭션 전송 [2] 서버에 트랜잭션 전송 : ");
        sendTx(tx, scan.nextLine());
    }

    private static JsonObject getSampleBranch(String path) throws Exception {
        String sampleSeedPath = "classpath:/seed/%s";
        ResourceLoader resourceLoader = new DefaultResourceLoader();
        Resource resource = resourceLoader.getResource(String.format(sampleSeedPath, path));
        JsonParser jsonParser = new JsonParser();

        return (JsonObject) jsonParser.parse(
                new InputStreamReader(resource.getInputStream(), "UTF-8"));
    }

    private static void sendTx(TransactionHusk tx, String num) {
        TransactionApi txApi;
        if (num.equals("2")) {
            txApi = new JsonRpcConfig().transactionApi("server");
        } else {
            txApi = new JsonRpcConfig().transactionApi();
        }
        txApi.sendTransaction(TransactionDto.createBy(tx));
    }
}
