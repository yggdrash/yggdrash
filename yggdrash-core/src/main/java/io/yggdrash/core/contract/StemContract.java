package io.yggdrash.core.contract;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.genesis.BranchJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StemContract extends BaseContract<JsonObject> {

    private static final Logger log = LoggerFactory.getLogger(StemContract.class);
    private final ArrayList<String> types;

    public StemContract() {
        types = new ArrayList<>();
        types.add("immunity");
        types.add("mutable");
        types.add("instant");
        types.add("private");
        types.add("test");
    }

    public TransactionReceipt genesis(JsonArray params) {
        if (state.getState().size() == 0) {
            TransactionReceipt receipt = create(params);
            log.info("[StemContract | genesis] SUCCESS! params => " + params);
            return receipt;
        }
        return new TransactionReceipt();
    }

    /**
     * Returns the id of a registered branch
     *
     * @param params branch   : The branch.json to register on the stem
     */
    public TransactionReceipt create(JsonArray params) {

        TransactionReceipt txReceipt = new TransactionReceipt();
        JsonObject branchObject = params.get(0).getAsJsonObject();

        for (Map.Entry<String, JsonElement> entry : branchObject.entrySet()) {
            BranchId branchId = BranchId.of(entry.getKey());
            JsonObject jsonObjectBranch = entry.getValue().getAsJsonObject();
            BranchJson branchJson;
            try {
                branchJson = BranchJson.toBranchJson(jsonObjectBranch);
            } catch (IOException e) {
                log.warn("Failed to convert BranchJson = {}", jsonObjectBranch);
                continue;
            }
            txReceipt.putLog(branchId.toString(), branchJson);
            state.put(branchId.toString(), jsonObjectBranch);
            if (isBranchIdValid(branchId, jsonObjectBranch)) {
                addAdditionalProperty(jsonObjectBranch);
                state.put(branchId.toString(), jsonObjectBranch);
                setSubState(branchId.toString(), jsonObjectBranch);
                log.info("\nBranch id : " + branchId.toString()
                        + "\nBranch info : " + jsonObjectBranch);
                //if (this.sender != null && isOwnerValid(owner)) {
                txReceipt.setStatus(TransactionReceipt.SUCCESS);
            }
        }
        return txReceipt;
    }

    private void addAdditionalProperty(JsonObject branch) {
        if (!branch.has("type")) {
            branch.addProperty("type", "immunity");
        }
        if (!branch.has("tag")) {
            branch.addProperty("tag", "0.1");
        }
        if (!branch.has("contractHistory")) {
            JsonArray contractHistory = new JsonArray();
            contractHistory.add(branch.get("contractId").getAsString());
            branch.add("contractHistory", contractHistory);
        }
    }

    /**
     * Returns the id of a updated branch
     *
     * @param params branchId The Id of the branch to update
     *               branch   The branch.json to update on the stem
     */
    public TransactionReceipt update(JsonArray params) {
        JsonObject branchObject = params.get(0).getAsJsonObject();
        TransactionReceipt txReceipt = new TransactionReceipt();
        for (Map.Entry<String, JsonElement> entry : branchObject.entrySet()) {
            BranchId branchId = BranchId.of(entry.getKey());
            JsonObject jsonObjectBranch = entry.getValue().getAsJsonObject();
            BranchJson branchJson;
            try {
                branchJson = BranchJson.toBranchJson(jsonObjectBranch);
                txReceipt.putLog(branchId.toString(), branchJson);
            } catch (IOException e) {
                log.warn("Failed to convert BranchJson = {}", jsonObjectBranch);
                continue;
            }

            if (this.sender != null && isOwnerValid(branchJson.owner)) {
                addAdditionalProperty(jsonObjectBranch);
                state.replace(branchId.toString(), jsonObjectBranch);
                txReceipt.setStatus(TransactionReceipt.SUCCESS);
                log.info("[StemContract | update] branchId => " + branchId);
                log.info("[StemContract | update] branch => " + jsonObjectBranch);
            }
        }
        return txReceipt;
    }

    /**
     * Returns a list of branch.json (query)
     *
     * param key       type, name, property, owner, tag or symbol
     * param element   content of the key
     */
    public Set<Object> search(JsonArray params) {
        String subStateKey = params.get(0).getAsJsonObject().get("key").getAsString();
        String key = params.get(0).getAsJsonObject().get("value").getAsString();

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
    public String view(JsonArray params) {
        String branchId = params.get(0).getAsJsonObject().get("branchId")
                .getAsString().toLowerCase();
        if (isBranchExist(branchId)) {
            return getBranch(BranchId.of(branchId)).toString();
        }
        return "";
    }

    /**
     * Returns current version of branch
     *
     * @param params   branchId
     */
    public String getcurrentversion(JsonArray params) {
        String branchId = params.get(0).getAsJsonObject().get("branchId")
                .getAsString().toLowerCase();
        if (isBranchExist(branchId)) {
            JsonArray contractHistory = getBranch(BranchId.of(branchId))
                    .get("contractHistory").getAsJsonArray();
            int index = contractHistory.size() - 1;

            return contractHistory.get(index).getAsString();
        }
        return "";
    }

    /**
     * Returns version history of branch
     *
     * @param params   branchId
     */
    public JsonArray getversionhistory(JsonArray params) {
        String branchId = params.get(0).getAsJsonObject().get("branchId")
                .getAsString().toLowerCase();
        if (isBranchExist(branchId)) {
            return getBranch(BranchId.of(branchId)).get("contractHistory").getAsJsonArray();
        }
        return new JsonArray();
    }

    /**
     * Returns a list contains all branch id
     *
     * @param params none
     * @return list of all branch id
     */
    public List<String> getallbranchid(JsonArray params) {
        return state.getAllKey();
    }

    private boolean isBranchExist(String branchId) {
        return state.get(branchId) != null;
    }

    private boolean isOwnerValid(String owner) {
        return this.sender.equals(owner);
    }

    private boolean isBranchIdValid(BranchId branchId, JsonObject branch) {
        return BranchId.of(branch).equals(branchId);
    }

    private JsonObject getBranch(BranchId branchId) {
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