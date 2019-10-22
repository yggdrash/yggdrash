
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
import io.yggdrash.contract.core.Receipt;
import io.yggdrash.contract.core.annotation.ContractBranchStateStore;
import io.yggdrash.contract.core.annotation.ContractChannelField;
import io.yggdrash.contract.core.annotation.ContractEndBlock;
import io.yggdrash.contract.core.annotation.ContractQuery;
import io.yggdrash.contract.core.annotation.ContractReceipt;
import io.yggdrash.contract.core.annotation.ContractStateStore;
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

    // Param properties
    private static final String TO = "to";
    private static final String FROM = "from";
    private static final String INIT = "init";
    private static final String NAME = "name";
    private static final String STEM = "STEM";
    private static final String YEED = "YEED";
    private static final String SYMBOL = "symbol";
    private static final String RESULT = "result";
    private static final String BRANCH = "branch";
    private static final String AMOUNT = "amount";
    private static final String SIGNED = "signed";
    private static final String ADDRESS = "address";
    private static final String PROPERTY = "property";
    private static final String PROPOSER = "proposer";
    private static final String CONTRACTS = "contracts";
    private static final String CONSENSUS = "consensus";
    private static final String SERVICE_FEE = "serviceFee";
    private static final String VALIDATORS = "validators";
    private static final String DESCRIPTION = "description";
    private static final String BLOCK_HEIGHT = "blockHeight";
    private static final String CONTRACT_NAME = "contractName";
    private static final String OPERATING_FLAG = "operatingFlag";
    private static final String IS_TRANSFERABLE = "isTransferable";
    private static final String CONTRACT_VERSION = "contractVersion";
    private static final String TRANSFER_CHANNEL = "transferChannel";
    private static final String TARGET_VALIDATOR = "targetValidator";
    private static final String GOVERNANCE_CONTRACT = "governanceContract";
    private static final String TRANSFER_FROM_CHANNEL = "transferFromChannel";
    private static final String GET_CONTRACT_BALANCE_OF = "getContractBalanceOf";

    // Log messages
    private static final String INIT_SUCCESS = "Stem init success";
    private static final String INVALID_AMOUNT = "Invalid amount";
    private static final String INVALID_PARAMS = "Invalid parameters";
    private static final String INVALID_BRANCH = "Invalid branch";
    private static final String INVALID_TARGET_VALIDATOR_SET = "Invalid target validator set";
    private static final String BRANCH_CREATED = "Branch %s is created";
    private static final String BRANCH_NOT_CREATED = "Create branch failed";
    private static final String BRANCH_UPDATED = "Branch %s is updated";
    private static final String BRANCH_NOT_UPDATED = "Update branch failed";
    private static final String BRANCH_ALREADY_EXISTS = "Branch %s already exists";
    private static final String BRANCH_NOT_EXISTS = "Branch not exists";
    private static final String BRANCH_META_NOT_EXISTS = "Branch metadata not exists";
    private static final String GOVERNANCE_CONTRACT_NOT_EXISTS = "Branch has no governanceContract";
    private static final String INSUFFICIENT_FUNDS = "Insufficient funds";
    private static final String INSUFFICIENT_FUNDS_OF_STEM = "Insufficient funds of stem contract";
    private static final String DEPOSIT_COMPLETED = "%s deposit completed successfully";
    private static final String DEPOSIT_FAILED = "%s deposit failed";
    private static final String WITHDRAW_COMPLETED = "Withdraw completed successfully";
    private static final String WITHDRAW_FAILED = "Withdraw to %s failed";
    private static final String SERVICE_FEE_TRANSFER_FAILED = "Service fee transfer failed";
    private static final String LACK_OF_QUORUM = "Lack of quorum";
    private static final String VALIDATOR_ADDED = "A new validator added to the branch";
    private static final String VALIDATOR_REMOVED = "The validator removed from the branch";
    private static final String VALIDATOR_REPLACED = "Replaced validator %s to %s";
    private static final String VALIDATORS_NOT_UPDATED = "Update validators failed";
    private static final String VALIDATORS_NOT_EXISTS = "Validators not exist";
    private static final String VALIDATOR_SET_ALL_CHANGED = "Validator set all changed";
    private static final String VALIDATOR_UNIQUE_ACCOUNT_LIST = "validator list is unique accounts list";
    private static final String VALIDATOR_IS_NOT_ACCOUNT = "validator %s is not account";
    private static final String ISSUER_IS_NOT_VALIDATOR = "Issuer is not a validator";
    private static final String ISSUER_IS_NOT_BRANCH_VALIDATOR = "Issuer is not branch validator";
    private static final String TARGET_VALIDATOR_ALREADY_EXISTS = "Target validator already exists in validator set";
    private static final String TARGET_VALIDATOR_NOT_EXISTS ="The target validator not exists in validator set";
    private static final String PROPOSER_OR_NEW_VALIDATOR_NOT_EXISTS = "Proposer not exists or new validator already exists in validator set";


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


        @ContractReceipt
        Receipt receipt;

        @ContractChannelField
        public ContractChannel channel;


        @Genesis
        @InvokeTransaction // TODO remove InvokeTransaction
        public Receipt init(JsonObject params) {
            log.info("[StemContract | genesis] SUCCESS!");
            setSuccessTxReceipt(INIT_SUCCESS);
            // TODO save yeed contract version
            // TODO set yeed contract version interface

            return receipt;
        }

        /**
         * Returns the id of a registered branch
         *
         * @param params branch   : The branch.json to register on the stem
         */
        @InvokeTransaction
        public void create(JsonObject params) { // Create branch
            try {
                BigInteger serviceFee = params.get(SERVICE_FEE).getAsBigInteger();
                JsonObject branch = params.getAsJsonObject(BRANCH);

                try {
                    checkBalance(serviceFee);
                    String branchId = saveBranch(branch);
                    transferFee(serviceFee);
                    // StemContract stores the serviceFee state for each branchId.
                    incrFeeState(branchId, serviceFee);
                    setSuccessTxReceipt(String.format(BRANCH_CREATED, branchId));
                } catch (Exception e) {
                    setFalseTxReceipt(BRANCH_NOT_CREATED);
                    setExceptionLog(e);
                    log.debug("Create Exception : {}", e.getMessage());
                }

            } catch (Exception e) {
                setFalseTxReceipt(INVALID_PARAMS);
                log.debug("{} : {}", INVALID_PARAMS, e.getMessage());
            }
        }

        private String saveBranch(JsonObject branch) throws RuntimeException {
            // Validate branch spec
            checkBranch(branch);

            // Calculate branch Id
            String branchId = HexUtil.toHexString(BranchUtil.branchIdGenerator(branch));
            if (isBranchExists(branchId)) {
                throw new RuntimeException(String.format(BRANCH_ALREADY_EXISTS, branchId));
            }

            saveBranch(branchId, branch);

            JsonObject governanceContract = getGovernanceContract(branch, branchId);
            if (governanceContract == null) {
                throw new RuntimeException(GOVERNANCE_CONTRACT_NOT_EXISTS);
            }

            // Get validators from governance contract
            JsonArray validators = governanceContract.get(INIT).getAsJsonObject().get(VALIDATORS).getAsJsonArray();
            Set<String> validatorSet = convertToValidatorSet(validators);
            // Check validator is unique in list
            if (validatorSet.size() != validators.size()) {
                throw new RuntimeException(VALIDATOR_UNIQUE_ACCOUNT_LIST);
            }

            if (!(validatorSet.contains(receipt.getIssuer()) || branchStateStore.isValidator(receipt.getIssuer()))) {
                // Check issuer is not yggdrash validator
                throw new RuntimeException(ISSUER_IS_NOT_BRANCH_VALIDATOR);
            }

            JsonObject validatorObject = new JsonObject();
            validatorObject.add(VALIDATORS, validators);
            // Store the validators of branch with its branchId
            saveValidators(branchId, validatorObject);

            // Copy metadata for a branch
            JsonObject branchCopy = branch.deepCopy();
            branchCopy.remove(CONTRACTS);
            branchCopy.remove(CONSENSUS);

            // Save metadata for a branch
            saveBranchMeta(branchId, branchCopy);

            return branchId;
        }

        private JsonObject getGovernanceContract(JsonObject branch, String branchId) {
            String governanceContractName = branch.get(GOVERNANCE_CONTRACT).getAsString(); // DPoA
            // Verify the branch has the governance contract corresponding to contract name
            Set<JsonObject> branchContracts = getContract(branchId);
            JsonObject governanceContract = null;
            for (JsonObject contract: branchContracts) {
                if (governanceContractName.equals(contract.get(NAME).getAsString())) {
                    governanceContract = contract;
                    break;
                }
            }
            return governanceContract;
        }

        private Set<String> convertToValidatorSet(JsonArray validators) {
            // Verify that validators are valid address format
            Set<String> validatorSet = new HashSet<>();
            for (JsonElement validator : validators) {
                String validatorString = validator.getAsString();
                validatorSet.add(validatorString);
                if (HexUtil.addressStringToBytes(validatorString) == ByteUtil.EMPTY_BYTE_ARRAY) {
                    setFalseTxReceipt(String.format(VALIDATOR_IS_NOT_ACCOUNT, validatorString));
                    return new HashSet<>();
                }
            }

            return validatorSet;
        }

        /**
         * Returns the id of a updated branch
         *
         * @param params branchId The Id of the branch to update
         *               branch   The branch.json to update on the stem
         */
        @InvokeTransaction
        public void update(JsonObject params) { // Update branch metadata
            try {
                BigInteger serviceFee = params.get(SERVICE_FEE).getAsBigInteger();
                String branchId = params.get(BRANCH_ID).getAsString();
                JsonObject branch = params.get(BRANCH).getAsJsonObject();

                try {
                    checkBalance(serviceFee);
                    updateBranchMeta(branch, branchId);
                    transferFee(serviceFee);
                    // Update the serviceFee state for this branchId
                    incrFeeState(branchId, serviceFee);
                    setSuccessTxReceipt(String.format(BRANCH_UPDATED, branchId));
                } catch (Exception e) {
                    setFalseTxReceipt(BRANCH_NOT_UPDATED);
                    setExceptionLog(e);
                    log.debug("Update Exception : {}", e.getMessage());
                }

            } catch (Exception e) {
                setFalseTxReceipt(INVALID_PARAMS);
                log.debug("{} : {}", INVALID_PARAMS, e.getMessage());
            }
        }

        private boolean updateBranchMeta(JsonObject updateBranch, String branchId) throws RuntimeException {
            // Check branchId Exist
            if (!isBranchExists(branchId)) {
                throw new RuntimeException(BRANCH_NOT_EXISTS);
            }

            if (!isBranchMetaExists(branchId)) {
                throw new RuntimeException(BRANCH_META_NOT_EXISTS);
            }

            if (!isBranchValidator()) {
                throw new RuntimeException(ISSUER_IS_NOT_BRANCH_VALIDATOR);
            }

            JsonObject originBranchMeta = getBranchMeta(branchId);
            originBranchMeta = metaMerge(originBranchMeta, updateBranch);
            // Update branch metadata
            saveBranchMeta(branchId, originBranchMeta);
            return true;
        }

        @InvokeTransaction
        public void deposit(JsonObject params) { // Deposit yeed to STEM
            // TODO check serviceFee governance
            try {
                String issuer = receipt.getIssuer();
                String from = params.has(FROM) ? params.get(FROM).getAsString() : issuer;
                BigInteger amount = params.get(AMOUNT).getAsBigInteger();

                try {
                    deposit(from, amount);
                    incrFeeState(receipt.getBranchId(), amount);
                    setSuccessTxReceipt(String.format(DEPOSIT_COMPLETED, from));
                } catch (Exception e) {
                    setExceptionLog(e);
                    log.debug("Deposit Exception : {}", e.getMessage());
                }

            } catch (Exception e) {
                setFalseTxReceipt(INVALID_PARAMS);
                log.debug("{} : {}", INVALID_PARAMS, e.getMessage());
            }
        }

        private void deposit(String from, BigInteger amount) throws RuntimeException {
            checkAmount(amount);

            boolean result = !from.equals(receipt.getIssuer())
                    ? callTransferFromChannel(createDepositParam(from, amount))
                    : callTransferChannel(createDepositParam(from, amount));
            if (!result) {
                throw new RuntimeException(String.format(DEPOSIT_FAILED, from));
            }
        }

        @InvokeTransaction
        public void withdraw(JsonObject params) { // Withdraw the serviceFee(yeed) for the branchId by validators
            // TODO check serviceFee governance
            try {
                BigInteger amount = params.get(AMOUNT).getAsBigInteger();
                String to = receipt.getIssuer();
                String branchId = receipt.getBranchId();

                try {
                    withdraw(to, amount);
                    decrFessState(branchId, amount);
                    setSuccessTxReceipt(WITHDRAW_COMPLETED);
                } catch (Exception e) {
                    setExceptionLog(e);
                    log.debug("Withdraw Exception : {}", e.getMessage());
                }

            } catch (Exception e) {
                setFalseTxReceipt(INVALID_PARAMS);
                log.debug("{} : {}", INVALID_PARAMS, e.getMessage());
            }
        }

        private void withdraw(String to, BigInteger amount) throws RuntimeException {
            checkAmount(amount);

            if (!isBranchValidator()) {
                throw new RuntimeException(ISSUER_IS_NOT_BRANCH_VALIDATOR);
            }

            if (!isWithdrawalAvailable(amount)) {
                throw new RuntimeException(INSUFFICIENT_FUNDS_OF_STEM);
            }

            if (!callTransferChannel(createWithdrawParam(to, amount))) {
                throw new RuntimeException(String.format(WITHDRAW_FAILED, to));
            }
        }

        @InvokeTransaction
        public void updateValidator(JsonObject params) { // Update validators that are branch metadata
            // TODO check serviceFee governance

            try {
                String branchId = params.get(BRANCH_ID).getAsString();
                Long blockHeight = params.get(BLOCK_HEIGHT).getAsLong();
                String proposer = params.get(PROPOSER).getAsString();
                String targetValidator = params.get(TARGET_VALIDATOR).getAsString();
                StemOperation operatingFlag = StemOperation.fromValue(params.get(OPERATING_FLAG).getAsString());
                JsonArray signed = params.get(SIGNED).getAsJsonArray();
                JsonArray validatorList = operatingFlag.equals(StemOperation.UPDATE_VALIDATOR_SET)
                        ? params.get(VALIDATORS).getAsJsonArray() : new JsonArray();

                try {
                    JsonArray validatorArray = getValidatorArray(branchId);
                    Set<String> validatorSet = getValidatorSet(validatorArray);
                    byte[] message = merge(branchId, blockHeight, proposer, targetValidator, operatingFlag);

                    checkVoteCount(validatorSet, signed, message);

                    switch (operatingFlag) {
                        case ADD_VALIDATOR:
                            // Verify that targetValidator already exists
                            if (validatorSet.contains(targetValidator)) {
                                throw new RuntimeException(TARGET_VALIDATOR_ALREADY_EXISTS);
                            }
                            // Add targetValidator to the list of validator set
                            validatorArray.add(targetValidator);

                            saveValidators(branchId, createValidatorsJsonObj(validatorArray));
                            setSuccessTxReceipt(VALIDATOR_ADDED);
                            break;
                        case REMOVE_VALIDATOR:
                            // Verify that targetValidator already exists
                            if (!validatorSet.contains(targetValidator)) {
                                throw new RuntimeException(TARGET_VALIDATOR_NOT_EXISTS);
                            }
                            // Remove targetValidator from the list of validator set
                            validatorSet.remove(targetValidator);
                            validatorArray = JsonUtil.convertCollectionToJsonArray(validatorSet);

                            saveValidators(branchId, createValidatorsJsonObj(validatorArray));
                            setSuccessTxReceipt(VALIDATOR_REMOVED);
                            break;
                        case REPLACE_VALIDATOR:
                            // Verify proposer exists and targetValidator not exists
                            if (!validatorSet.contains(proposer) || validatorSet.contains(targetValidator)) {
                                throw new RuntimeException(PROPOSER_OR_NEW_VALIDATOR_NOT_EXISTS);
                            }

                            validatorSet.remove(proposer);
                            validatorSet.add(targetValidator);
                            validatorArray = JsonUtil.convertCollectionToJsonArray(validatorSet);

                            saveValidators(branchId, createValidatorsJsonObj(validatorArray));
                            setSuccessTxReceipt(String.format(VALIDATOR_REPLACED, proposer, targetValidator));
                            break;
                        case UPDATE_VALIDATOR_SET:
                            // Update entire validator set
                            JsonObject newValidatorSet = createValidatorsJsonObj(validatorList);

                            // TargetValidator is a hash of all validators by sha3omit12
                            byte[] validatorsByteArray = newValidatorSet.toString().getBytes(StandardCharsets.UTF_8);
                            byte[] validatorsSha3 = HashUtil.sha3omit12(validatorsByteArray);
                            String calculateTargetValidator = HexUtil.toHexString(validatorsSha3);

                            // Verify validator list
                            if (!targetValidator.equals(calculateTargetValidator)) {
                                throw new RuntimeException(INVALID_TARGET_VALIDATOR_SET);
                            }

                            saveValidators(branchId, newValidatorSet);
                            setSuccessTxReceipt(VALIDATOR_SET_ALL_CHANGED);
                            break;
                    }
                    // TODO transfer serviceFee
                    // TODO save feeState
                } catch (Exception e) {
                    setFalseTxReceipt(VALIDATORS_NOT_UPDATED);
                    setExceptionLog(e);
                    log.debug("UpdateValidator Exception : {}", e.getMessage());
                }

            } catch (Exception e) {
                setFalseTxReceipt(INVALID_PARAMS);
                log.debug("{} : {}", INVALID_PARAMS, e.getMessage());
            }
        }

        private byte[] merge(String bid, Long blockHeight, String proposer, String tValidator, StemOperation opFlag) {
            return ByteUtil.merge(
                    HexUtil.hexStringToBytes(bid),
                    ByteUtil.longToBytes(blockHeight),
                    HexUtil.hexStringToBytes(proposer),
                    HexUtil.hexStringToBytes(tValidator),
                    opFlag.toValue().getBytes(StandardCharsets.UTF_8)
            );
        }

        JsonObject getValidators(String branchId) { // Get Validator Set from state
            return this.state.get(branchValidatorKey(branchId));
        }

        private JsonArray getValidatorArray(String branchId) throws RuntimeException {
            // Get Validator Set from state
            JsonObject validators = getValidators(branchId);
            if (validators == null) {
                throw new RuntimeException(VALIDATORS_NOT_EXISTS);
            }

            return validators.get(VALIDATORS).getAsJsonArray();
        }

        private Set<String> getValidatorSet(JsonArray validatorArray) throws RuntimeException {
            // Verify that issuer is a branch validator
            Set<String> validatorSet = JsonUtil.convertJsonArrayToSet(validatorArray);
            if (!validatorSet.contains(receipt.getIssuer())) {
                throw new RuntimeException(ISSUER_IS_NOT_VALIDATOR);
            }

            return validatorSet;
        }

        private void checkVoteCount(Set<String> validatorSet, JsonArray signed, byte[] msg) throws RuntimeException {
            msg = HashUtil.sha3(msg); // All message is sha3hashed

            int voteCount = (int) Math.ceil(1.0 * validatorSet.size() * 2 / 3);
            int vote = 0;
            log.debug("Vote count {}", voteCount);

            // Check the validator set that issuer wants to update
            Set<String> checkValidator = new HashSet<>();
            checkValidator.addAll(validatorSet);

            List<String> signedList = JsonUtil.convertJsonArrayToStringList(signed);
            // Verify signatures of validators
            for (String sign : signedList) {
                // TODO move signature to contract core
                byte[] signatureArray = HexUtil.hexStringToBytes(sign);
                ECKey.ECDSASignature signature = new ECKey.ECDSASignature(signatureArray);
                int realV = signatureArray[0] - 27;
                byte[] address = ECKey.recoverAddressFromSignature(realV, signature, msg);
                String addressHexString = HexUtil.toHexString(address);
                // Count the votes by verifying that the validator that signed the message exists
                if (checkValidator.contains(addressHexString)) {
                    vote++;
                    checkValidator.remove(addressHexString);
                }
            }
            checkValidator.clear();

            if (vote < voteCount) {
                throw new RuntimeException(LACK_OF_QUORUM);
            }
        }

        @ContractEndBlock
        public Receipt endBlock() {
            //TODO endBlock policy
            return receipt;
        }

        /**
         * @param params branch id
         *
         * @return branch json object
         */
        @ContractQuery
        public JsonObject getBranch(JsonObject params) {
            try {
                String branchId = params.get(BRANCH_ID).getAsString();
                // TODO ServiceFee not enough message
                return getBranch(branchId);
            } catch (Exception e) {
                return new JsonObject();
            }
        }

        private JsonObject getBranch(String branchId) {
            return isBranchExists(branchId)? this.state.get(branchIdKey(branchId)) : new JsonObject();
        }

        @ContractQuery
        public JsonObject getBranchMeta(JsonObject param) {
            try {
                String branchId = param.get(BRANCH_ID).getAsString();
                return getBranchMeta(branchId);
            } catch (Exception e) {
                return new JsonObject();
            }
        }

        private JsonObject getBranchMeta(String branchId) {
            return isBranchMetaExists(branchId) ? this.state.get(branchMetaKey(branchId)) : new JsonObject();
        }

        JsonObject metaMerge(JsonObject branchMeta, JsonObject branchMetaUpdate) {
            List<String> keyList = Arrays.asList(new String[] {DESCRIPTION}); // Updatable properties
            keyList.stream().forEach(key -> {
                if (branchMeta.has(key) && branchMetaUpdate.has(key)) {
                    branchMeta.addProperty(key, branchMetaUpdate.get(key).getAsString());
                }
            });
            return branchMeta;
        }

        /**
         * @param params branch id
         *
         * @return contract json object
         */
        @ContractQuery
        public Set<JsonObject> getContract(JsonObject params) {
            try {
                String branchId = params.get(BRANCH_ID).getAsString();
                return getContract(branchId);
            } catch (Exception e) {
                return new HashSet<>();
            }
        }

        public Set<JsonObject> getContract(String branchId) {
            Set<JsonObject> contractSet = new HashSet<>();
            JsonObject branch = getBranch(branchId);
            if (branch.size() > 0) {
                JsonArray contracts = branch.get(CONTRACTS).getAsJsonArray();
                for (JsonElement c : contracts) {
                    contractSet.add(c.getAsJsonObject());
                }
            }
            return contractSet;
        }

        private String getContractVersion(String branchId, String contractName) {
            return getContract(branchId).stream()
                    .filter(cObj -> cObj.get(NAME).getAsString().equals(contractName))
                    .findFirst()
                    .map(cObj -> cObj.get(CONTRACT_VERSION).getAsString())
                    .orElse("");
        }

        /**
         * @param params branch id
         *
         * @return serviceFee state for branch
         */
        @ContractQuery
        public BigInteger feeState(JsonObject params) {
            try {
                String branchId = params.get(BRANCH_ID).getAsString();
                return getFeeState(branchId);
            } catch (Exception e) {
                return BigInteger.ZERO;
            }
        }

        private void incrFeeState(String branchId, BigInteger amount) {
            JsonObject feeObj = new JsonObject();
            feeObj.addProperty(SERVICE_FEE, amount.add(getFeeState(branchId)));
            this.state.put(branchId, feeObj);
        }

        private void decrFessState(String branchId, BigInteger amount) {
            JsonObject feeObj = new JsonObject();
            feeObj.addProperty(SERVICE_FEE, getFeeState(branchId).subtract(amount));
            this.state.put(branchId, feeObj);
        }

        private BigInteger getFeeState(String branchId) {
            return this.state.contains(branchId)
                    ? this.state.get(branchId).get(SERVICE_FEE).getAsBigInteger() : BigInteger.ZERO;
        }

        private void transferFee(BigInteger serviceFee) throws Exception {
            if (!callTransferChannel(createDepositParam(this.receipt.getIssuer(), serviceFee))) {
                throw new Exception(SERVICE_FEE_TRANSFER_FAILED);

            }
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

        public boolean callTransferFromChannel(JsonObject param) {
            // Get Contract Version in branch
            String yeedContractVersion = this.branchStateStore.getContractVersion(YEED);
            log.debug("YEED Contract {}", yeedContractVersion);
            JsonObject result = this.channel.call(
                    yeedContractVersion, ContractMethodType.CHANNEL_METHOD, TRANSFER_FROM_CHANNEL, param);

            return result.get(RESULT).getAsBoolean();
        }

        private boolean callTransferChannel(JsonObject param) {
            // Get Contract Version in branch
            String yeedContractVersion = this.branchStateStore.getContractVersion(YEED);
            log.debug("YEED Contract {}", yeedContractVersion);
            JsonObject result = this.channel.call(yeedContractVersion,
                    ContractMethodType.CHANNEL_METHOD, TRANSFER_CHANNEL, param);

            return result.get(RESULT).getAsBoolean();
        }

        private BigInteger getContractBalance(String contractName) {
            JsonObject param = new JsonObject();
            param.addProperty(CONTRACT_NAME, contractName);

            // Get Contract Version in branch
            String yeedContractVersion = this.branchStateStore.getContractVersion(YEED);
            JsonObject result = this.channel.call(
                    yeedContractVersion, ContractMethodType.CHANNEL_METHOD, GET_CONTRACT_BALANCE_OF, param);

            return result.get(RESULT).getAsBigInteger();
        }

        private boolean isWithdrawalAvailable(BigInteger amount) {
            BigInteger stemBalance = getContractBalance(STEM);
            return stemBalance.subtract(amount).compareTo(BigInteger.ZERO) >= 0;
        }

        private boolean isPositive(BigInteger amount) {
            return amount.compareTo(BigInteger.ZERO) > 0; // amount > 0
        }

        private boolean isBranchExists(String branchId) {
            return this.state.contains(branchIdKey(branchId));
        }

        private boolean isBranchMetaExists(String branchId) {
            return this.state.contains(branchMetaKey(branchId));
        }

        private boolean isBranchValidator() {
            // Check branch validator
            JsonObject validators = getValidators(receipt.getBranchId());
            return validators != null && validators.toString().contains(receipt.getIssuer());
        }

        private boolean isBalanceEnough(String address, BigInteger serviceFee) {
            JsonObject param = new JsonObject();
            param.addProperty(ADDRESS, address);
            param.addProperty(AMOUNT, serviceFee);

            // Get Contract Version in branch
            String yeedContractVersion = this.branchStateStore.getContractVersion(YEED);
            JsonObject result = this.channel.call(
                    yeedContractVersion, ContractMethodType.CHANNEL_METHOD, IS_TRANSFERABLE, param);

            return result.get(RESULT).getAsBoolean();
        }

        private void checkAmount(BigInteger amount) throws RuntimeException {
            // Amount must be greater than zero
            if (!isPositive(amount)) {
                throw new RuntimeException(INVALID_AMOUNT);
            }
        }

        private void checkBalance(BigInteger amount) throws RuntimeException {
            checkAmount(amount);

            // Verify if the issuer can pay the serviceFee
            if (!isBalanceEnough(receipt.getIssuer(), amount)) {
                throw new RuntimeException(INSUFFICIENT_FUNDS);
            }
        }

        private void checkBranch(JsonObject branch) throws RuntimeException {
            // Check property
            boolean verify = true;
            List<String> existProperty = Arrays.asList(
                    new String[]{NAME, SYMBOL, PROPERTY, CONTRACTS, GOVERNANCE_CONTRACT});

            for (String propertyKey : existProperty) {
                verify &= branch.get(propertyKey).isJsonNull() != true;
            }

            if (!verify) {
                throw new RuntimeException(INVALID_BRANCH);
            }
        }

        private JsonObject createValidatorsJsonObj(JsonArray validatorArray) {
            JsonObject validatorList = new JsonObject();
            validatorList.add(VALIDATORS, validatorArray);
            return validatorList;
        }

        private JsonObject createWithdrawParam(String to, BigInteger amount) {
            JsonObject param = new JsonObject();
            param.addProperty(FROM, STEM);
            param.addProperty(TO, to);
            param.addProperty(AMOUNT, amount);
            return param;
        }

        private JsonObject createDepositParam(String from, BigInteger amount) {
            JsonObject param = new JsonObject();
            param.addProperty(FROM, from);
            param.addProperty(TO, STEM);
            param.addProperty(AMOUNT, amount);
            return param;
        }

        private void saveBranch(String branchId, JsonObject branch) {
            this.state.put(branchIdKey(branchId), branch);
        }

        private void saveBranchMeta(String branchId, JsonObject branchMeta) {
            this.state.put(branchMetaKey(branchId), branchMeta);
        }

        private void saveValidators(String branchId, JsonObject validators) {
            this.state.put(branchValidatorKey(branchId), validators);
        }

        private String branchIdKey(String branchId) {
            return String.format("%s%s", PrefixKeyEnum.STEM_BRANCH, branchId);
        }

        private String branchMetaKey(String branchId) {
            return String.format("%s%s", PrefixKeyEnum.STEM_META, branchId);
        }

        private String branchValidatorKey(String branchId) {
            return String.format("%s%s", PrefixKeyEnum.STEM_BRANCH_VALIDATOR, branchId);
        }

        private void setExceptionLog(Exception e) {
            if (e instanceof  RuntimeException) {
                setFalseTxReceipt(e.getMessage());
            } else {
                setErrorTxReceipt(e.getMessage());
            }
        }

        private void setErrorTxReceipt(String msg) {
            this.receipt.setStatus(ExecuteStatus.ERROR);
            this.receipt.addLog(msg);
        }

        private void setFalseTxReceipt(String msg) {
            this.receipt.setStatus(ExecuteStatus.FALSE);
            this.receipt.addLog(msg);
        }

        private void setSuccessTxReceipt(String msg) {
            this.receipt.setStatus(ExecuteStatus.SUCCESS);
            this.receipt.addLog(msg);
        }

    }
}