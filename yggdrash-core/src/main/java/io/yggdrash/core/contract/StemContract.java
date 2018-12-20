package io.yggdrash.core.contract;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.yggdrash.core.blockchain.Branch;
import io.yggdrash.core.blockchain.BranchId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static io.yggdrash.common.config.Constants.BRANCH_ID;

public class StemContract extends BaseContract<JsonObject> {

    private static final Logger log = LoggerFactory.getLogger(StemContract.class);

    private final String branchIdListKey = "BRANCH_ID_LIST";

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
     * @param params branch   : The branch.json to register on the stem
     */
    public TransactionReceipt create(JsonObject params) {
        TransactionReceipt txReceipt = new TransactionReceipt();

        for (Map.Entry<String, JsonElement> entry : params.entrySet()) {
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
            if (!isBranchExist(branchId.toString()) && isBranchIdValid(branchId, stateValue)) {
                try {
                    stateValue.init();
                    // Branch ID 추가부터
                    addBranchId(branchId);
                    state.put(branchId.toString(), stateValue.getJson());
                    setSubState(branchId.toString(), stateValue.getJson());
                    txReceipt.setStatus(TransactionReceipt.SUCCESS);
                } catch (Exception e) {
                    e.printStackTrace();
                    txReceipt.setStatus(TransactionReceipt.FALSE);
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
    public TransactionReceipt update(JsonObject params) {
        TransactionReceipt txReceipt = new TransactionReceipt();

        for (Map.Entry<String, JsonElement> entry : params.entrySet()) {
            BranchId branchId = BranchId.of(entry.getKey());
            JsonObject json = entry.getValue().getAsJsonObject();
            txReceipt.putLog(branchId.toString(), json);
            StemContractStateValue stateValue = getStateValue(branchId.toString());
            if (stateValue != null && isOwnerValid(json.get("owner").getAsString())) {
                updateBranch(stateValue, json);
                state.put(branchId.toString(), stateValue.getJson());
                txReceipt.setStatus(TransactionReceipt.SUCCESS);
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
     * Returns a list of branch.json (query)
     *
     * params key       type, name, property, owner, tag or symbol
     * params value   content of the key
     */
    public Set<Object> search(JsonObject params) {
        String subStateKey = params.get("key").getAsString();
        String key = params.get("value").getAsString();

        if (state.getSubState(subStateKey) != null
                && state.getSubState(subStateKey).get(key) != null) {
            return state.getSubState(subStateKey).get(key);
        }
        return new HashSet<>();
    }

    /**
     * Returns branch.json as JsonString (query)
     *
     * @param params   branchId
     */
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
     */
    // TODO REMOVE getAllBranchID
    public List<String> getallbranchid() {
        JsonObject branchList = state.get(branchIdListKey);
        if (branchList == null) {
            return Collections.emptyList();
        }
        JsonArray branchIds = branchList.getAsJsonArray("branchIds");
        List<String> branchIdList = new ArrayList<>();
        for (JsonElement branchId : branchIds) {
            branchIdList.add(branchId.getAsString());
        }
        return branchIdList;
    }

    private boolean isBranchExist(String branchId) {
        return state.get(branchId) != null;
    }

    private void addBranchId(BranchId newBranchId) {
        if(!isBranchExist(newBranchId.toString())){
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