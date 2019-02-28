package io.yggdrash.core.contract;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.yggdrash.common.contract.Contract;
import io.yggdrash.contract.core.ExecuteStatus;
import io.yggdrash.contract.core.TransactionReceipt;
import io.yggdrash.contract.core.store.ReadWriterStore;
import io.yggdrash.core.blockchain.Branch;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.contract.core.annotation.ContractQuery;
import io.yggdrash.contract.core.annotation.ContractStateStore;
import io.yggdrash.contract.core.annotation.ContractTransactionReceipt;
import io.yggdrash.contract.core.annotation.Genesis;
import io.yggdrash.contract.core.annotation.InvokeTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.yggdrash.common.config.Constants.BRANCH_ID;
import static io.yggdrash.common.config.Constants.VALIDATOR;

public class StemContract implements Contract<JsonObject> {

    private static final Logger log = LoggerFactory.getLogger(StemContract.class);

    private final String branchIdListKey = "BRANCH_ID_LIST";

    @ContractStateStore
    ReadWriterStore<String, JsonObject> state;


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
        // TODO Change StemContract Spec
        for (Map.Entry<String, JsonElement> entry : params.entrySet()) {
            BranchId branchId = BranchId.of(entry.getKey());
            JsonObject json = entry.getValue().getAsJsonObject();

            StemContractStateValue stateValue;
            try {
                stateValue = StemContractStateValue.of(json);
            } catch (Exception e) {
                log.warn("Failed to convert Branch = {}", json);
                continue;
            }
            if (!isBranchExist(branchId.toString()) && isBranchIdValid(branchId, stateValue)) {
                try {
                    stateValue.init();
                    // Branch ID 추가부터
                    addBranchId(branchId);
                    state.put(branchId.toString(), stateValue.getJson());

                    txReceipt.setStatus(ExecuteStatus.SUCCESS);
                } catch (Exception e) {
                    e.printStackTrace();
                    txReceipt.setStatus(ExecuteStatus.FALSE);
                }

                log.info("[StemContract | create] branchId => " + branchId);
                log.info("[StemContract | create] branch => " + json);
            }
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
        for (Map.Entry<String, JsonElement> entry : params.entrySet()) {
            BranchId branchId = BranchId.of(entry.getKey());
            JsonObject json = entry.getValue().getAsJsonObject();

            StemContractStateValue stateValue = getStateValue(branchId.toString());
            if (stateValue != null && isOwnerValid(json.get("validator").getAsString())) {
                updateBranch(stateValue, json);
                state.put(branchId.toString(), stateValue.getJson());
                txReceipt.setStatus(ExecuteStatus.SUCCESS);
                log.info("[StemContract | update] branchId => " + branchId);
                log.info("[StemContract | update] branch => " + stateValue.getJson());
            }
        }
        return txReceipt;
    }

    private void updateBranch(StemContractStateValue stateValue, JsonObject json) {
        if (json.has("tag")) {
            stateValue.setTag(json.get("tag").getAsString());
        }
        if (json.has("description")) {
            stateValue.setDescription(json.get("description").getAsString());
        }
        if (json.has("type")) {
            stateValue.setType(json.get("type").getAsString());
        }
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
            return getStateValue(branchId).getContractHistory();
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
            return getStateValue(branchId).getJson();
        }
        return new JsonObject();
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
            JsonArray contracts = getStateValue(branchId).getJson()
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
     * @return contract json object
     */
    @ContractQuery
    public Set<String> getValidator(JsonObject params) {
        String branchId = params.get(BRANCH_ID).getAsString();
        Set<String> validatorSet = new HashSet<>();
        if (isBranchExist(branchId)) {
            JsonArray contracts = getStateValue(branchId).getJson()
                    .getAsJsonArray("validator");
            for (JsonElement c : contracts) {
                validatorSet.add(c.getAsString());
            }
        }
        return validatorSet;
    }


    /**
     * @param params branch id
     *
     * @return contract json object
     */
    @ContractQuery
    public Set<String> getBranchIdByValidator(JsonObject params) {
        String validator = params.get(VALIDATOR).getAsString();
        Set<String> branchIdSet = new HashSet<>();

        getBranchIdList().stream().forEach(id -> {
            getStateValue(id).getValidators().stream().forEach(v -> {
                if (validator.equals(v)) {
                    branchIdSet.add(id);
                }
            });
        });
        return branchIdSet;
    }
    
    private boolean isBranchExist(String branchId) {
        return state.get(branchId) != null;
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

    private boolean isOwnerValid(String owner) {
        String sender = this.txReceipt.getIssuer();
        return sender != null && sender.equals(owner);
    }

    private boolean isBranchIdValid(BranchId branchId, Branch branch) {
        return branchId.equals(branch.getBranchId());
    }

    private StemContractStateValue getStateValue(JsonObject param) {
        String branchId = param.get("branchId").getAsString();
        return getStateValue(branchId);
    }

    private StemContractStateValue getStateValue(String branchId) {
        JsonObject json = state.get(branchId);
        if (json == null) {
            return null;
        } else {
            return new StemContractStateValue(json);
        }
    }


}