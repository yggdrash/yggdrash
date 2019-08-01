package io.yggdrash.core.blockchain.osgi;

import com.google.gson.JsonObject;
import io.yggdrash.ContractTestUtils;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.contract.BranchContract;
import io.yggdrash.common.contract.vo.dpoa.Validator;
import io.yggdrash.common.contract.vo.dpoa.ValidatorSet;
import io.yggdrash.common.store.BranchStateStore;
import io.yggdrash.common.store.StateStore;
import io.yggdrash.common.store.datasource.HashMapDbSource;
import io.yggdrash.contract.core.ExecuteStatus;
import io.yggdrash.contract.core.TransactionReceipt;
import io.yggdrash.contract.core.TransactionReceiptAdapter;
import io.yggdrash.contract.core.TransactionReceiptImpl;
import io.yggdrash.contract.core.annotation.ContractBranchStateStore;
import io.yggdrash.contract.core.annotation.ContractStateStore;
import io.yggdrash.contract.core.annotation.ContractTransactionReceipt;
import io.yggdrash.core.blockchain.osgi.service.VersioningContract;
import io.yggdrash.core.store.StoreAdapter;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

public class VesionContractTest {
    private static final VersioningContract.VersioningContractService service =
            new VersioningContract.VersioningContractService();
    private StateStore stateStore;
    private TransactionReceiptAdapter adapter;
    private Field txReceiptField;
    private File contractFile;

    private static final String updateContract = "8c65bc05e107aab9ceaa872bbbb2d96d57811de4";
    private static final String issuer1 = "a2b0f5fce600eb6c595b28d6253bed92be0568ed";
    private static final String issuer2 = "d2a5721e80dc439385f3abc5aab0ac4ed2b1cd95";
    private static final String issuer3 = "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94";

    private static final String txId = "a2b0f5fce600eb6c595b28d6253bed92be0568eda2b0f5fce600eb6c595b28d6253bed92be0568ed";

    @Before
    public void setUp() throws IllegalAccessException {
        stateStore = new StateStore(new HashMapDbSource());
        adapter = new TransactionReceiptAdapter();

        TestBranchStateStore branchStateStore = new TestBranchStateStore();
        Map<String, Validator> validatorMap = branchStateStore.getValidators().getValidatorMap();
        validatorMap.put(issuer1, new Validator(issuer1));
        validatorMap.put(issuer2, new Validator(issuer2));
        validatorMap.put(issuer3, new Validator(issuer3));

        Field[] fields = service.getClass().getDeclaredFields();

        for (Field field : fields) {
            field.setAccessible(true);

            for (Annotation annotation : field.getDeclaredAnnotations()) {
                if (annotation.annotationType().equals(ContractStateStore.class)) {
                    StoreAdapter adapterStore = new StoreAdapter(stateStore, "versioning");
                    field.set(service, adapterStore); //default => tmpStateStore
                }
                if (annotation.annotationType().equals(ContractTransactionReceipt.class)) {
                    field.set(service, adapter);
                }

                if (annotation.annotationType().equals(ContractBranchStateStore.class)) {
                    field.set(service, branchStateStore);
                }
            }
        }
    }

    @Test
    public void updateTest() throws Exception {
        TransactionReceipt receipt = createTxReceipt(issuer1);
        adapter.setTransactionReceipt(receipt);

        String filePath = Objects.requireNonNull(
                getClass().getClassLoader().getResource(String.format("contracts/%s.jar", updateContract))).getFile();

        JsonObject params = ContractTestUtils.versionUpdateTxBodyJson(new File(filePath));
        service.updateProposer(params);

        assertThat(receipt.getStatus()).isEqualTo(ExecuteStatus.SUCCESS);

    }

    @Test
    public void votingSuccessTest() throws Exception {
        TransactionReceipt updateReceipt = createTxReceipt(issuer1);
        adapter.setTransactionReceipt(updateReceipt);

        String filePath = Objects.requireNonNull(
                getClass().getClassLoader().getResource(String.format("contracts/%s.jar", updateContract))).getFile();

        JsonObject updateParam = ContractTestUtils.versionUpdateTxBodyJson(new File(filePath));
        service.updateProposer(updateParam);

        assertThat(updateReceipt.getStatus()).isEqualTo(ExecuteStatus.SUCCESS);

        // file upload & start vote

        JsonObject agree = ContractTestUtils.versionVoteTxBodyJson(updateReceipt.getTxId(), true);
        JsonObject disagree = ContractTestUtils.versionVoteTxBodyJson(updateReceipt.getTxId(), true);

        // vote validator-1 : agree
        service.vote(agree);


        // vote validator-2 : agree
        TransactionReceipt voteReceipt2 = createTxReceipt(issuer2);
        voteReceipt2.setTxId(updateReceipt.getTxId());
        adapter.setTransactionReceipt(voteReceipt2);

        service.vote(agree);

        assertThat(voteReceipt2.getStatus()).isEqualTo(ExecuteStatus.SUCCESS);

        TransactionReceipt voteReceipt3 = createTxReceipt(issuer3);
        voteReceipt3.setTxId(updateReceipt.getTxId());
        adapter.setTransactionReceipt(voteReceipt3);

        service.vote(agree);

        assertThat(voteReceipt3.getStatus()).isEqualTo(ExecuteStatus.SUCCESS);

    }

    private void updateStatusTest() {

    }

//    @Test
//    public void votingFailTest() throws Exception {
//        String issuer = "a2b0f5fce600eb6c595b28d6253bed92be0568ed";
//        TransactionReceipt preReceipt = new TransactionReceiptImpl();
//        preReceipt.setBlockHeight(10L);
//        preReceipt.setIssuer(issuer);
//        preReceipt.setTxId("a2b0f5fce600eb6c595b28d6253bed92be0568eda2b0f5fce600eb6c595b28d6253bed92be0568ed");
//        txReceiptField.set(service, preReceipt);
//        Path currentRelativePath = Paths.get("");
//        String s = currentRelativePath.toAbsolutePath().toString();
//        String s2 = String.format("%s/%s", s, "build");
//        String result = String.format("%s/%s", s2, "contract");
//        JsonObject params = createUpdateParams(result);
//        service.updateProposer(params);
//
//        service.vote(createVoteParams(true));
//        String issuer2 = "d2a5721e80dc439385f3abc5aab0ac4ed2b1cd95";
//        preReceipt.setIssuer(issuer2);
//
//        service.vote(createVoteParams(false));
//
//        String issuer3 = "d2a5721e80dc439385f3abc5aab0ac4ed2b1cd95";
//        preReceipt.setIssuer(issuer3);
//        TransactionReceipt receipt = service.vote(createVoteParams(false));
//    }
//
//    @Test
//    public void notValidatorvotingTest() throws Exception {
//        String issuer = "0dc4393d2a5721e885f3abc5aab0ac4ed2b1cd95";
//        TransactionReceipt preReceipt = new TransactionReceiptImpl();
//        preReceipt.setBlockHeight(10L);
//        preReceipt.setIssuer(issuer);
//        preReceipt.setTxId("a2b0f5fce600eb6c595b28d6253bed92be0568eda2b0f5fce600eb6c595b28d6253bed92be0568ed");
//        txReceiptField.set(service, preReceipt);
//        Path currentRelativePath = Paths.get("");
//        String s = currentRelativePath.toAbsolutePath().toString();
//        String s2 = String.format("%s/%s", s, "build");
//        String result = String.format("%s/%s", s2, "contract");
//        JsonObject params = createUpdateParams(result);
//        service.updateProposer(params);
//        TransactionReceipt receipt = service.vote(createVoteParams(true));
//        assertEquals(ExecuteStatus.FALSE, receipt.getStatus());
//    }

    private TransactionReceipt createTxReceipt(String issuer) {
        TransactionReceipt receipt = new TransactionReceiptImpl();
        receipt.setIssuer(issuer);
        receipt.setBlockHeight(100L);
        receipt.setTxId(txId);

        return receipt;
    }

//    private JsonObject createVoteParams(boolean vote) {
//        JsonObject params = new JsonObject();
//        params.addProperty("txId",
//                "a2b0f5fce600eb6c595b28d6253bed92be0568eda2b0f5fce600eb6c595b28d6253bed92be0568ed");
//        params.addProperty("agree", vote);
//        return params;
//    }
//
//    private JsonObject createUpdateParams(String path) {
//        String bash64String = new String(convertVersionToBase64(path));
//        JsonObject params = new JsonObject();
//        params.addProperty("contractVersion", "4adc453cbd99b3be960118e9eced4b5dad435d0f");
//        params.addProperty("contract", bash64String);
//        return params;
//    }
//
//    private byte[] convertVersionToBase64(String contractPath) {
//        contractFile(contractPath);
//        if (contractFile.exists()) {
//            byte[] fileArray = loadContract(contractFile);
//            return base64Enc(fileArray);
//        }
//        return null;
//    }
//
//    private void contractFile(String path) {
//        try (Stream<Path> filePathStream = Files.walk(Paths.get(String.valueOf(path)))) {
//            filePathStream.forEach(p -> {
//                File contractPath = new File(p.toString());
//                this.contractFile = contractPath;
//            });
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    private static byte[] loadContract(File contractFile) {
//        byte[] contractBinary;
//        try (FileInputStream inputStream = new FileInputStream(contractFile)) {
//            contractBinary = new byte[Math.toIntExact(contractFile.length())];
//            inputStream.read(contractBinary);
//        } catch (IOException e) {
//            return null;
//        }
//        return contractBinary;
//    }
//
//    private static byte[] base64Enc(byte[] buffer) {
//        return Base64.encodeBase64(buffer);
//    }

    public class TestBranchStateStore implements BranchStateStore {
        ValidatorSet set = new ValidatorSet();

        @Override
        public Long getLastExecuteBlockIndex() {
            return null;
        }

        @Override
        public Sha3Hash getLastExecuteBlockHash() {
            return null;
        }

        @Override
        public Sha3Hash getGenesisBlockHash() {
            return null;
        }

        @Override
        public Sha3Hash getBranchIdHash() {
            return null;
        }

        @Override
        public ValidatorSet getValidators() {
            return set;
        }

        @Override
        public boolean isValidator(String address) {
            return set.contains(address);
        }

        @Override
        public List<BranchContract> getBranchContacts() {
            return null;
        }

        @Override
        public String getContractVersion(String contractName) {
            return contractName;
        }

        @Override
        public String getContractName(String contractVersion) {
            return contractVersion;
        }

        public void setValidators(ValidatorSet validatorSet) {
            this.set = validatorSet;
        }
    }

}
