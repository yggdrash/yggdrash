package io.yggdrash.contract.versioning;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.common.store.StateStore;
import io.yggdrash.common.store.datasource.HashMapDbSource;
import io.yggdrash.common.utils.ContractUtils;
import io.yggdrash.contract.core.TransactionReceipt;
import io.yggdrash.contract.core.TransactionReceiptImpl;
import io.yggdrash.contract.core.annotation.ContractStateStore;
import org.apache.commons.codec.binary.Base64;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
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
            store.put("validatorSet", createValidatorSet());
        }

        @Test
        @Ignore
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
        @Ignore
        public void votingTest() throws Exception {
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

            service.vote(createVoteParams());
            String issuer2 = "d2a5721e80dc439385f3abc5aab0ac4ed2b1cd95";
            preReceipt.setIssuer(issuer2);

            service.vote(createVoteParams());
        }

        private JsonObject createVoteParams() {
            JsonObject params = new JsonObject();
            params.addProperty("txId", "a2b0f5fce600eb6c595b28d6253bed92be0568eda2b0f5fce600eb6c595b28d6253bed92be0568ed");
            params.addProperty("agree", true);
            return params;
        }

        private JsonObject createUpdateParams(String path) {
            String bash64String = new String(convertVersionToBase64(path));
            JsonObject params = new JsonObject();
            params.addProperty("contractVersion", "4adc453cbd99b3be960118e9eced4b5dad435d0f");
            params.addProperty("contract", bash64String);
            return params;
        }

        private static byte[] convertVersionToBase64(String contractPath) {
            File contractFile = contractFile(contractPath);
            if (contractFile.exists()) {
                byte[] fileArray = loadContract(contractFile);
                return base64Enc(fileArray);
            }
            return null;
        }

        private static File contractFile(String Path) {
            String result = String.format("%s/%s", Path, "84a3384724f69f2c7ea8b9bb932b59d5037f4a7f.jar");
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

        private static byte[] base64Enc(byte[] buffer) {
            return Base64.encodeBase64(buffer);
        }

        private JsonObject createValidatorSet() {
            JsonObject params = new JsonObject();
            JsonArray arr = new JsonArray();

            params.addProperty("d2a5721e80dc439385f3abc5aab0ac4ed2b1cd95", "d2a5721e80dc439385f3abc5aab0ac4ed2b1cd95d2a5721e80dc439385f3abc5aab0ac4ed2b1cd95");
            params.addProperty("a2b0f5fce600eb6c595b28d6253bed92be0568ed", "a2b0f5fce600eb6c595b28d6253bed92be0568edd2a5721e80dc439385f3abc5aab0ac4ed2b1cd95");
            params.addProperty("c91e9d46dd4b7584f0b6348ee18277c10fd7cb94", "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94d2a5721e80dc439385f3abc5aab0ac4ed2b1cd95");

            return params;
        }
}
