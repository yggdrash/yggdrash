package io.yggdrash.contract.versioning;

import com.google.gson.JsonObject;
import io.yggdrash.common.contract.ContractVersion;
import io.yggdrash.common.utils.JsonUtil;
import io.yggdrash.contract.core.ExecuteStatus;
import io.yggdrash.contract.core.TransactionReceipt;
import io.yggdrash.contract.core.annotation.ContractQuery;
import io.yggdrash.contract.core.annotation.ContractStateStore;
import io.yggdrash.contract.core.annotation.ContractTransactionReceipt;
import io.yggdrash.contract.core.annotation.InvokeTransaction;
import io.yggdrash.contract.core.store.ReadWriterStore;
import org.apache.commons.codec.binary.Base64;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

public class VersioningContract implements BundleActivator{
    private static final Logger log = LoggerFactory.getLogger(VersioningContract.class);

    @Override
    public void start(BundleContext context) throws Exception {
        log.info("⚪ Start versioning contract");
        Hashtable<String, String> props = new Hashtable<>();
        props.put("YGGDRASH", "ContractVersioning");
        context.registerService(VersioningContractService.class.getName(), new VersioningContractService(), props);
    }

    @Override
    public void stop(BundleContext context) {
        log.info("⚫ Stop versioning contract");
    }

    public static class VersioningContractService {
        private static final Long MAX_FILE_LENGTH = 5242880L; // default 5MB bytes
        private static final String SUFFIX_UPDATE_CONTRACT = "/update-temp-contracts";
        private static final String SUFFIX = ".jar";

        @ContractStateStore
        ReadWriterStore<String, JsonObject> state;

        @ContractTransactionReceipt
        TransactionReceipt txReceipt;

        @InvokeTransaction
        public TransactionReceipt updateProposer(JsonObject params) {
            // TODO 컨트랙트 조회
            VersioningContractStateValue stateValue;
            try {
                stateValue = VersioningContractStateValue.of(txReceipt.getTxId());
                stateValue.init();

                String upgradeContract = params.get("contract").getAsString();
                byte[] binaryFile = base64Dec(upgradeContract.getBytes("UTF-8"));

                if (!validatorVerify() || (binaryFile == null) || !sizeVerify(binaryFile)) {
                    return txReceipt;
                }

                try {
                    writeTempContract(stateValue, binaryFile, params);
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

        private void setStateValue(VersioningContractStateValue stateValue
                , byte[] contractBinary, JsonObject params) {
            if (params.has("votePeorid")) {
                stateValue.setBlockHeight(txReceipt.getBlockHeight(),
                        params.get("votePeorid").getAsLong());
            } else {
                stateValue.setBlockHeight(txReceipt.getBlockHeight());
            }
            stateValue.setTargetContractVersion(params.get("contractVersion").getAsString());
            stateValue.setUpdateContract(contractBinary);
            //TODO set validatoreSet in branch store
            Set<String> validatorSet = new HashSet<>();
            JsonObject validators = state.get("validatorSet");
            for (String v : validators.keySet()) {
                validatorSet.add(v);
            }
            stateValue.setVotable(txReceipt.getIssuer(), validatorSet);
        }

        private void writeTempContract(VersioningContractStateValue stateValue, byte[] binaryFile, JsonObject params) {
            try {
                setStateValue(stateValue, binaryFile, params);
                FileOutputStream fos;
                Path path = Paths.get(System.getProperty("user.dir"));
                String tempContractPath = String.format("%s/.yggdrash/contract/%s", path.getParent(), SUFFIX_UPDATE_CONTRACT);

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
            } catch (Exception e) {
                log.error(e.toString());
                txReceipt.setStatus(ExecuteStatus.FALSE);
            }
        }

        @InvokeTransaction
        public TransactionReceipt vote(JsonObject params) {


            txReceipt.setStatus(ExecuteStatus.FALSE);
            VersioningContractStateValue stateValue;
            try {
                ContractVote contractVote = JsonUtil.generateJsonToClass(params.toString(), ContractVote.class);
                stateValue = VersioningContractStateValue.of(getContractSet(contractVote.getTxId()));
                Long targetBlockHeight = stateValue.getContractSet().getTargetBlockHeight();
                Long currentBlockHeight = txReceipt.getBlockHeight();

                if (currentBlockHeight > targetBlockHeight ) {
                    //TODO remove temp contract
                    txReceipt.setStatus(ExecuteStatus.FALSE);
                    return txReceipt;
                }

                //TODO updatable 이면 컨트랙트 파일 이동
//            if (stateValue.getContractSet().isUpgradable()) {
//
//            } else {

//            }

                setVote(stateValue, contractVote, txReceipt.getIssuer());

                state.put(contractVote.getTxId(),
                        stateValue.getJson().get(contractVote.getTxId()).getAsJsonObject());
                txReceipt.setStatus(ExecuteStatus.SUCCESS);
                log.info("[Contract | Vote] Possible Upgrade  => " +
                        stateValue.getJson().get(contractVote.getTxId()).getAsJsonObject().get("upgradable"));
                log.info("[Contract | Vote] Contract State => " +
                        stateValue.getJson().get(contractVote.getTxId()).getAsJsonObject().get("votedState"));
            } catch (Exception e) {
                txReceipt.setStatus(ExecuteStatus.FALSE);
                log.warn("Failed to vote = {}", params);
            }
            return txReceipt;
        }

        private void setVote(VersioningContractStateValue stateValue, ContractVote contractVote, String issuer) {
            stateValue.voting(contractVote, issuer);
        }

        @ContractQuery
        public ContractSet updateStatus(JsonObject params) {
            ContractSet contractSet = null;
            JsonObject json = state.get(params.get("txId").getAsString());
            if (json != null) {
                contractSet = JsonUtil.generateJsonToClass(json.toString(), ContractSet.class);
            }
            return contractSet;
        }

        private boolean validatorVerify() {
            //TODO change get validator in branch store
            JsonObject validators = state.get("validatorSet");
            for (String v : validators.keySet()) {
                if (!v.isEmpty() && v.equals(txReceipt.getIssuer())) {
                    return true;
                }
            }
            return false;
        }

        private ContractSet getContractSet(String txId) {
            ContractSet contractSet = null;
            JsonObject json = state.get(txId);
            if (json != null) {
                contractSet = JsonUtil.generateJsonToClass(json.toString(), ContractSet.class);
            }
            return contractSet;
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
