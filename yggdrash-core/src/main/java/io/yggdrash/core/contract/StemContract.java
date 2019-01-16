package io.yggdrash.core.contract;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import static io.yggdrash.common.config.Constants.BRANCH_ID;
import io.yggdrash.core.blockchain.Branch;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.runtime.annotation.ContractQuery;
import io.yggdrash.core.runtime.annotation.ContractStateStore;
import io.yggdrash.core.runtime.annotation.ContractTransactionReceipt;
import io.yggdrash.core.runtime.annotation.Genesis;
import io.yggdrash.core.runtime.annotation.InvokeTransction;
import io.yggdrash.core.store.Store;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StemContract implements Contract<JsonObject> {

    private static final Logger log = LoggerFactory.getLogger(StemContract.class);

    private final String branchIdListKey = "BRANCH_ID_LIST";

    @ContractStateStore
    Store<String, JsonObject> state;


    @ContractTransactionReceipt
    TransactionReceipt txReceipt;

    @Genesis
    @InvokeTransction // TODO remove InvokeTransaction
    public TransactionReceipt genesis(JsonObject param) {
        txReceipt = create(param);
        log.info("[StemContract | genesis] SUCCESS! param => " + param);

        return txReceipt;
    }

    /**
     * Returns the id of a registered branch
     *
     * @param params branch   : The branch.json to register on the stem
     */
    @InvokeTransction
    public TransactionReceipt create(JsonObject params) {

        for (Map.Entry<String, JsonElement> entry : params.entrySet()) {
            BranchId branchId = BranchId.of(entry.getKey());
            JsonObject json = entry.getValue().getAsJsonObject();
            txReceipt.putLog(branchId.toString(), json.toString());
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
    @InvokeTransction
    public TransactionReceipt update(JsonObject params) {

        for (Map.Entry<String, JsonElement> entry : params.entrySet()) {
            BranchId branchId = BranchId.of(entry.getKey());
            JsonObject json = entry.getValue().getAsJsonObject();
            txReceipt.putLog(branchId.toString(), json.toString());
            StemContractStateValue stateValue = getStateValue(branchId.toString());
            if (stateValue != null && isOwnerValid(json.get("owner").getAsString())) {
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
        stateValue.updateContract(json.get("contractId").getAsString());
    }

    /**
     * Returns branch.json as JsonString (query)
     *
     * @param params   branchId
     */
    @ContractQuery
    public JsonObject view(JsonObject params) {
        String branchId = params.get(BRANCH_ID).getAsString().toLowerCase();
        if (isBranchExist(branchId)) {
            return getStateValue(branchId).getJson();
        }
        return new JsonObject();
    }

    /**
     * Returns current contract of branch
     *
     * @param params   branchId
     */
    @ContractQuery
    public ContractId getcurrentcontract(JsonObject params) {
        String branchId = params.get(BRANCH_ID)
                .getAsString().toLowerCase();
        if (isBranchExist(branchId)) {
            return getStateValue(branchId).getContractId();
        }
        return null;
    }

    /**
     * Returns version history of branch
     *
     * @param params   branchId
     */
    @ContractQuery
    public List<ContractId> getcontracthistory(JsonObject params) {
        String branchId = params.get(BRANCH_ID)
                .getAsString().toLowerCase();
        if (isBranchExist(branchId)) {
            return getStateValue(branchId).getContractHistory();
        }
        return Collections.emptyList();
    }

    /**
     * Returns a list contains all branch id
     *
     * @return list of all branch id
     * // TODO REMOVE getAllBranchId
     */
    @ContractQuery
    public Set<String> getallbranchid() {
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

    private boolean isBranchExist(String branchId) {
        return state.get(branchId) != null;
    }

    // new branchId
    private void addBranchId(BranchId newBranchId) {
        // check branch exist
        if (!isBranchExist(newBranchId.toString())) {
            JsonArray branchIds = new JsonArray();
            for (String branchId : getallbranchid()) {
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
        String branchId = param.get("branchId").getAsString().toLowerCase();
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