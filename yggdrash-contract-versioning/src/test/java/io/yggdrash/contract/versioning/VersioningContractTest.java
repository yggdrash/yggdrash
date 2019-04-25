package io.yggdrash.contract.versioning;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.common.store.StateStore;
import io.yggdrash.common.store.datasource.HashMapDbSource;
import io.yggdrash.common.utils.ContractUtils;
import io.yggdrash.contract.core.ExecuteStatus;
import io.yggdrash.contract.core.TransactionReceipt;
import io.yggdrash.contract.core.TransactionReceiptImpl;
import io.yggdrash.contract.core.annotation.ContractStateStore;
import org.apache.commons.codec.binary.Base64;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

public class VersioningContractTest {
    private VersioningContract.VersioningContractService service;
    private StateStore store;
    private Field txReceiptField;
    private File contractFile;

    @Before
    public void setUp() throws IllegalAccessException {
        service = new VersioningContract.VersioningContractService();
        store = new StateStore(new HashMapDbSource());
        List<Field> txReceipt = ContractUtils.txReceiptFields(service);
        if (txReceipt.size() == 1) {
            txReceiptField = txReceipt.get(0);
        }

        for (Field f : ContractUtils.contractFields(service, ContractStateStore.class)) {
            f.setAccessible(true);
            f.set(service, store);
        }
        store.put("validatorSet", createValidatorSet());
    }

    @Test
    public void updateTest() throws Exception {
        String issuer = "a2b0f5fce600eb6c595b28d6253bed92be0568ed";
        TransactionReceipt preReceipt = new TransactionReceiptImpl();
        preReceipt.setBlockHeight(10L);
        preReceipt.setIssuer(issuer);
        preReceipt.setTxId("a2b0f5fce600eb6c595b28d6253bed92be0568eda2b0f5fce600eb6c595b28d6253bed92be0568ed");
        txReceiptField.set(service, preReceipt);
        Path currentRelativePath = Paths.get("");
        String s = currentRelativePath.toAbsolutePath().toString();
        String s2 = String.format("%s/%s", s, "build");
        String result = String.format("%s/%s", s2, "contract");
        JsonObject params = createUpdateParams(result);
        service.updateProposer(params);
    }

    @Test
    public void votingSuccessTest() throws Exception {
        String issuer = "a2b0f5fce600eb6c595b28d6253bed92be0568ed";
        TransactionReceipt preReceipt = new TransactionReceiptImpl();
        preReceipt.setBlockHeight(10L);
        preReceipt.setIssuer(issuer);
        preReceipt.setTxId("a2b0f5fce600eb6c595b28d6253bed92be0568eda2b0f5fce600eb6c595b28d6253bed92be0568ed");
        txReceiptField.set(service, preReceipt);
        Path currentRelativePath = Paths.get("");
        String s = currentRelativePath.toAbsolutePath().toString();
        String s2 = String.format("%s/%s", s, "build");
        String result = String.format("%s/%s", s2, "contract");
        JsonObject params = createUpdateParams(result);
        service.updateProposer(params);

        service.vote(createVoteParams(true));
        String issuer2 = "d2a5721e80dc439385f3abc5aab0ac4ed2b1cd95";
        preReceipt.setIssuer(issuer2);

        service.vote(createVoteParams(true));

        String issuer3 = "d2a5721e80dc439385f3abc5aab0ac4ed2b1cd95";
        preReceipt.setIssuer(issuer3);
        service.vote(createVoteParams(false));
    }

    @Test
    public void votingFailTest() throws Exception {
        String issuer = "a2b0f5fce600eb6c595b28d6253bed92be0568ed";
        TransactionReceipt preReceipt = new TransactionReceiptImpl();
        preReceipt.setBlockHeight(10L);
        preReceipt.setIssuer(issuer);
        preReceipt.setTxId("a2b0f5fce600eb6c595b28d6253bed92be0568eda2b0f5fce600eb6c595b28d6253bed92be0568ed");
        txReceiptField.set(service, preReceipt);
        Path currentRelativePath = Paths.get("");
        String s = currentRelativePath.toAbsolutePath().toString();
        String s2 = String.format("%s/%s", s, "build");
        String result = String.format("%s/%s", s2, "contract");
        JsonObject params = createUpdateParams(result);
        service.updateProposer(params);

        service.vote(createVoteParams(true));
        String issuer2 = "d2a5721e80dc439385f3abc5aab0ac4ed2b1cd95";
        preReceipt.setIssuer(issuer2);

        service.vote(createVoteParams(false));

        String issuer3 = "d2a5721e80dc439385f3abc5aab0ac4ed2b1cd95";
        preReceipt.setIssuer(issuer3);
        TransactionReceipt receipt = service.vote(createVoteParams(false));
    }

    @Test
    public void notValidatorvotingTest() throws Exception {
        String issuer = "0dc4393d2a5721e885f3abc5aab0ac4ed2b1cd95";
        TransactionReceipt preReceipt = new TransactionReceiptImpl();
        preReceipt.setBlockHeight(10L);
        preReceipt.setIssuer(issuer);
        preReceipt.setTxId("a2b0f5fce600eb6c595b28d6253bed92be0568eda2b0f5fce600eb6c595b28d6253bed92be0568ed");
        txReceiptField.set(service, preReceipt);
        Path currentRelativePath = Paths.get("");
        String s = currentRelativePath.toAbsolutePath().toString();
        String s2 = String.format("%s/%s", s, "build");
        String result = String.format("%s/%s", s2, "contract");
        JsonObject params = createUpdateParams(result);
        service.updateProposer(params);
        TransactionReceipt receipt = service.vote(createVoteParams(true));
        assertEquals(ExecuteStatus.FALSE, receipt.getStatus());
    }

    private JsonObject createVoteParams(boolean vote) {
        JsonObject params = new JsonObject();
        params.addProperty("txId",
                "a2b0f5fce600eb6c595b28d6253bed92be0568eda2b0f5fce600eb6c595b28d6253bed92be0568ed");
        params.addProperty("agree", vote);
        return params;
    }

    private JsonObject createUpdateParams(String path) {
        String bash64String = new String(convertVersionToBase64(path));
        JsonObject params = new JsonObject();
        params.addProperty("contractVersion", "4adc453cbd99b3be960118e9eced4b5dad435d0f");
        params.addProperty("contract", bash64String);
        return params;
    }

    private byte[] convertVersionToBase64(String contractPath) {
        contractFile(contractPath);
        if (contractFile.exists()) {
            byte[] fileArray = loadContract(contractFile);
            return base64Enc(fileArray);
        }
        return null;
    }

    private void contractFile(String path) {
        try (Stream<Path> filePathStream = Files.walk(Paths.get(String.valueOf(path)))) {
            filePathStream.forEach(p -> {
                File contractPath = new File(p.toString());
                this.contractFile = contractPath;
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static byte[] loadContract(File contractFile) {
        byte[] contractBinary;
        try (FileInputStream inputStream = new FileInputStream(contractFile)) {
            contractBinary = new byte[Math.toIntExact(contractFile.length())];
            inputStream.read(contractBinary);
        } catch (IOException e) {
            return null;
        }
        return contractBinary;
    }

    private static byte[] base64Enc(byte[] buffer) {
        return Base64.encodeBase64(buffer);
    }

    private JsonObject createValidatorSet() {
        JsonObject params = new JsonObject();
        JsonArray arr = new JsonArray();

        params.addProperty("d2a5721e80dc439385f3abc5aab0ac4ed2b1cd95",
                "d2a5721e80dc439385f3abc5aab0ac4ed2b1cd95d2a5721e80dc439385f3abc5aab0ac4ed2b1cd95");
        params.addProperty("a2b0f5fce600eb6c595b28d6253bed92be0568ed",
                "a2b0f5fce600eb6c595b28d6253bed92be0568edd2a5721e80dc439385f3abc5aab0ac4ed2b1cd95");
        params.addProperty("c91e9d46dd4b7584f0b6348ee18277c10fd7cb94",
                "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94d2a5721e80dc439385f3abc5aab0ac4ed2b1cd95");

        return params;
    }
}
