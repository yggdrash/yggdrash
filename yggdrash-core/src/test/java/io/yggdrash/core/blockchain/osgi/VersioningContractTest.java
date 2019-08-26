package io.yggdrash.core.blockchain.osgi;

import com.google.gson.JsonObject;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.common.contract.BranchContract;
import io.yggdrash.common.contract.vo.dpoa.Validator;
import io.yggdrash.common.contract.vo.dpoa.ValidatorSet;
import io.yggdrash.common.store.BranchStateStore;
import io.yggdrash.common.store.StateStore;
import io.yggdrash.common.store.datasource.HashMapDbSource;
import io.yggdrash.common.utils.JsonUtil;
import io.yggdrash.contract.core.ExecuteStatus;
import io.yggdrash.contract.core.TransactionReceipt;
import io.yggdrash.contract.core.TransactionReceiptAdapter;
import io.yggdrash.contract.core.TransactionReceiptImpl;
import io.yggdrash.contract.core.annotation.ContractBranchStateStore;
import io.yggdrash.contract.core.annotation.ContractStateStore;
import io.yggdrash.contract.core.annotation.ContractTransactionReceipt;
import io.yggdrash.core.blockchain.osgi.service.ContractProposal;
import io.yggdrash.core.blockchain.osgi.service.VersioningContract;
import io.yggdrash.core.store.StoreAdapter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class VersioningContractTest {

    private static final Logger log = LoggerFactory.getLogger(VersioningContractTest.class);

    @Spy
    Downloader downloader = new Downloader(new DefaultConfig());

    @Mock
    private VersioningContract service;

    private StateStore stateStore;
    private TransactionReceiptAdapter adapter;

    private static final String updateContract = "8c65bc05e107aab9ceaa872bbbb2d96d57811de4";
    private static final String issuer1 = "a2b0f5fce600eb6c595b28d6253bed92be0568ed";
    private static final String issuer2 = "d2a5721e80dc439385f3abc5aab0ac4ed2b1cd95";
    private static final String issuer3 = "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94";
    private static final String user = "2f78f54ee5e1209d0417c1edad168f62b933b631";

    private static final String txId = "34eec4dcb662e54492e3b69adb1d2dce5d7451ca6d22221c38ce5bc6f8871b51";

    @Before
    public void setUp() throws IllegalAccessException {
        MockitoAnnotations.initMocks(this);
        service = new VersioningContract();

        stateStore = new StateStore(new HashMapDbSource());
        adapter = new TransactionReceiptAdapter();

        TestBranchStateStore branchStateStore = new TestBranchStateStore(); //TODO Mock
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
    public void voteProposalTest() {
        // Propose contract
        contractPropose();

        // Check contract proposal status
        ContractProposal status = proposalStatus();
        log.debug("Proposal Status : {}", JsonUtil.convertObjToString(status));

        vote(issuer3, false);
        vote(issuer2, true);

        assertTrue(adapter.getTxLog().contains("Update proposal voting is in progress"));

        vote(issuer1, true); // agreeCnt -> 2/3

        assertTrue(adapter.getTxLog().contains("Contract file has been downloaded"));
        assertTrue(adapter.getTxLog().contains("Update proposal voting was completed successfully"));
    }

    @Test
    public void voteFailedTest() {
        contractPropose();

        TransactionReceipt receipt = createTxReceipt(user);
        adapter.setTransactionReceipt(receipt);

        JsonObject param = new JsonObject();
        param.addProperty("txId", txId);
        param.addProperty("agree", true);

        // Validator validation failed
        service.vote(param);
        printTxLog();

        assertEquals(ExecuteStatus.FALSE, adapter.getStatus());
        assertTrue(adapter.getTxLog().contains("Validator verification failed"));

        receipt.setIssuer(issuer1);
        receipt.setTxId("0xbcd28b03f23d78f5c5bfebff78fee9f660b39bb5feac125e5f2d9224150ab0d3");
        receipt.setBlockHeight(70000L);
        adapter.setTransactionReceipt(receipt);

        // Proposal expiration validation failed
        service.vote(param);
        printTxLog();

        assertEquals(ExecuteStatus.FALSE, adapter.getStatus());
        assertTrue(adapter.getTxLog().contains("Contract proposal has already expired"));

        receipt.setIssuer(issuer2);
        receipt.setTxId("0xc574156e631044749c4eba404579f634cd0b10d0da5d4c6cc476879416ec8752");
        receipt.setBlockHeight(101L);
        adapter.setTransactionReceipt(receipt);
        param.addProperty("txId", "0xbcd28b03f23d78f5c5bfebff78fee9f660b39bb5feac125e5f2d9224150ab0d3 ");

        // Proposal not found
        service.vote(param);
        printTxLog();

        assertEquals(ExecuteStatus.FALSE, adapter.getStatus());
        assertTrue(adapter.getTxLog().contains("Contract proposal not found"));
    }

    public void contractPropose() {
        // The contract file is already uploaded to S3

        TransactionReceipt receipt = createTxReceipt(issuer1);
        adapter.setTransactionReceipt(receipt);

        JsonObject param = new JsonObject();
        param.addProperty("contractVersion", updateContract);
        param.addProperty("sourceUrl", "https://github.com/yggdrash/yggdrash");
        param.addProperty("buildVersion", "1.8.0_172");
        param.addProperty("votingPeriod", 200L);

        service.propose(param);

        assertEquals(ExecuteStatus.SUCCESS, receipt.getStatus());
        assertTrue(receipt.getTxLog().contains("Contract proposal has been issued"));
    }

    public ContractProposal proposalStatus() {
        JsonObject param = new JsonObject();
        param.addProperty("txId", txId);

        return service.proposalStatus(param);
    }

    private TransactionReceipt createTxReceipt(String issuer) {
        TransactionReceipt receipt = new TransactionReceiptImpl();
        receipt.setIssuer(issuer);
        receipt.setBlockHeight(100L);
        receipt.setTxId(txId);

        return receipt;
    }

    private void printTxLog() {
        log.debug("Issuer : {}", adapter.getIssuer());
        log.debug("TxId : {}", adapter.getTxId());
        log.debug("BlockHeight : {}", adapter.getBlockHeight());
        log.debug("Status : {}", adapter.getStatus());
        log.debug("TxLog : {}", adapter.getTxLog());
        log.debug("=========================================================================");
    }

    private TransactionReceipt vote(String issuer, boolean agree) {
        TransactionReceipt receipt = createTxReceipt(issuer);
        adapter.setTransactionReceipt(receipt);

        JsonObject param = new JsonObject();
        param.addProperty("txId", txId);
        param.addProperty("agree", agree);

        service.vote(param);

        assertEquals(ExecuteStatus.SUCCESS, receipt.getStatus());

        return receipt;
    }

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
