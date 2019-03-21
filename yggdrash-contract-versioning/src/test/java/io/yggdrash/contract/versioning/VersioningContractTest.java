package io.yggdrash.contract.versioning;

import com.google.gson.JsonObject;
import io.yggdrash.common.store.StateStore;
import io.yggdrash.common.store.datasource.HashMapDbSource;
import io.yggdrash.common.utils.ContractUtils;
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
import java.util.List;

public class VersioningContractTest {
    private VersioningContract.VersioningContractService service;
    private StateStore<JsonObject> store;
    private Field txReceiptField;

    @Before
    public void setUp() throws IllegalAccessException {
        service = new VersioningContract.VersioningContractService();
        store = new StateStore<>(new HashMapDbSource());
        List<Field> txReceipt = ContractUtils.txReceiptFields(service);
        if (txReceipt.size() == 1) {
            txReceiptField = txReceipt.get(0);
        }

        for (Field f : ContractUtils.contractFields(service, ContractStateStore.class)) {
            f.setAccessible(true);
            f.set(service, store);
        }
    }

    @Test
    public void updateTest() throws Exception {
        String issuer = "a2b0f5fce600eb6c595b28d6253bed92be0568ed";
        TransactionReceipt preReceipt = new TransactionReceiptImpl();
        preReceipt.setBlockHeight(10L);
        preReceipt.setIssuer(issuer);
        txReceiptField.set(service, preReceipt);

        JsonObject params = createUpdateParams();
        service.updateProposer(params);
    }

    private JsonObject createUpdateParams() {
        String branchId = "8b176b18903237a24d3cd4a5dc88feaa5a0dc746";
        String bash64String = new String(convertVersionToBase64(
                "/Users/haewonwoo/woohae/yggdrash/yggdrash-core/.yggdrash/osgi", branchId));

        JsonObject params = new JsonObject();

        params.addProperty("contractVersion", "system-coin-contract-1.0.0.jar");
        params.addProperty("contract", bash64String);
        return params;
    }

    public static byte[] convertVersionToBase64(String contractPath, String branchId) {
        File contractFile = contractFile(contractPath, branchId);
        if (contractFile.exists()) {
            byte[] fileArray = loadContract(contractFile);
            return base64Enc(fileArray);
        }
        return null;
    }

    public static File contractFile(String Path, String branchId) {
        String containerPath = String.format("%s/%s", Path, branchId);
        String systemContractPath = String.format("%s/bundles%s", containerPath, "/system-contracts");

        String result = String.format("%s/%s", systemContractPath, "system-coin-contract-1.0.0.jar");
        return new File(result);
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

    public static byte[] base64Enc(byte[] buffer) {
        return Base64.encodeBase64(buffer);
    }
}
