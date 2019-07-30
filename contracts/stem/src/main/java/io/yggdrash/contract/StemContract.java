
package io.yggdrash.contract;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.yggdrash.common.contract.Contract;
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
import io.yggdrash.contract.core.annotation.ContractChannelField;
import io.yggdrash.contract.core.annotation.ContractQuery;
import io.yggdrash.contract.core.annotation.ContractStateStore;
import io.yggdrash.contract.core.annotation.ContractTransactionReceipt;
import io.yggdrash.contract.core.annotation.Genesis;
import io.yggdrash.contract.core.annotation.InvokeTransaction;
import io.yggdrash.contract.core.channel.ContractChannel;
import io.yggdrash.contract.core.channel.ContractMethodType;
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
        // Get YEED contract in this
    }

    public static class StemService implements Contract {
        private static final Logger log = LoggerFactory.getLogger(StemContract.class);

        @ContractBranchStateStore
        BranchStateStore branchStateStore;

        @ContractStateStore
        ReadWriterStore<String, JsonObject> state;


        @ContractTransactionReceipt
        TransactionReceipt txReceipt;

        @ContractChannelField
        public ContractChannel channel;


        @Genesis
        @InvokeTransaction // TODO remove InvokeTransaction
        public TransactionReceipt init(JsonObject params) {
            log.info("[StemContract | genesis] SUCCESS!");

            // TODO save yeed contract version
            // TODO set yeed contract version interface

            return txReceipt;
        }

        /**
         * Returns the id of a registered branch
         *
         * @param params branch   : The branch.json to register on the stem
         */
        @InvokeTransaction
        public void create(JsonObject params) { // Create branch
            BigInteger serviceFee = params.get("serviceFee").getAsBigInteger();

            // Verify if the issuer can pay the serviceFee
            if (!isBalanceEnough(txReceipt.getIssuer(), serviceFee)) {
                setErrorTxReceipt("Insufficient funds");
                return;
            }

            JsonObject branch = params.getAsJsonObject("branch");

            // Validate branch spec
            if (!branchVerify(branch)) {
                setFalseTxReceipt("Branch had not verified");
                return;
            }

            // Calculate branch Id
            String branchId = HexUtil.toHexString(BranchUtil.branchIdGenerator(branch));
            log.debug("Create :: branchId = {}", branchId);

            // Store branch
            boolean save = saveBranch(branchId, branch);
            if (!save) {
                setFalseTxReceipt("Branch already exists");
                return;
            }

            String governanceContractName = branch.get("governanceContract").getAsString(); // DPoA

            // Verify the branch has the governance contract corresponding to contract name
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

            // Get validators from governance contract
            JsonArray validators = governanceContract
                    .get("init")
                    .getAsJsonObject()
                    .get("validators")
                    .getAsJsonArray();

            // Verify that validators are valid address format
            Set<String> validatorSet = new HashSet<>();
            for (JsonElement validator : validators) {
                String validatorString = validator.getAsString();
                validatorSet.add(validatorString);
                if (HexUtil.addressStringToBytes(validatorString) == ByteUtil.EMPTY_BYTE_ARRAY) {
                    setFalseTxReceipt(String.format("validator %s is not account", validatorString));
                    return;
                }
            }

            // Check validator is unique in list
            if (validatorSet.size() != validators.size()) {
                setFalseTxReceipt("validator list is unique accounts list");
                return;
            }

            log.debug("The validatorSet contains issuer : {}", validatorSet.contains(txReceipt.getIssuer()));
            log.debug("the branchStateStore contains issuer : {}", branchStateStore.isValidator(txReceipt.getIssuer()));

            // Check issuer is validator or yggdrash validator
            if (!(validatorSet.contains(txReceipt.getIssuer())
                    || branchStateStore.isValidator(txReceipt.getIssuer()))) {
                // Check issuer is not yggdrash validator
                setFalseTxReceipt("Issuer is not branch validator");
                return;
            }

            JsonObject validatorObject = new JsonObject();
            validatorObject.add("validators", validators);

            // Store the validators of branch with its branchId
            saveValidators(branchId, validatorObject);

            // Copy metadata for a branch
            JsonObject branchCopy = branch.deepCopy();
            branchCopy.remove("contracts");
            branchCopy.remove("consensus");

            // Save metadata for a branch
            saveBranchMeta(branchId, branchCopy);

            //branchStateStore.getBranchContacts().stream().filter()
            // TODO check serviceFee governance

            // Attempt to serviceFee transfer to stemContract
            boolean transferResult = transferFee(serviceFee);
            if (!transferResult) {
                setErrorTxReceipt(String.format("Service fee transfer failed. Branch %s is not created", branchId));
                return;
            }

            // StemContract stores the serviceFee state for each branchId.
            incrFeeState(branchId, serviceFee);
            setSuccessTxReceipt(String.format("Branch %s is created", branchId));
        }

        /**
         * Returns the id of a updated branch
         *
         * @param params branchId The Id of the branch to update
         *               branch   The branch.json to update on the stem
         */
        @InvokeTransaction
        public void update(JsonObject params) { // Update branch metadata
            BigInteger serviceFee = params.get("serviceFee").getAsBigInteger();

            // Verify if the issuer can pay the serviceFee
            if (!isBalanceEnough(txReceipt.getIssuer(), serviceFee)) {
                setErrorTxReceipt("Insufficient funds");
                return;
            }

            String branchId = params.get("branchId").getAsString();

            // Check branchId Exist
            if (!isBranchExists(branchId)) {
                setErrorTxReceipt("Branch not exists");
                return;
            }

            // Verify that the transaction issuer is a validator

            if (!checkBranchValidators(branchId)) {
                setFalseTxReceipt("Issuer is not branch validator");
                return;
            }

            JsonObject originBranchMeta = getBranchMeta(branchId);
            if (originBranchMeta == null) {
                setFalseTxReceipt("Branch metadata not exists");
                return;
            }

            // Transfer serviceFee to StemContract from issuer
            boolean transferResult = transferFee(serviceFee);
            if (!transferResult) {
                setErrorTxReceipt(String.format("Service fee transfer failed. Branch %s is not updated", branchId));
                return;
            }

            // Update the serviceFee state for this branchId
            incrFeeState(branchId, serviceFee);

            JsonObject updateBranch = params.get("branch").getAsJsonObject();
            originBranchMeta = metaMerge(originBranchMeta, updateBranch);
            // Update branch metadata
            saveBranchMeta(branchId, originBranchMeta);
            setSuccessTxReceipt(String.format("Branch %s is updated", branchId));
        }

        @InvokeTransaction
        public void deposit(JsonObject param) { // Deposit yeed to branch
            // TODO check serviceFee governance

            String issuer = txReceipt.getIssuer();
            String from = param.has("from") ? param.get("from").getAsString() : issuer;
            BigInteger amount = param.get("amount").getAsBigInteger();

            boolean result = !from.equals(issuer)
                    ? callTransferFromChannel(createDepositParam(from, amount))
                    : callTransferChannel(createDepositParam(from, amount));

            if (!result) {
                setFalseTxReceipt(String.format("%s deposit failed", from));
                return;
            }

            incrFeeState(txReceipt.getBranchId(), amount);
            setSuccessTxReceipt(String.format("%s deposit completed successfully", from));
        }

        @InvokeTransaction
        public void withdraw(JsonObject params) { // Withdraw the serviceFee(yeed) for the branchId by validators
            // TODO check serviceFee governance

            String branchId = txReceipt.getBranchId();

            // Verify that the transaction issuer is a validator
            if (!checkBranchValidators(branchId)) {
                setFalseTxReceipt("Issuer is not branch validator");
                return;
            }

            String to = txReceipt.getIssuer();
            BigInteger amount = params.get("amount").getAsBigInteger();

            // Check availability of withdrawals
            if (!isWithdrawalAvailable(amount)) {
                setFalseTxReceipt("Insufficient funds of stem contract");
                return;
            }

            boolean transferResult = callTransferChannel(createWithdrawParam(to, amount));
            if (!transferResult) {
                setFalseTxReceipt("Withdraw failed");
                return;
            }

            decrFessState(branchId, amount);
            setSuccessTxReceipt("Withdraw completed successfully");
        }

        @InvokeTransaction
        public void updateValidator(JsonObject params) { // Update validators that are branch metadata
            // TODO check serviceFee governance

            String branchId = params.get("branchId").getAsString();
            Long blockHeight = params.get("blockHeight").getAsLong();
            String proposer = params.get("proposer").getAsString();
            String targetValidator = params.get("targetValidator").getAsString();
            StemOperation operatingFlag = StemOperation.fromValue(params.get("operatingFlag").getAsString());

            // Get Validator Set from state
            JsonObject validators = getValidators(branchId);
            if (validators == null) {
                setFalseTxReceipt("Validators not exist");
                return;
            }

            JsonArray validatorArray = validators.get("validators").getAsJsonArray();
            Set<String> validatorSet = JsonUtil.convertJsonArrayToSet(validatorArray);
            // Verify that issuer is a branch validator
            if (!validatorSet.contains(txReceipt.getIssuer())) {
                setFalseTxReceipt("Issuer is not a validator");
                return;
            }

            byte[] message = ByteUtil.merge(
                    HexUtil.hexStringToBytes(branchId),
                    ByteUtil.longToBytes(blockHeight),
                    HexUtil.hexStringToBytes(proposer),
                    HexUtil.hexStringToBytes(targetValidator),
                    operatingFlag.toValue().getBytes(StandardCharsets.UTF_8)
            );
            message = HashUtil.sha3(message); // All message is sha3hashed

            int voteCount = (int) Math.ceil(1.0 * validatorSet.size() * 2 / 3);
            int vote = 0;
            log.debug("Vote count {}", voteCount);


            // Check the validator set that issuer wants to update
            Set<String> checkValidator = new HashSet<>();
            checkValidator.addAll(validatorSet);
            JsonArray signed = params.get("signed").getAsJsonArray();
            List<String> signedList = JsonUtil.convertJsonArrayToStringList(signed);

            // Verify signatures of validators
            for (String sign : signedList) {
                // TODO move signature to contract core
                byte[] signatureArray = HexUtil.hexStringToBytes(sign);
                ECKey.ECDSASignature signature = new ECKey.ECDSASignature(signatureArray);
                int realV = signatureArray[0] - 27;
                byte[] address = ECKey.recoverAddressFromSignature(realV, signature, message);
                String addressHexString = HexUtil.toHexString(address);
                // Count the votes by verifying that the validator that signed the message exists
                if (checkValidator.contains(addressHexString)) {
                    vote++;
                    checkValidator.remove(addressHexString);
                }
            }
            checkValidator.clear();

            // Check the validators votes
            if (vote < voteCount) {
                setFalseTxReceipt("Lack of quorum");
                return;
            }

            if (operatingFlag == StemOperation.ADD_VALIDATOR) {
                // Verify that targetValidator already exists
                if (validatorSet.contains(targetValidator)) {
                    setFalseTxReceipt("Target validator already exists in validator set");
                    return;
                }
                // Add targetValidator to the list of validator set
                validatorArray.add(targetValidator);

                saveValidators(branchId, createValidatorsJsonObj(validatorArray));
                setSuccessTxReceipt("A new validator added to the branch");
            } else if (operatingFlag == StemOperation.REMOVE_VALIDATOR) {
                // Verify that targetValidator already exists
                if (!validatorSet.contains(targetValidator)) {
                    setFalseTxReceipt("The target validator not exists in validator set");
                    return;
                }
                // Remove targetValidator from the list of validator set
                validatorSet.remove(targetValidator);
                validatorArray = JsonUtil.convertCollectionToJsonArray(validatorSet);

                saveValidators(branchId, createValidatorsJsonObj(validatorArray));
                setSuccessTxReceipt("The validator removed from the branch");
            } else if (operatingFlag == StemOperation.REPLACE_VALIDATOR) {
                // Verify proposer exists and targetValidator not exists
                if (!validatorSet.contains(proposer) || validatorSet.contains(targetValidator)) {
                    setFalseTxReceipt("Proposer not exists or new validator already exists in validator set");
                    return;
                }

                validatorSet.remove(proposer);
                validatorSet.add(targetValidator);
                validatorArray = JsonUtil.convertCollectionToJsonArray(validatorSet);

                saveValidators(branchId, createValidatorsJsonObj(validatorArray));
                setSuccessTxReceipt(String.format("Replaced validator %s to %s", proposer, targetValidator));
            } else if (operatingFlag == StemOperation.UPDATE_VALIDATOR_SET) {
                // Update entire validator set
                JsonArray validatorList = params.get("validators").getAsJsonArray();
                JsonObject newValidatorSet = createValidatorsJsonObj(validatorList);

                // TargetValidator is a hash of all validators by sha3omit12
                byte[] validatorsByteArray = newValidatorSet.toString().getBytes(StandardCharsets.UTF_8);
                byte[] validatorsSha3 = HashUtil.sha3omit12(validatorsByteArray);
                String calculateTargetValidator = HexUtil.toHexString(validatorsSha3);

                // Verify validator list
                if (!targetValidator.equals(calculateTargetValidator)) {
                    setFalseTxReceipt("Invalid target validator set");
                    return;
                }

                saveValidators(branchId, newValidatorSet);
                setSuccessTxReceipt("Validator set all changed");
            }

            // TODO transfer serviceFee
            // TODO save feeState
        }

        /**
         * @param params branch id
         *
         * @return branch json object
         */
        @ContractQuery
        public JsonObject getBranch(JsonObject params) {
            String branchId = params.get(BRANCH_ID).getAsString();
            JsonObject branch = getBranch(branchId);

            // TODO ServiceFee not enough mesaage
            return branch;
        }

        public JsonObject getBranch(String branchId) {
            String branchIdKey = String.format("%s%s", PrefixKeyEnum.STEM_BRANCH, branchId);
            return this.state.get(branchIdKey);
        }

        public boolean isBranchExists(String branchId) {
            String branchIdKey = String.format("%s%s", PrefixKeyEnum.STEM_BRANCH, branchId);
            return this.state.contains(branchIdKey);
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

        public JsonObject getValidators(String branchId) {
            String branchValidatorKey = String.format("%s%s", PrefixKeyEnum.STEM_BRANCH_VALIDATOR, branchId);
            return this.state.get(branchValidatorKey);
        }

        public JsonObject metaMerge(JsonObject branchMeta, JsonObject branchMetaUpdate) {
            List<String> keyList = Arrays.asList(new String[] {"description"}); // Updatable properties
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

        private String getContractVersion(String branchId, String contractName) {
            return getContract(branchId).stream()
                    .filter(cObj -> cObj.get("name").getAsString().equals(contractName))
                    .findFirst()
                    .map(cObj -> cObj.get("contractVersion").getAsString())
                    .orElse("");
        }

        /**
         * @param params branch id
         *
         * @return serviceFee state for branch
         */
        @ContractQuery
        public BigInteger feeState(JsonObject params) {
            String branchId = params.get("branchId").getAsString();
            return getFeeState(branchId);
        }

        private void incrFeeState(String branchId, BigInteger amount) {
            JsonObject feeObj = new JsonObject();
            feeObj.addProperty("serviceFee", amount.add(getFeeState(branchId)));
            this.state.put(branchId, feeObj);
        }

        private void decrFessState(String branchId, BigInteger amount) {
            JsonObject feeObj = new JsonObject();
            feeObj.addProperty("serviceFee", getFeeState(branchId).subtract(amount));
            this.state.put(branchId, feeObj);
        }

        private BigInteger getFeeState(String branchId) {
            return this.state.contains(branchId)
                    ? this.state.get(branchId).get("serviceFee").getAsBigInteger() : BigInteger.ZERO;
        }

        private boolean transferFee(BigInteger serviceFee) {
            return callTransferChannel(createDepositParam(this.txReceipt.getIssuer(), serviceFee));
        }

        private boolean isWithdrawalAvailable(BigInteger amount) {
            BigInteger stemBalance = getContractBalance("STEM");
            return stemBalance.subtract(amount).compareTo(BigInteger.ZERO) >= 0;
        }

        private JsonObject createWithdrawParam(String to, BigInteger amount) {
            JsonObject param = new JsonObject();
            param.addProperty("from", "STEM");
            param.addProperty("to", to);
            param.addProperty("amount", amount);
            return param;
        }

        private JsonObject createDepositParam(String from, BigInteger amount) {
            JsonObject param = new JsonObject();
            param.addProperty("from", from);
            param.addProperty("to", "STEM");
            param.addProperty("amount", amount);
            return param;
        }

        private BigInteger getContractBalance(String contractName) {
            JsonObject param = new JsonObject();
            param.addProperty("contractName", contractName);

            // Get Contract Version in branch
            String yeedContractVersion = this.branchStateStore.getContractVersion("YEED");
            JsonObject result = this.channel.call(
                    yeedContractVersion, ContractMethodType.CHANNEL_METHOD, "getContractBalanceOf", param);

            return result.get("result").getAsBigInteger();
        }

        private boolean isBalanceEnough(String address, BigInteger serviceFee) {
            JsonObject param = new JsonObject();
            param.addProperty("address", address);
            param.addProperty("amount", serviceFee);

            // Get Contract Version in branch
            String yeedContractVersion = this.branchStateStore.getContractVersion("YEED");
            JsonObject result = this.channel.call(
                    yeedContractVersion, ContractMethodType.CHANNEL_METHOD, "isTransferable", param);

            return result.get("result").getAsBoolean();
        }

        public boolean callTransferFromChannel(JsonObject param) {
            // Get Contract Version in branch
            String yeedContractVersion = this.branchStateStore.getContractVersion("YEED");
            log.debug("YEED Contract {}", yeedContractVersion);
            JsonObject result = this.channel.call(
                    yeedContractVersion, ContractMethodType.CHANNEL_METHOD, "transferFromChannel", param);

            return result.get("result").getAsBoolean();
        }

        private boolean callTransferChannel(JsonObject param) {
            // Get Contract Version in branch
            String yeedContractVersion = this.branchStateStore.getContractVersion("YEED");
            log.debug("YEED Contract {}", yeedContractVersion);
            JsonObject result = this.channel.call(yeedContractVersion,
                    ContractMethodType.CHANNEL_METHOD, "transferChannel", param);

            return result.get("result").getAsBoolean();
        }

        private boolean branchVerify(JsonObject branch) {
            // Check property
            boolean verify = true;
            List<String> existProperty = Arrays.asList(
                    new String[]{"name", "symbol", "property", "contracts", "governanceContract"});

            for (String perpertyKey :existProperty) {
                verify &= branch.get(perpertyKey).isJsonNull() != true;
            }

            return verify;
        }

        private boolean saveBranch(String branchId, JsonObject branch) {
            String branchIdKey = String.format("%s%s", PrefixKeyEnum.STEM_BRANCH, branchId);

            if (!this.state.contains(branchIdKey)) {
                this.state.put(branchIdKey, branch);
                return true;
            }
            return false;
        }

        private void saveBranchMeta(String branchId, JsonObject branchMeta) {
            String branchMetaKey = String.format("%s%s", PrefixKeyEnum.STEM_META, branchId);
            this.state.put(branchMetaKey, branchMeta);
        }

        private void saveValidators(String branchId, JsonObject validators) {
            String branchValidatorKey = String.format("%s%s", PrefixKeyEnum.STEM_BRANCH_VALIDATOR, branchId);
            this.state.put(branchValidatorKey, validators);
        }

        private boolean checkBranchValidators(String branchId) {
            // Check branch validator
            JsonObject validators = getValidators(branchId);
            // Check issuer
            if (validators == null || !validators.toString().contains(txReceipt.getIssuer())) {
                return false;
            }
            return true;
        }

        private JsonObject createValidatorsJsonObj(JsonArray validatorArray) {
            JsonObject validatorList = new JsonObject();
            validatorList.add("validators", validatorArray);
            return validatorList;
        }

        private void setErrorTxReceipt(String msg) {
            this.txReceipt.setStatus(ExecuteStatus.ERROR);
            this.txReceipt.addLog(msg);
        }

        private void setFalseTxReceipt(String msg) {
            this.txReceipt.setStatus(ExecuteStatus.FALSE);
            this.txReceipt.addLog(msg);
        }

        private void setSuccessTxReceipt(String msg) {
            this.txReceipt.setStatus(ExecuteStatus.SUCCESS);
            this.txReceipt.addLog(msg);
        }

    }
}