package io.yggdrash.core.contract;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.yggdrash.core.blockchain.Branch;
import io.yggdrash.core.blockchain.BranchId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StemContract extends BaseContract<StemContractStateValue> {

    private static final Logger log = LoggerFactory.getLogger(StemContract.class);

    public TransactionReceipt genesis(JsonObject param) {
        if (state.getStateSize() == 0L) {
            TransactionReceipt receipt = create(param);
            log.info("[StemContract | genesis] SUCCESS! param => " + param);
            return receipt;
        }
        return new TransactionReceipt();
    }

    /**
     * Returns the id of a registered branch
     *
     * @param param branch   : The branch.json to register on the stem
     */
    public TransactionReceipt create(JsonObject param) {
        TransactionReceipt txReceipt = new TransactionReceipt();

        for (Map.Entry<String, JsonElement> entry : param.entrySet()) {
            BranchId branchId = BranchId.of(entry.getKey());
            JsonObject json = entry.getValue().getAsJsonObject();
            txReceipt.putLog(branchId.toString(), json);
            StemContractStateValue stateValue;
            try {
                stateValue = StemContractStateValue.of(json);
            } catch (Exception e) {
                log.warn("Failed to convert Branch = {}", json);
                continue;
            }
            if (getStateValue(branchId) == null && isBranchIdValid(branchId, stateValue)) {
                stateValue.init();
                state.put(branchId.toString(), stateValue);
                setSubState(branchId.toString(), json);

                txReceipt.setStatus(TransactionReceipt.SUCCESS);
                log.info("[StemContract | create] branchId => " + branchId);
                log.info("[StemContract | create] branch => " + json);
            }
        }
        return txReceipt;
    }

    /**
     * Returns the id of a updated branch
     *
     * @param param branchId The Id of the branch to update
     *               branch   The branch.json to update on the stem
     */
    public TransactionReceipt update(JsonObject param) {
        TransactionReceipt txReceipt = new TransactionReceipt();

        for (Map.Entry<String, JsonElement> entry : param.entrySet()) {
            BranchId branchId = BranchId.of(entry.getKey());
            JsonObject json = entry.getValue().getAsJsonObject();
            txReceipt.putLog(branchId.toString(), json);
            StemContractStateValue stateValue = getStateValue(branchId);
            if (stateValue != null && isOwnerValid(json.get("owner").getAsString())) {
                updateBranch(stateValue, json);
                txReceipt.setStatus(TransactionReceipt.SUCCESS);
                log.info("[StemContract | update] branchId => " + branchId);
                log.info("[StemContract | update] branch => " + json);
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
     * Returns a list of branch.json (query)
     *
     * param key       type, name, property, owner, tag or symbol
     * param element   content of the key
     */
    public Set<Object> search(JsonObject param) {
        String subStateKey = param.get("key").getAsString();
        String key = param.get("value").getAsString();

        if (state.getSubState(subStateKey) != null
                && state.getSubState(subStateKey).get(key) != null) {
            return state.getSubState(subStateKey).get(key);
        }
        return new HashSet<>();
    }

    /**
     * Returns branch.json as JsonString (query)
     *
     * @param param   branchId
     */
    public JsonObject view(JsonObject param) {
        String branchId = param.get("branchId")
                .getAsString().toLowerCase();
        if (isBranchExist(branchId)) {
            return getStateValue(BranchId.of(branchId)).getJson();
        }
        return new JsonObject();
    }

    /**
     * Returns current contract of branch
     *
     * @param param   branchId
     */
    public ContractId getcurrentcontract(JsonObject param) {
        String branchId = param.get("branchId")
                .getAsString().toLowerCase();
        if (isBranchExist(branchId)) {
            return getStateValue(BranchId.of(branchId)).getContractId();
        }
        return null;
    }

    /**
     * Returns version history of branch
     *
     * @param param   branchId
     */
    public List<ContractId> getcontracthistory(JsonObject param) {
        String branchId = param.get("branchId")
                .getAsString().toLowerCase();
        if (isBranchExist(branchId)) {
            return getStateValue(BranchId.of(branchId)).getContractHistory();
        }
        return Collections.emptyList();
    }

    /**
     * Returns a list contains all branch id
     *
     * @param param none
     * @return list of all branch id
     */
    public List<String> getallbranchid(JsonObject param) {
        return state.getAllKey();
    }

    private boolean isBranchExist(String branchId) {
        return state.get(branchId) != null;
    }

    private boolean isOwnerValid(String owner) {
        return sender != null && sender.equals(owner);
    }

    private boolean isBranchIdValid(BranchId branchId, Branch branch) {
        return branchId.equals(branch.getBranchId());
    }

    private StemContractStateValue getStateValue(BranchId branchId) {
        return state.get(branchId.toString());
    }

    private void setSubState(String branchId, JsonObject branch) {
        state.putSubState("type",
                branch.get("type").getAsString(), branchId);
        state.putSubState("name",
                branch.get("name").getAsString(), branchId);
        state.putSubState("property",
                branch.get("property").getAsString(), branchId);
        state.putSubState("owner",
                branch.get("owner").getAsString(), branchId);
        state.putSubState("symbol",
                branch.get("symbol").getAsString(), branchId);

        printSubState();
    }

    private void printSubState() {
        log.trace("[StemContract | printSubState] typeState => "
                + state.getSubState("type").toString());
        log.trace("[StemContract | printSubState] nameState => "
                + state.getSubState("name").toString());
        log.trace("[StemContract | printSubState] propertyState => "
                + state.getSubState("property").toString());
        log.trace("[StemContract | printSubState] ownerState => "
                + state.getSubState("owner").toString());
        log.trace("[StemContract | printSubState] symbolState => "
                + state.getSubState("symbol").toString());
    }
}