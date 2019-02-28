package io.yggdrash.core.contract.osgi;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.yggdrash.core.blockchain.Branch;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.contract.Contract;
import io.yggdrash.core.contract.ContractVersion;
import io.yggdrash.core.contract.ExecuteStatus;
import io.yggdrash.core.contract.StemContractStateValue;
import io.yggdrash.core.contract.TransactionReceipt;
import io.yggdrash.core.runtime.annotation.ContractQuery;
import io.yggdrash.core.runtime.annotation.ContractStateStore;
import io.yggdrash.core.runtime.annotation.ContractTransactionReceipt;
import io.yggdrash.core.runtime.annotation.Genesis;
import io.yggdrash.core.runtime.annotation.InvokeTransaction;
import io.yggdrash.core.store.Store;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import static io.yggdrash.common.config.Constants.BRANCH_ID;
import static io.yggdrash.common.config.Constants.TX_ID;
import static io.yggdrash.common.config.Constants.VALIDATOR;

public class StemContract implements BundleActivator, ServiceListener {
    private static final Logger log = LoggerFactory.getLogger(io.yggdrash.core.contract.osgi.StemContract.class);

    @Override
    public void start(BundleContext context) throws Exception {
        log.info("⚪ Start stem contract");
        //Find for service in another bundle
        Hashtable<String, String> props = new Hashtable();
        props.put("YGGDRASH", "Stem");
        context.registerService(StemService.class.getName(), new StemService(), props);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        log.info("⚫ Stop stem contract");
    }

    @Override
    public void serviceChanged(ServiceEvent event) {

    }

    public static class StemService implements Contract<JsonObject> {
        private final String branchIdListKey = "BRANCH_ID_LIST";

        @ContractStateStore
        Store<String, JsonObject> state;


        @ContractTransactionReceipt
        TransactionReceipt txReceipt;

        @Genesis
        @InvokeTransaction // TODO remove InvokeTransaction
        public TransactionReceipt init(JsonObject param) {
            txReceipt = create(param);
            log.info("[StemContract | genesis] SUCCESS! param => " + param);
            return txReceipt;
        }

        /**
         * Returns the id of a registered branch
         *
         * @param params branch   : The branch.json to register on the stem
         */
        @InvokeTransaction
        public TransactionReceipt create(JsonObject params) {
            StemContractStateValue stateValue;
            try {
                stateValue = StemContractStateValue.of(params);
                BranchId branchId = stateValue.getBranchId();
                if (!isBranchExist(branchId.toString()) && isBranchIdValid(branchId, stateValue)) {
                    try {
                        addBranchId(branchId);
                        setStateValue(stateValue, params);
                        state.put(branchId.toString(), stateValue.getJson());
                        addTxId(branchId);

                        txReceipt.setStatus(ExecuteStatus.SUCCESS);
                    } catch (Exception e) {
                        e.printStackTrace();
                        txReceipt.setStatus(ExecuteStatus.FALSE);
                    }

                    log.info("[StemContract | create] branchId => " + branchId);
                    log.info("[StemContract | create] branch => " + params);
                }
            } catch (Exception e) {
                log.warn("Failed to convert Branch = {}", params);
            }
            return txReceipt;
        }

        /**
         * Returns the id of a updated branch
         *
         * @param params branchId The Id of the branch to update
         *               branch   The branch.json to update on the stem
         */
        @InvokeTransaction
        public TransactionReceipt update(JsonObject params) {
            StemContractStateValue stateValue;
            try {
                stateValue = StemContractStateValue.of(params);
                BranchId branchId = stateValue.getBranchId();
                if (isOwnerValid(params.get("validator").getAsString())
                        && stateValue != null && !isBranchExist(branchId.toString())
                        && isBranchIdValid(branchId, stateValue)) {
                    setStateValue(stateValue, params);

                    state.put(branchId.toString(), stateValue.getJson());
                    addTxId(branchId);
                    txReceipt.setStatus(ExecuteStatus.SUCCESS);
                    log.info("[StemContract | update] branchId => " + branchId);
                    log.info("[StemContract | update] branch => " + stateValue.getJson());
                }
            } catch (Exception e) {
                log.warn("Failed to convert Branch = {}", params);
            }

            return txReceipt;
        }

        /**
         * fee = fee - transaction size fee
         * tx size fee = txSize / 1mbyte
         * 1mbyte to 1yeed
         *
         * @param stateValue, json
         */
        private void setStateValue(StemContractStateValue stateValue, JsonObject json) {

            if (json.has("fee") && txReceipt.getTxSize() != null) {
                BigDecimal fee = json.get("fee").getAsBigDecimal();
                BigDecimal txSize = BigDecimal.valueOf(txReceipt.getTxSize());

                BigDecimal txFee = txSize.divide(BigDecimal.valueOf(1000000));
                BigDecimal resultFee = fee.subtract(txFee);

                stateValue.setFee(resultFee);
                stateValue.setBlockHeight(txReceipt.getBlockHeight());

                stateValue.setExtinguishBlockHeight();
            }

//        if (json.has("validator")) {
//            stateValue.set (json.get("validator").getAsJsonArray());
//        }
        }

        /**
         * Returns boolean
         *
         * @param params
         */
        @InvokeTransaction
        public Boolean extinguishBranch(JsonObject params) {
            // TODO branch extinguish
            return false;
        }

        /**
         * Returns boolean
         *
         * @param branchId
         */
        public void callYeed(BranchId branchId) {
            // TODO message call to yeed
        }

        /**
         * Returns current contract of branch
         *
         * @param params   branchId
         */
        @ContractQuery
        public ContractVersion getCurrentContract(JsonObject params) {
            String branchId = params.get(BRANCH_ID).getAsString();
            if (isBranchExist(branchId)) {
                //return getStateValue(branchId).getContractVersion();
            }
            return null;
        }

        /**
         * Returns version history of branch
         *
         * @param params   branchId
         */
        @ContractQuery
        public List<ContractVersion> getContractHistory(JsonObject params) {
            String branchId = params.get(BRANCH_ID).getAsString();
            if (isBranchExist(branchId)) {
                return getBranchStateValue(branchId).getContractHistory();
            }
            return Collections.emptyList();
        }

        /**
         * Returns a list contains all branch id
         *
         * @return list of all branch id
         */
        @ContractQuery
        public Set<String> getBranchIdList() {
            JsonObject branchList = state.get(branchIdListKey);
            if (branchList == null) {
                return Collections.emptySet();
            }
            JsonArray branchIds = branchList.getAsJsonArray("branchIds");
            Set<String> branchIdSet = new HashSet<>();
            for (JsonElement branchId : branchIds) {
                branchIdSet.add(branchId.getAsString());
            }
            return branchIdSet;
        }

        /**
         * @param params branch id
         *
         * @return branch json object
         */
        @ContractQuery
        public JsonObject getBranch(JsonObject params) {
            String branchId = params.get(BRANCH_ID).getAsString();
            if (isBranchExist(branchId)) {
                return getBranchStateValue(branchId).getJson();
            }
            return new JsonObject();
        }

        /**
         * @param params transaction id
         *
         * @return branch id
         */
        @ContractQuery
        public String getBranchIdByTxId(JsonObject params) {
            String txId = params.get(TX_ID).getAsString();
            JsonObject branchId = state.get(txId);
            return branchId == null ? new String()
                    : branchId.get("branchId").getAsString();
        }

        /**
         * @param params branch id
         *
         * @return contract json object
         */
        @ContractQuery
        public Set<JsonElement> getContractByBranch(JsonObject params) {
            String branchId = params.get(BRANCH_ID).getAsString();
            Set<JsonElement> contractSet = new HashSet<>();
            if (isBranchExist(branchId)) {
                JsonArray contracts = getBranchStateValue(branchId).getJson()
                        .getAsJsonArray("contracts");
                for (JsonElement c : contracts) {
                    contractSet.add(c);
                }
            }
            return contractSet;
        }

        /**
         * @param params branch id
         *
         * @return validator set
         */
        @ContractQuery
        public Set<String> getValidator(JsonObject params) {
            String branchId = params.get(BRANCH_ID).getAsString();
            Set<String> validatorSet = new HashSet<>();
            if (isBranchExist(branchId)) {
                JsonArray validators = getBranchStateValue(branchId).getJson()
                        .getAsJsonArray("validator");
                for (JsonElement v : validators) {
                    validatorSet.add(v.getAsString());
                }
            }
            return validatorSet;
        }


        /**
         * @param params branch id
         *
         * @return branch id set
         */
        @ContractQuery
        public Set<String> getBranchIdByValidator(JsonObject params) {
            String validator = params.get(VALIDATOR).getAsString();
            Set<String> branchIdSet = new HashSet<>();

            getBranchIdList().stream().forEach(id -> {
                getBranchStateValue(id).getValidators().stream().forEach(v -> {
                    if (validator.equals(v)) {
                        branchIdSet.add(id);
                    }
                });
            });
            return branchIdSet;
        }

        /**
         * @param params branch id
         *
         * @return fee state
         */
        public BigDecimal feeState(JsonObject params) {
            //TODO 현재 블록의 수수료 조회
            // 수수료 소진되는 로직

            String branchId = params.get(BRANCH_ID).getAsString();
            if (isBranchExist(branchId)) {
                return getBranchStateValue(branchId).getFee();
            }
            return BigDecimal.ZERO;
        }

        /**
         * @param params branch id
         *
         * @return block height state
         */
        public Long blockHeightState(JsonObject params) {
            return txReceipt.getBlockHeight();
        }


        private boolean isBranchExist(String branchId) {
            return state.contains(branchId);
        }

        // new branchId
        private void addBranchId(BranchId newBranchId) {
            // check branch exist
            if (!isBranchExist(newBranchId.toString())) {
                JsonArray branchIds = new JsonArray();
                for (String branchId : getBranchIdList()) {
                    branchIds.add(branchId);
                }
                JsonObject obj = new JsonObject();
                branchIds.add(newBranchId.toString());
                obj.add("branchIds", branchIds);
                state.put(branchIdListKey, obj);

            }
        }

        private void addTxId(BranchId branchId) {
            if (isBranchExist(branchId.toString())
                    && txReceipt.getTxId() != null) {
                JsonObject bid = new JsonObject();
                bid.addProperty("branchId", branchId.toString());
                state.put(txReceipt.getTxId(), bid);
            }
        }

        private boolean isOwnerValid(String owner) {
            String sender = this.txReceipt.getIssuer();
            return sender != null && sender.equals(owner);
        }

        private boolean isBranchIdValid(BranchId branchId, Branch branch) {
            return branchId.equals(branch.getBranchId());
        }

        private StemContractStateValue getStateValue(JsonObject param) {
            String branchId = param.get("branchId").getAsString();
            return getBranchStateValue(branchId);
        }

        private StemContractStateValue getBranchStateValue(String branchId) {
            JsonObject json = state.get(branchId);
            if (json == null) {
                return null;
            } else {
                return new StemContractStateValue(json);
            }
        }

        private Boolean validatorVerify(JsonObject params) {
            //TODO sign verify of validator
            txReceipt.getIssuer();
            params.getAsJsonArray("validator");

            return false;
        }
    }
}