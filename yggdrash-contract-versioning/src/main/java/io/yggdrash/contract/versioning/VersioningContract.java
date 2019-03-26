package io.yggdrash.contract.versioning;

import com.google.gson.JsonObject;
import io.yggdrash.common.contract.ContractVersion;
import io.yggdrash.common.contract.vo.PrefixKeyEnum;
import io.yggdrash.common.contract.vo.dpoa.ValidatorSet;
import io.yggdrash.common.utils.JsonUtil;
import io.yggdrash.contract.core.ExecuteStatus;
import io.yggdrash.contract.core.TransactionReceipt;
import io.yggdrash.contract.core.annotation.ContractQuery;
import io.yggdrash.contract.core.annotation.ContractStateStore;
import io.yggdrash.contract.core.annotation.ContractTransactionReceipt;
import io.yggdrash.contract.core.annotation.InvokeTransaction;
import io.yggdrash.contract.core.store.ReadWriterStore;
import io.yggdrash.contract.dpoa.DPoAContract;
import org.apache.commons.codec.binary.Base64;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Hashtable;

public class VersioningContract implements BundleActivator, ServiceListener {
    private static final Logger log = LoggerFactory.getLogger(VersioningContract.class);
    private BundleContext bundleContext = null;
    private ServiceTracker serviceTracker = null;

    @Override
    public void start(BundleContext context) throws Exception {
        log.info("⚪ Start contract version control");

        bundleContext = context;
        serviceTracker = new ServiceTracker(
                bundleContext
                , bundleContext.createFilter(
                "(&(objectClass=" + DPoAContract.DPoAService.class.getName() + ")" +
                        "(YGGDRASH=DPoA))"),
                null
        );
        serviceTracker.open();

        Hashtable<String, String> props = new Hashtable<>();
        props.put("YGGDRASH", "ContractVersionControl");
//        context.registerService(VersioningContractService.class.getName(), new VersioningContractService(serviceTracker), props);
        context.registerService(VersioningContractService.class.getName(), new VersioningContractService(), props);
    }

    @Override
    public void stop(BundleContext context) {
        log.info("⚫ Stop contract version control");
    }

    @Override
    public void serviceChanged(ServiceEvent event) {

    }

    public static class VersioningContractService {
        private static final Long MAX_FILE_LENGTH = 5242880L; // default 5MB bytes
        private static final String SUFFIX_UPDATE_CONTRACT = "/update-temp-contracts";
        private static final String SUFFIX = ".jar";
//        private final ServiceTracker serviceTracker;

//        public VersioningContractService(ServiceTracker serviceTracker) {
//            this.serviceTracker = serviceTracker;
//        }

        @ContractStateStore
        ReadWriterStore<String, JsonObject> state;

        @ContractTransactionReceipt
        TransactionReceipt txReceipt;

        public ProposeContractSet getProposeValidatorSet(String txId) {
            ProposeContractSet proposeContractSet = null;
            JsonObject json = state.get(PrefixKeyEnum.PROPOSE_VALIDATORS.toValue());
            if (json != null) {
                proposeContractSet = JsonUtil.generateJsonToClass(
                        json.toString(), ProposeContractSet.class);
            }

            return proposeContractSet;
        }

        @InvokeTransaction
        public TransactionReceipt updateProposer(JsonObject params) throws UnsupportedEncodingException {
            // TODO contract name 컨트랙트 조회 - branch store에서 조회

            VersioningContractStateValue stateValue;
            try {
                stateValue = VersioningContractStateValue.of(txReceipt.getTxId());
                stateValue.init();

                if (!validatorVerify()) {
                    return txReceipt;
                }

                String upgradeContract = params.get("contract").getAsString();
                byte[] binaryFile = base64Dec(upgradeContract.getBytes("UTF-8"));

                if ((binaryFile == null) || !sizeVerify(binaryFile)) {
                    return txReceipt;
                }

                try {
                    setStateValue(stateValue, binaryFile, params.get("contractVersion").getAsString());
                    FileOutputStream fos;
                    String exportPath = System.getProperty("user.dir");
                    String tempContractPath = String.format("%s/src/main/resources%s", exportPath, SUFFIX_UPDATE_CONTRACT);

                    File fileDir = new File(tempContractPath);
                    if (!fileDir.exists()) {
                        fileDir.mkdirs();
                    }
                    ContractVersion version = ContractVersion.of(binaryFile);
                    File destFile = new File(tempContractPath + File.separator + version + SUFFIX);
                    destFile.setReadOnly();

                    fos = new FileOutputStream(destFile);
                    fos.write(binaryFile);
                    fos.close();

                    state.put(txReceipt.getTxId(),
                            stateValue.getJson().get(txReceipt.getTxId()).getAsJsonObject());
                    txReceipt.setStatus(ExecuteStatus.SUCCESS);
                    log.info("[Contract | update] TX Id => " + txReceipt.getTxId());
                    log.info("[Contract | update] Contract State => " + stateValue.getJson());
                } catch (Exception e) {
                    log.error(e.toString());
                    txReceipt.setStatus(ExecuteStatus.FALSE);
                }

            } catch (Exception e) {
                log.warn("Failed to convert json = {}", params);
            }
            return txReceipt;
        }

        private void setStateValue(VersioningContractStateValue stateValue, byte[] contractBinary, String targetVersion) {
            stateValue.setTargetContractVersion(targetVersion);
            stateValue.setBlockHeight(txReceipt.getBlockHeight());
            stateValue.setUpdateContract(contractBinary);
            DPoAContract.DPoAService dPoAService = new DPoAContract.DPoAService();
            //TODO set validatoreSet in branch store
            ValidatorSet validatorSet = dPoAService.getValidatorSet();
            stateValue.setVotable(txReceipt.getIssuer(), validatorSet);
        }

        @InvokeTransaction
        public TransactionReceipt vote(JsonObject params) {
            txReceipt.setStatus(ExecuteStatus.FALSE);

            VersioningContractStateValue stateValue;
            try {
                ContractVote contractVote = JsonUtil.generateJsonToClass(params.toString(), ContractVote.class);
                ProposeContractSet proposeContractSet = getProposerContract(contractVote.getTxId());


//                DPoAContract.DPoAService dPoAService = (DPoAContract.DPoAService) serviceTracker.getService();
                DPoAContract.DPoAService dPoAService = new DPoAContract.DPoAService();
                ValidatorSet validatorSet = dPoAService.getValidatorSet();
                ProposeContractSet.Votable votable = new ProposeContractSet.Votable(txReceipt.getIssuer(), validatorSet);
                if (votable.getVotedMap().get(txReceipt.getIssuer()) == null
                        || votable.getVotedMap().get(txReceipt.getIssuer()).isVoted()) {
                    return txReceipt;
                }

                if (contractVote.isAgree()) {
                    votable.setAgreeCnt(votable.getAgreeCnt() + 1);
                } else {
                    votable.setDisagreeCnt(votable.getDisagreeCnt() + 1);
                }
                votable.getVotedMap().get(txReceipt.getIssuer()).setAgree(contractVote.isAgree());
                votable.getVotedMap().get(txReceipt.getIssuer()).setVoted(true);

                if (contractVote.isAgree()) {
                    votable.setAgreeCnt(votable.getAgreeCnt() + 1);
                } else {
                    votable.setDisagreeCnt(votable.getDisagreeCnt() + 1);
                }
                votable.getVotedMap().get(txReceipt.getIssuer()).setAgree(contractVote.isAgree());
                votable.getVotedMap().get(txReceipt.getIssuer()).setVoted(true);

                stateValue = VersioningContractStateValue.of(contractVote.getTxId());
                stateValue.setContract(getContract(contractVote.getTxId()));
                stateValue.setVotable(txReceipt.getIssuer(), validatorSet);
                txReceipt.setStatus(ExecuteStatus.SUCCESS);
            } catch (Exception e) {
                log.warn("Failed to convert json = {}", params);
            }
            return txReceipt;
        }

        @ContractQuery
        public Contract updateStatus(JsonObject params) {
            Contract contract = null;
            String txId = params.get("txId").getAsString();
            JsonObject json = state.get(txId);
            if (json != null) {
                contract = JsonUtil.generateJsonToClass(json.toString(), Contract.class);
            }
            return contract;
        }

        private boolean validatorVerify() {
            //TODO change get validator in branch store
//            DPoAContract.DPoAService dPoAService = (DPoAContract.DPoAService) serviceTracker.getService();
//            dPoAService.getValidatorSet();
//
//            ValidatorSet validatorSet = dPoAService.getValidatorSet();
//            if (validatorSet == null || validatorSet.getValidatorMap() == null
//                    || validatorSet.getValidatorMap().get(txReceipt.getIssuer()) == null) {
//                return false;
//            }
            return true;
        }

        @ContractQuery
        public ProposeContractSet getProposerContract(String txId) {
            ProposeContractSet proposeContractSet = null;
            JsonObject json = state.get(txId);

            if (json != null) {
                proposeContractSet = JsonUtil.generateJsonToClass(json.toString(), ProposeContractSet.class);
            }
            return proposeContractSet;
        }

        @ContractQuery
        public Contract getContract(String txId) {
            Contract contract = null;
            JsonObject json = state.get(txId);
            if (json != null) {
                contract = JsonUtil.generateJsonToClass(json.toString(), Contract.class);
            }
            return contract;
        }


        private boolean sizeVerify(byte[] binaryFile) {
            if (binaryFile.length > MAX_FILE_LENGTH) {
                return false;
            }
            return true;
        }

        private static byte[] base64Dec(byte[] buffer) {
            return Base64.decodeBase64(buffer);
        }
    }
}
