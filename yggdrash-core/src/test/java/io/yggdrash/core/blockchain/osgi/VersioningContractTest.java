package io.yggdrash.core.blockchain.osgi;

import com.google.gson.JsonObject;
import io.yggdrash.TestConstants;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.common.contract.BranchContract;
import io.yggdrash.common.contract.vo.dpoa.Validator;
import io.yggdrash.common.contract.vo.dpoa.ValidatorSet;
import io.yggdrash.common.store.BranchStateStore;
import io.yggdrash.common.store.StateStore;
import io.yggdrash.common.store.datasource.HashMapDbSource;
import io.yggdrash.common.utils.JsonUtil;
import io.yggdrash.contract.core.ContractEvent;
import io.yggdrash.contract.core.ExecuteStatus;
import io.yggdrash.contract.core.Receipt;
import io.yggdrash.contract.core.ReceiptAdapter;
import io.yggdrash.contract.core.ReceiptImpl;
import io.yggdrash.contract.core.annotation.ContractBranchStateStore;
import io.yggdrash.contract.core.annotation.ContractReceipt;
import io.yggdrash.contract.core.annotation.ContractStateStore;
import io.yggdrash.contract.core.channel.ContractEventType;
import io.yggdrash.core.blockchain.osgi.service.ContractProposal;
import io.yggdrash.core.blockchain.osgi.service.VersioningContract;
import io.yggdrash.core.blockchain.osgi.service.VotingProgress;
import io.yggdrash.core.store.StoreAdapter;
import org.junit.Assert;
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class VersioningContractTest {

    private static final Logger log = LoggerFactory.getLogger(VersioningContractTest.class);

    @Spy
    Downloader downloader = new Downloader(new DefaultConfig());

    @Mock
    private VersioningContract service;

    private StateStore stateStore;
    private ReceiptAdapter adapter;

    private static final String updateContract = "8c65bc05e107aab9ceaa872bbbb2d96d57811de4";
    private static final String issuer1 = "a2b0f5fce600eb6c595b28d6253bed92be0568ed";
    private static final String issuer2 = "d2a5721e80dc439385f3abc5aab0ac4ed2b1cd95";
    private static final String issuer3 = "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94";
    private static final String user = "2f78f54ee5e1209d0417c1edad168f62b933b631";

    private static final String txId = "34eec4dcb662e54492e3b69adb1d2dce5d7451ca6d22221c38ce5bc6f8871b51";
    private static final long curBlockHeight = 100;

    @Before
    public void setUp() throws IllegalAccessException {
        MockitoAnnotations.initMocks(this);
        service = new VersioningContract();

        stateStore = new StateStore(new HashMapDbSource());
        adapter = new ReceiptAdapter();

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
                if (annotation.annotationType().equals(ContractReceipt.class)) {
                    field.set(service, adapter);
                }

                if (annotation.annotationType().equals(ContractBranchStateStore.class)) {
                    field.set(service, branchStateStore);
                }
            }
        }
    }

    private static long VOTE_PERIOD = 10L;
    private static long APPLY_PERIOD = 10L;

    @Test
    public void endBlockTestInstallEvent() {
        contractPropose();

        // issuer1 is proposer
        vote(issuer3, false);
        vote(issuer2, true); // agreeCnt -> 2/3
        //vote(issuer1, true);

        // EndBlock receipt
        Receipt receipt = new ReceiptImpl();
        receipt.setBlockHeight(curBlockHeight + VOTE_PERIOD); // EndBlock Height
        adapter.setReceipt(receipt);

        service.endBlock();

        assertEquals(ExecuteStatus.SUCCESS, adapter.getStatus());
        assertFalse(adapter.getEvents().isEmpty());
        assertEquals(1, adapter.getEvents().size());
        ContractEvent event = adapter.getEvents().stream().findFirst().get();
        log.info("EndBlock Status : {},  Event : {}", adapter.getStatus(), JsonUtil.parseJsonObject(event));
        assertEquals(ContractEventType.AGREE, event.getType());

    }

    @Test
    public void endblockTestExpiredEvent() {
        contractPropose();

        // EndBlock receipt
        Receipt receipt = new ReceiptImpl();
        receipt.setBlockHeight(curBlockHeight + VOTE_PERIOD); // EndBlock Height
        adapter.setReceipt(receipt);

        service.endBlock();

        assertEquals(ExecuteStatus.SUCCESS, adapter.getStatus());
        assertEquals(VotingProgress.VotingStatus.EXPIRED, proposalStatus().getVotingProgress().votingStatus);

    }

    @Test
    public void voteSuccessTest() {
        // Propose contract
        contractPropose();

        // Check contract proposal status
        ContractProposal status = proposalStatus();
        log.debug("Proposal Status : {}", JsonUtil.convertObjToString(status));

        // issuer1 is proposer
        vote(issuer3, false);

        Assert.assertEquals(VotingProgress.VotingStatus.VOTEABLE, proposalStatus().getVotingProgress().votingStatus);

        assertTrue(adapter.getLog().contains("Update proposal voting is in progress"));

        vote(issuer2, true); // agreeCnt -> 2/3

        Assert.assertEquals(VotingProgress.VotingStatus.AGREE, proposalStatus().getVotingProgress().votingStatus);
    }

    @Test
    public void voteFailedTest() {
        contractPropose();

        Receipt receipt = createTxReceipt(user);
        adapter.setReceipt(receipt);

        JsonObject param = new JsonObject();
        param.addProperty("txId", txId);
        param.addProperty("agree", true);

        // Validator validation failed
        service.vote(param);
        printTxLog();

        assertEquals(ExecuteStatus.FALSE, adapter.getStatus());
        assertTrue(adapter.getLog().contains("Validator verification failed"));

        receipt.setIssuer(issuer1);
        receipt.setTxId("0xbcd28b03f23d78f5c5bfebff78fee9f660b39bb5feac125e5f2d9224150ab0d3");
        adapter.setReceipt(receipt);

        service.vote(param);
        printTxLog();

        assertEquals(ExecuteStatus.FALSE, adapter.getStatus());
        assertTrue(adapter.getLog().contains("Validator has already voted"));

        receipt.setIssuer(issuer2);
        receipt.setTxId("0xbcd28b03f23d78f5c5bfebff78fee9f660b39bb5feac125e5f2d9224150ab0d3");
        receipt.setBlockHeight(70000L);
        adapter.setReceipt(receipt);

        // Proposal expiration validation failed
        service.vote(param);
        printTxLog();

        assertEquals(ExecuteStatus.FALSE, adapter.getStatus());
        assertTrue(adapter.getLog().contains("Contract proposal has already expired"));

        receipt.setIssuer(issuer2);
        receipt.setTxId("0xc574156e631044749c4eba404579f634cd0b10d0da5d4c6cc476879416ec8752");
        receipt.setBlockHeight(101L);
        adapter.setReceipt(receipt);
        param.addProperty("txId", "0xbcd28b03f23d78f5c5bfebff78fee9f660b39bb5feac125e5f2d9224150ab0d3 ");

        // Proposal not found
        service.vote(param);
        printTxLog();

        assertEquals(ExecuteStatus.FALSE, adapter.getStatus());
        assertTrue(adapter.getLog().contains("Contract proposal not found"));
    }

    @Test
    public void proposeMultipleContracts() {
        // Two contract events at the same blockHeight test

        // Activate proposal
        Receipt activateReceipt = createTxReceipt(issuer1);
        adapter.setReceipt(activateReceipt);

        JsonObject param1 = new JsonObject();
        param1.addProperty("proposalVersion", updateContract);
        param1.addProperty("sourceUrl", "https://github.com/yggdrash/yggdrash");
        param1.addProperty("buildVersion", "1.8.0_172");
        param1.addProperty("proposalType", "activate");
        param1.addProperty("votePeriod", VOTE_PERIOD);
        param1.addProperty("applyPeriod", APPLY_PERIOD);

        service.propose(param1);
        assertEquals(ExecuteStatus.SUCCESS, activateReceipt.getStatus());

        vote(issuer2, true);
        vote(issuer3, true);

        // Deactivate proposal
        String deactivateTxId = "567ce4e36663c859bbe72f0bb90977c9d083f19120d0ecbfc48c8e5cfae88a94";
        Receipt deactivateReceipt = createTxReceipt(issuer1, deactivateTxId);
        adapter.setReceipt(deactivateReceipt);

        JsonObject param2 = new JsonObject();
        param2.addProperty("proposalVersion", "f8f7c637abbd33422f966974663c2d73280840f3");
        param2.addProperty("sourceUrl", "https://github.com/yggdrash/yggdrash");
        param2.addProperty("buildVersion", "1.0.0");
        param2.addProperty("proposalType", "deactivate");
        param2.addProperty("votePeriod", VOTE_PERIOD);
        param2.addProperty("applyPeriod", APPLY_PERIOD);

        service.propose(param2);
        assertEquals(ExecuteStatus.SUCCESS, deactivateReceipt.getStatus());

        vote(issuer2, true, deactivateTxId);
        vote(issuer3, true, deactivateTxId);

        // EndBlock of TargetBlockHeight
        Receipt endBlockReceipt1 = new ReceiptImpl();
        endBlockReceipt1.setBlockHeight(curBlockHeight + VOTE_PERIOD); // EndBlock Height
        adapter.setReceipt(endBlockReceipt1);

        service.endBlock();

        assertEquals("EndBlock Status of TargetBlockHeight", ExecuteStatus.SUCCESS, endBlockReceipt1.getStatus());
        assertEquals("EndBlock ContractEvent size of TargetBlockHeight", 2, endBlockReceipt1.getEvents().size());

        for (ContractEvent event : endBlockReceipt1.getEvents()) {
            log.debug("ContractEvent Json : {}", JsonUtil.parseJsonObject(event));
            assertEquals(ContractEventType.AGREE, event.getType());
        }

        // EndBlock of ApplyBlockHeight
        Receipt endBlockReceipt2 = new ReceiptImpl();
        endBlockReceipt2.setBlockHeight(curBlockHeight + VOTE_PERIOD + APPLY_PERIOD); // EndBlock Height
        adapter.setReceipt(endBlockReceipt2);

        service.endBlock();

        assertEquals("EndBlock Status of TargetBlockHeight", ExecuteStatus.SUCCESS, endBlockReceipt2.getStatus());
        assertEquals("EndBlock ContractEvent size of TargetBlockHeight", 2, endBlockReceipt2.getEvents().size());

        for (ContractEvent event : endBlockReceipt2.getEvents()) {
            log.debug("ContractEvent Json : {}", JsonUtil.parseJsonObject(event));
            assertEquals(ContractEventType.APPLY, event.getType());
        }
    }

    public void contractPropose() {
        // The contract file is already uploaded to S3

        Receipt receipt = createTxReceipt(issuer1);
        adapter.setReceipt(receipt);

        JsonObject param = new JsonObject();
        param.addProperty("proposalVersion", updateContract);
        param.addProperty("sourceUrl", "https://github.com/yggdrash/yggdrash");
        param.addProperty("buildVersion", "1.8.0_172");
        param.addProperty("proposalType", "activate");
        param.addProperty("votePeriod", VOTE_PERIOD);
        param.addProperty("applyPeriod", APPLY_PERIOD);

        service.propose(param);

        assertEquals(ExecuteStatus.SUCCESS, receipt.getStatus());
        assertTrue(receipt.getLog().contains("Contract proposal has been issued"));
    }

    public ContractProposal proposalStatus() {
        JsonObject param = new JsonObject();
        param.addProperty("txId", txId);

        return service.proposalStatus(param);
    }

    private Receipt createTxReceipt(String issuer) {
        return createTxReceipt(issuer, txId);
    }

    private Receipt createTxReceipt(String issuer, String txId) {
        Receipt receipt = new ReceiptImpl();
        receipt.setIssuer(issuer);
        receipt.setBlockHeight(curBlockHeight);
        receipt.setTxId(txId);
        receipt.setContractVersion(TestConstants.VERSIONING_CONTRACT.toString());

        return receipt;
    }

    private void printTxLog() {
        log.debug("Issuer : {}", adapter.getIssuer());
        log.debug("TxId : {}", adapter.getTxId());
        log.debug("BlockHeight : {}", adapter.getBlockHeight());
        log.debug("Status : {}", adapter.getStatus());
        log.debug("TxLog : {}", adapter.getLog());
        log.debug("=========================================================================");
    }

    private Receipt vote(String issuer, boolean agree) {
        return vote(issuer, agree, txId);
    }

    private Receipt vote(String issuer, boolean agree, String txId) {
        Receipt receipt = createTxReceipt(issuer);
        adapter.setReceipt(receipt);

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
