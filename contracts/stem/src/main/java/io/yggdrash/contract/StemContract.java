
package io.yggdrash.contract;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.yggdrash.common.contract.Contract;
import io.yggdrash.common.contract.standard.CoinStandard;
import io.yggdrash.common.contract.vo.PrefixKeyEnum;
import io.yggdrash.common.crypto.ECKey;
import io.yggdrash.common.crypto.HashUtil;
import io.yggdrash.common.crypto.HexUtil;
import io.yggdrash.common.store.BranchStateStore;
import io.yggdrash.common.utils.BranchUtil;
import io.yggdrash.common.utils.ByteUtil;
import io.yggdrash.common.utils.JsonUtil;
import io.yggdrash.contract.core.ExecuteStatus;
import io.yggdrash.contract.core.TransactionReceipt;
import io.yggdrash.contract.core.annotation.ContractBranchStateStore;
import io.yggdrash.contract.core.annotation.ContractQuery;
import io.yggdrash.contract.core.annotation.ContractStateStore;
import io.yggdrash.contract.core.annotation.ContractTransactionReceipt;
import io.yggdrash.contract.core.annotation.Genesis;
import io.yggdrash.contract.core.annotation.InvokeTransaction;
import io.yggdrash.contract.core.store.ReadWriterStore;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import static io.yggdrash.common.config.Constants.BRANCH_ID;


public class StemContract implements BundleActivator, ServiceListener {
    private static final Logger log = LoggerFactory.getLogger(StemContract.class);
    //private ServiceRegistration registration;

    // Get other Service
    private CoinStandard asset;

    @Override
    public void start(BundleContext context) {
        log.info("Start stem contract");

        //Find for service in another bundle
        Hashtable<String, String> props = new Hashtable<>();
        props.put("YGGDRASH", "Stem");
        context.registerService(StemService.class.getName(), new StemService(), props);
        //Register our service in the bundle context using the name.
        //registration = context.registerService(StemService.class.getName(), new StemService(), props);
    }

    @Override
    public void stop(BundleContext context) {
        log.info("Stop stem contract");
        //TODO Why don't unregister the service?
        //registration.unregister();
    }

    @Override
    public void serviceChanged(ServiceEvent event) {
        // get YEED contract in this
        //
    }

    public void setAsset(CoinStandard coinStandard) {
        this.asset = coinStandard;
    }


    public static class StemService implements Contract {
        private static final Logger log = LoggerFactory.getLogger(StemContract.class);

        @ContractBranchStateStore
        BranchStateStore branchStateStore;

        @ContractStateStore
        ReadWriterStore<String, JsonObject> state;


        @ContractTransactionReceipt
        TransactionReceipt txReceipt;

        @Genesis
        @InvokeTransaction // TODO remove InvokeTransaction
        public TransactionReceipt init(JsonObject param) {
            log.info("[StemContract | genesis] SUCCESS!");
            return txReceipt;
        }

        /**
         * Returns the id of a registered branch
         *
         * @param params branch   : The branch.json to register on the stem
         */
        @InvokeTransaction
        public void create(JsonObject params) {
            // TODO store branch
            // Validate branch spec
            // params

            JsonObject branch = params.getAsJsonObject("branch");


            // branch verify
            if (!branchVerify(branch)) {
                this.txReceipt.setStatus(ExecuteStatus.FALSE);
                this.txReceipt.addLog("Branch had not verified");
                return;
            }

            // calculation branch Id
            String branchId = HexUtil.toHexString(BranchUtil.branchIdGenerator(branch));
            log.debug("branchId : {}", branchId);
            // prefix Branch_id

            String governanceContractName = branch.get("governanceContract").getAsString();

            boolean save = saveBranch(branchId, branch);
            if (!save) {
                this.txReceipt.setStatus(ExecuteStatus.FALSE);
                return;
            }

            // get Validator
            Set<JsonObject> branchContracts = getContract(branchId);
            JsonObject governanceContract = null;
            for (JsonObject contract: branchContracts) {
                if (governanceContractName.equals(contract.get("name").getAsString())) {
                    governanceContract = contract;
                    break;
                }
            }

            if (governanceContract == null) {
                this.txReceipt.setStatus(ExecuteStatus.FALSE);
                this.txReceipt.addLog("Branch has no governanceContract");
                return;
            }

            JsonArray validators = governanceContract
                    .get("init")
                    .getAsJsonObject()
                    .get("validators")
                    .getAsJsonArray();

            // validators verify
            Set<String> validatorSet = new HashSet<>();
            for (JsonElement validator : validators) {
                String validatorString = validator.getAsString();
                validatorSet.add(validatorString);
                // check validator is address
                if (HexUtil.addressStringToBytes(validatorString) == ByteUtil.EMPTY_BYTE_ARRAY) {
                    this.txReceipt.setStatus(ExecuteStatus.FALSE);
                    this.txReceipt.addLog(String.format("validator %s is not account", validatorString));
                    return;
                }
            }

            // check validator is unique in list
            if (validatorSet.size() != validators.size()) {
                this.txReceipt.setStatus(ExecuteStatus.FALSE);
                this.txReceipt.addLog("validator list is unique accounts list");
                return;
            }
            log.debug(" is validator contain {}",validatorSet.contains(txReceipt.getIssuer()));
            log.debug(" is branchStateStore contain {}",branchStateStore.isValidator(txReceipt.getIssuer()));
            // Check issuer is validator or yggdrash validator
            if (!(validatorSet.contains(txReceipt.getIssuer())
                    || branchStateStore.isValidator(txReceipt.getIssuer()))) {
                // Check issuer is not yggdrash validator

                this.txReceipt.setStatus(ExecuteStatus.FALSE);
                this.txReceipt.addLog("Issuer is not branch validator");
                return;
            }

            JsonObject validatorObject = new JsonObject();
            validatorObject.add("validators", validators);

            saveValidators(branchId, validatorObject);

            // get meta information
            JsonObject branchCopy = branch.deepCopy();

            // get branch Meta information
            branchCopy.remove("contracts");
            branchCopy.remove("consensus");

            // save Meta information
            saveBranchMeta(branchId, branchCopy);

            // check fee
            // check fee govonence
            // get validator
            // get meta information

            this.txReceipt.setStatus(ExecuteStatus.SUCCESS);
            this.txReceipt.addLog(String.format("Branch %s is created", branchId));


        }

        /**
         * Returns the id of a updated branch
         *
         * @param params branchId The Id of the branch to update
         *               branch   The branch.json to update on the stem
         */
        @InvokeTransaction
        public void update(JsonObject params) {
            // TODO update branch meta information

            // get branch id
            String branchId = params.get("branchId").getAsString();

            // check branchId Exist

            // check branch validator
            if (!checkBranchValidators(branchId)) {
                // Transaction Issuer is not validator
                txReceipt.setStatus(ExecuteStatus.FALSE);
                txReceipt.addLog("Issuer is not branch validator");
                return;
            }

            JsonObject updateBranch = params.get("branch").getAsJsonObject();
            JsonObject originBranchMeta = getBranchMeta(branchId);

            if (originBranchMeta == null) {
                txReceipt.setStatus(ExecuteStatus.FALSE);
                txReceipt.addLog("branch is not exist");
            } else {
                originBranchMeta = metaMerge(originBranchMeta, updateBranch);
                // save branch meta information
                saveBranchMeta(branchId, originBranchMeta);
                txReceipt.setStatus(ExecuteStatus.SUCCESS);
            }
        }

        @InvokeTransaction
        public void transferBranch(JsonObject params) {
            // TODO transfer Yeed to Branch
            String branchId = params.get(BRANCH_ID).getAsString();
        }

        @InvokeTransaction
        public void withdrawBranch(JsonObject params) {
            // TODO withdraw Yeed from Branch

            String branchId = params.get(BRANCH_ID).getAsString();
            // check branch validator
            if (!checkBranchValidators(branchId)) {
                // Transaction Issuer is not validator
                txReceipt.setStatus(ExecuteStatus.FALSE);
                txReceipt.addLog("Issuer is not branch validator");
                return;
            }

        }


        @InvokeTransaction
        public void updateValidator(JsonObject params) {
            // TODO update branch meta information

            // get branch id
            String branchId = params.get("branchId").getAsString();
            Long blockHeight = params.get("blockHeight").getAsLong();
            String proposer = params.get("proposer").getAsString();
            String targetValidator = params.get("targetValidator").getAsString();
            StemOperation operatingFlag = StemOperation.fromValue(params.get("operatingFlag").getAsString());

            // Get Validator Set
            JsonObject validators = getValidators(branchId);

            if (validators == null) {
                txReceipt.setStatus(ExecuteStatus.FALSE);
                txReceipt.addLog("validator is not exist");
                return;
            }
            JsonArray validatorArray = validators.get("validators").getAsJsonArray();
            Set<String> validatorSet = JsonUtil.convertJsonArrayToSet(validatorArray);
            // check is branch validator
            if (!validatorSet.contains(txReceipt.getIssuer())) {
                txReceipt.setStatus(ExecuteStatus.FALSE);
                txReceipt.addLog("issuer is not validator");
                return;
            }

            byte[] message = ByteUtil.merge(
                    HexUtil.hexStringToBytes(branchId),
                    ByteUtil.longToBytes(blockHeight),
                    HexUtil.hexStringToBytes(proposer),
                    HexUtil.hexStringToBytes(targetValidator),
                    operatingFlag.toValue().getBytes(StandardCharsets.UTF_8)
            );
            JsonArray signed = params.get("signed").getAsJsonArray();

            // all message is sha3hashed
            message = HashUtil.sha3(message);

            int voteCount = (int) Math.ceil(1.0 * validatorSet.size() * 2 / 3);
            int vote = 0;
            log.debug("vote count {}", voteCount);

            // verify signed
            List<String> signedList = JsonUtil.convertJsonArrayToStringList(signed);
            // for check validator set
            Set<String> checkValidator = new HashSet<>();
            checkValidator.addAll(validatorSet);

            // check branch validators vote[]
            for (String sign : signedList) {
                // TODO move signature to contract core
                byte[] signatureArray = HexUtil.hexStringToBytes(sign);
                ECKey.ECDSASignature signature = new ECKey.ECDSASignature(signatureArray);
                int realV = signatureArray[0] - 27;
                byte[] address = ECKey.recoverAddressFromSignature(realV, signature, message);
                String addressHexString = HexUtil.toHexString(address);
                // check validator set
                if (checkValidator.contains(addressHexString)) {
                    vote++;
                    checkValidator.remove(addressHexString);
                }
            }
            checkValidator.clear();

            if (vote < voteCount) {
                // 정족수 부족
                txReceipt.setStatus(ExecuteStatus.FALSE);
                txReceipt.addLog("Lack of quorum");
                return;
            }


            if (operatingFlag == StemOperation.ADD_VALIDATOR) {
                // add Validator
                // check validator is exist
                if (validatorSet.contains(targetValidator)) {
                    txReceipt.setStatus(ExecuteStatus.FALSE);
                    txReceipt.addLog("Target validator is exist in validator set");
                    return;
                }
                // add Validator
                validatorArray.add(targetValidator);
                JsonObject validatorList = new JsonObject();
                validatorList.add("validators", validatorArray);
                saveValidators(branchId, validatorList);
                txReceipt.setStatus(ExecuteStatus.SUCCESS);
                txReceipt.addLog("new validator add in branch");
                return;

            } else if (operatingFlag == StemOperation.REMOVE_VALIDATOR) {
                // remove validator
                if (!validatorSet.contains(targetValidator)) {
                    txReceipt.setStatus(ExecuteStatus.FALSE);
                    txReceipt.addLog("Target validator is not exist in validator set");
                    return;
                }
                validatorSet.remove(targetValidator);
                validatorArray = JsonUtil.convertCollectionToJsonArray(validatorSet);
                JsonObject validatorList = new JsonObject();
                validatorList.add("validators", validatorArray);
                saveValidators(branchId, validatorList);
                txReceipt.setStatus(ExecuteStatus.SUCCESS);
                txReceipt.addLog("validator remove in branch");
                return;

            } else if (operatingFlag == StemOperation.REPLACE_VALIDATOR) {
                // replace validator
                // param get validator list
                // remove proposer
                // add targetValidator
                // check proposer exist and targetValidator not exist
                if (!validatorSet.contains(proposer) || validatorSet.contains(targetValidator)) {
                    // propser
                    txReceipt.setStatus(ExecuteStatus.FALSE);
                    txReceipt.addLog("proposer not exist validator set or new validator exist validator set");
                    return;
                }
                // remove and add
                validatorSet.remove(proposer);
                validatorSet.add(targetValidator);

                // save validator set
                JsonObject validatorList = new JsonObject();
                validatorArray = JsonUtil.convertCollectionToJsonArray(validatorSet);
                validatorList.add("validators", validatorArray);
                saveValidators(branchId, validatorList);
                txReceipt.setStatus(ExecuteStatus.SUCCESS);
                txReceipt.addLog(String.format("validator replace %s to %s", proposer, targetValidator));
                return;

            } else if (operatingFlag == StemOperation.UPDATE_VALIDATOR_SET) {
                // update all validator set
                JsonObject newValidatorSet = new JsonObject();
                JsonArray validatorList = params.get("validators").getAsJsonArray();
                newValidatorSet.add("validators", validatorList);

                // target validator is list hash(sha3ommit12)
                byte[] validatorsByteArray = newValidatorSet.toString().getBytes(StandardCharsets.UTF_8);
                byte[] validatorsSha3 = HashUtil.sha3omit12(validatorsByteArray);
                String calculateTargetValidator = HexUtil.toHexString(validatorsSha3);

                // check and verify validatorList
                if (!targetValidator.equals(calculateTargetValidator)) {
                    txReceipt.setStatus(ExecuteStatus.FALSE);
                    txReceipt.addLog("target Validator set is invalid");
                    return;
                }

                // update Validator set
                saveValidators(branchId, newValidatorSet);

                txReceipt.setStatus(ExecuteStatus.SUCCESS);
                txReceipt.addLog("validator set change");
                return;
            }


            // save branch validator set information

        }

        private boolean saveBranch(String branchId, JsonObject branch) {
            String branchIdKey = String.format("%s%s", PrefixKeyEnum.STEM_BRANCH, branchId);

            if (!this.state.contains(branchIdKey)) {
                this.state.put(branchIdKey, branch);
                return true;
            }
            return false;
        }

        /**
         * @param params branch id
         *
         * @return branch json object
         */
        @ContractQuery
        public JsonObject getBranch(JsonObject params) {
            // TODO get branch information
            String branchId = params.get(BRANCH_ID).getAsString();
            JsonObject branch = getBranch(branchId);

            // TODO fee not enough mesaage
            return branch;
        }

        public JsonObject getBranch(String branchId) {
            String branchIdKey = String.format("%s%s", PrefixKeyEnum.STEM_BRANCH, branchId);
            return this.state.get(branchIdKey);
        }

        private void saveBranchMeta(String branchId, JsonObject branchMeta) {
            String branchMetaKey = String.format("%s%s", PrefixKeyEnum.STEM_META, branchId);
            this.state.put(branchMetaKey, branchMeta);
        }

        @ContractQuery
        public JsonObject getBranchMeta(JsonObject param) {
            String branchId = param.get(BRANCH_ID).getAsString();

            return getBranchMeta(branchId);
        }

        public JsonObject getBranchMeta(String branchId) {
            String branchMetaKey = String.format("%s%s", PrefixKeyEnum.STEM_META, branchId);
            return this.state.get(branchMetaKey);
        }

        private void saveValidators(String branchId, JsonObject validators) {
            String branchValidatorKey = String.format("%s%s", PrefixKeyEnum.STEM_BRANCH_VALIDATOR, branchId);
            this.state.put(branchValidatorKey, validators);
        }

        public JsonObject getValidators(String branchId) {
            String branchValidatorKey = String.format("%s%s", PrefixKeyEnum.STEM_BRANCH_VALIDATOR, branchId);
            return this.state.get(branchValidatorKey);
        }


        public JsonObject metaMerge(JsonObject branchMeta, JsonObject branchMetaUpdate) {
            // update 가능 항목
            List<String> keyList = Arrays.asList(new String[]{"description"});
            keyList.stream().forEach(key -> {
                if (branchMeta.has(key) && branchMetaUpdate.has(key)) {
                    branchMeta.addProperty(key, branchMetaUpdate.get(key).getAsString());
                }
            });

            return branchMeta;
        }

        /**
         * Returns boolean
         *
         * @param branchId
         * */
        public void messageCall(String branchId) {
            // TODO message call to contract
            // TODO isEnoughFee
        }

        /**
         * @param params branch id
         *
         * @return contract json object
         */
        @ContractQuery
        public Set<JsonObject> getContract(JsonObject params) {
            String branchId = params.get(BRANCH_ID).getAsString();
            return getContract(branchId);
        }

        public Set<JsonObject> getContract(String branchId) {
            Set<JsonObject> contractSet = new HashSet<>();

            JsonObject branch = getBranch(branchId);
            if (branch != null) {
                JsonArray contracts = branch.get("contracts").getAsJsonArray();
                for (JsonElement c : contracts) {
                    contractSet.add(c.getAsJsonObject());
                }
            }
            return contractSet;
        }

        /**
         * @param params branch id
         *
         * @return fee state
         */
        @ContractQuery
        public BigInteger feeState(JsonObject params) {
            String branchId = params.get(BRANCH_ID).getAsString();
            // TODO get branch feeState and calculate
            return BigInteger.ZERO;

        }

        private boolean branchVerify(JsonObject branch) {
            // check property
            boolean verify = true;
            List<String> existProperty = Arrays.asList(
                    new String[]{"name", "symbol", "property", "contracts", "governanceContract"});

            for (String perpertyKey :existProperty) {
                verify &= branch.get(perpertyKey).isJsonNull() != true;
            }

            return verify;
        }

        private boolean checkBranchValidators(String branchId) {
            // check branch validator
            JsonObject validators = getValidators(branchId);
            // check issuer
            if (validators == null || !validators.toString().contains(txReceipt.getIssuer())) {
                return false;
            }
            return true;
        }

    }
}