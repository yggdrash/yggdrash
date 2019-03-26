//package io.yggdrash.contract.versioning;
//
//import com.google.gson.JsonObject;
//import io.yggdrash.common.store.StateStore;
//import io.yggdrash.common.store.datasource.HashMapDbSource;
//import io.yggdrash.common.utils.ContractUtils;
//import io.yggdrash.contract.core.TransactionReceipt;
//import io.yggdrash.contract.core.TransactionReceiptImpl;
//import io.yggdrash.contract.core.annotation.ContractStateStore;
//import org.apache.commons.codec.binary.Base64;
//import org.junit.Before;
//import org.junit.Test;
//
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.IOException;
//import java.lang.reflect.Field;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.util.List;
//
//public class VersioningContractTest {
//         private VersioningContract.VersioningContractService service;
//        private StateStore<JsonObject> store;
//        private Field txReceiptField;
//
//        @Before
//        public void setUp() throws IllegalAccessException {
//            service = new VersioningContract.VersioningContractService();
//            store = new StateStore<>(new HashMapDbSource());
//            List<Field> txReceipt = ContractUtils.txReceiptFields(service);
//            if (txReceipt.size() == 1) {
//                txReceiptField = txReceipt.get(0);
//            }
//
//            for (Field f : ContractUtils.contractFields(service, ContractStateStore.class)) {
//                f.setAccessible(true);
//                f.set(service, store);
//            }
//        }
//
//        @Test
//        public void updateTest() throws Exception {
//            String issuer = "a2b0f5fce600eb6c595b28d6253bed92be0568ed";
//            TransactionReceipt preReceipt = new TransactionReceiptImpl();
//            preReceipt.setBlockHeight(10L);
//            preReceipt.setIssuer(issuer);
//            txReceiptField.set(service, preReceipt);
//            Path currentRelativePath = Paths.get("");
//            String s = currentRelativePath.toAbsolutePath().toString();
//            String s2 = String.format("%s/%s", s, "build");
//            String result = String.format("%s/%s", s2, "contract");
//            JsonObject params = createUpdateParams(result);
//            service.updateProposer(params);
//
//        }
//
//        private JsonObject createUpdateParams(String path) {
//            String bash64String = new String(convertVersionToBase64(path));
//
//            JsonObject params = new JsonObject();
//
//            params.addProperty("contractVersion", "system-coin-contract-1.0.1.jar");
//            params.addProperty("contract", bash64String);
//            return params;
//        }
//
//        public static byte[] convertVersionToBase64(String contractPath) {
//            File contractFile = contractFile(contractPath);
//            if (contractFile.exists()) {
//                byte[] fileArray = loadContract(contractFile);
//                return base64Enc(fileArray);
//            }
//            return null;
//        }
//
//        public static File contractFile(String Path) {
//            String result = String.format("%s/%s", Path, "c6d5d1eba93f08b5df5d212d30151a5de542dc0b.jar");
//            return new File(result);
//        }
//
//        private static byte[] loadContract(File contractFile) {
//            byte[] contractBinary;
//            try (FileInputStream inputStream = new FileInputStream(contractFile)) {
//                contractBinary = new byte[Math.toIntExact(contractFile.length())];
//                inputStream.read(contractBinary);
//            } catch (IOException e) {
//                return null;
//            }
//            return contractBinary;
//        }
//
//        public static byte[] base64Enc(byte[] buffer) {
//            return Base64.encodeBase64(buffer);
//        }
//}
