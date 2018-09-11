package io.yggdrash.contract;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.yggdrash.core.TransactionReceipt;
import io.yggdrash.crypto.HashUtil;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StemContract extends BaseContract<JsonObject> {

    private static final Logger log = LoggerFactory.getLogger(StemContract.class);
    private ArrayList<String> types;

    public StemContract() {
        types = new ArrayList<>();
        types.add("immunity");
        types.add("mutable");
        types.add("instant");
        types.add("private");
        types.add("test");
    }

    public TransactionReceipt genesis(JsonArray params) {
        // TODO implemented
        log.info("[StemContract | genesis] SUCCESS! params => " + params);
        return new TransactionReceipt();
    }

    /**
     * Returns the id of a registered branch
     *
     * @param params branchId : The Id of the branch to create
     *               branch   : The branch.json to register on the stem
     */
    public TransactionReceipt create(JsonArray params) {
        String branchId = params.get(0).getAsJsonObject().get("branchId").getAsString();
        JsonObject branch = params.get(0).getAsJsonObject().get("branch").getAsJsonObject();

        TransactionReceipt txReceipt = new TransactionReceipt();
        txReceipt.put("branchId", branchId);
        txReceipt.put("branch", branch);
        txReceipt.setStatus(0);

        log.info("[StemContract | create] (param) branch => " + branch);
        // 1. The type of the branch must be one of types.
        // 2. The reference_address of the branch must be contained to branchStore.
        //    (In case of the branch has the reference_address)

        String refAddress = branch.get("reference_address").getAsString();
        String type = branch.get("type").getAsString();
        String owner = branch.get("owner").getAsString();
        if (this.sender != null && isOwnerValid(owner)) {
            if (verify(refAddress, type)) {
                if (isBranchIdValid(branchId, branch)) {
                    state.put(branchId, branch);
                    setSubState(branchId, branch);
                    log.info("[StemContract | create] SUCCESS! branchId => " + branchId);
                    txReceipt.setStatus(1);
                    //return branchId;
                }
            }
        }
        //return null;
        return txReceipt;
    }

    /**
     * Returns the id of a updated branch
     *
     * @param params branchId The Id of the branch to update
     *               branch   The branch.json to update on the stem
     */
    public TransactionReceipt update(JsonArray params) {
        String branchId = params.get(0).getAsJsonObject().get("branchId").getAsString();
        JsonObject branch = params.get(0).getAsJsonObject().get("branch").getAsJsonObject();

        TransactionReceipt txReceipt = new TransactionReceipt();
        txReceipt.put("branchId", branchId);
        txReceipt.put("branch", branch);
        txReceipt.setStatus(0);

        String owner = branch.get("owner").getAsString();
        if (this.sender != null && isOwnerValid(owner)) {
            if (isBranchIdValid(branchId, branch)) {
                if (isVersionHistoryUpdated(branchId, branch)) {
                    log.info("[StemContract | update] branchId => " + branchId);
                    log.info("[StemContract | update] branch => " + branch);
                    state.replace(branchId, branch);
                    txReceipt.setStatus(1);
                    //return branchId;
                }
                state.replace(branchId, branch);
            }
        }
        //return null;
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
            return getBranch(branchId).toString();
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
            JsonArray versionHistory = getBranch(branchId).get("versionHistory").getAsJsonArray();
            int index = versionHistory.size() - 1;

            return versionHistory.get(index).getAsString();
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
            return getBranch(branchId).get("versionHistory").getAsJsonArray();
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

    private boolean verify(String refAddress, String type) {
        return isRefAddressValid(refAddress) && isTypeValid(type);
    }

    private boolean isBranchExist(String branchId) {
        return state.get(branchId) != null;
    }

    private boolean isOwnerValid(String owner) {
        return this.sender.equals(owner);
    }

    private boolean isRefAddressValid(String key) {
        if (!key.isEmpty() && state.get(key) == null) {
            log.warn("[Validation] reference_address is not valid");
            return false;
        }
        return true;
    }

    private boolean isTypeValid(String key) {
        if (!types.contains(key)) {
            log.warn("[Validation] type is not valid");
            return false;
        }
        return true;
    }

    private boolean isBranchIdValid(String branchId, JsonObject branch) {
        if (branchId.equals(getBranchId(branch))) {
            log.info("[Validation] branchId is valid");
            return true;
        }
        log.warn("[Validation] branchId is not valid");
        return false;
    }

    private boolean isVersionHistoryUpdated(String branchId, JsonObject branch) {
        JsonElement updatedVersion = branch.get("version");
        JsonArray versionHistory = state.get(branchId).get("versionHistory").getAsJsonArray();
        if (!versionHistory.contains(updatedVersion)) {
            versionHistory.add(updatedVersion);
            return true;
        }
        return false;
    }

    private JsonObject getBranch(String branchId) {
        return state.get(branchId);
    }

    public String getBranchId(JsonObject branch) {
        return Hex.encodeHexString(getBranchHash(branch));
    }

    private byte[] getBranchHash(JsonObject branch) {
        return HashUtil.sha3(getRawBranch(branch));
    }

    private byte[] getRawBranch(JsonObject branch) {
        ByteArrayOutputStream branchStream = new ByteArrayOutputStream();
        try {
            branchStream.write(branch.get("name").getAsString().getBytes());
            branchStream.write(branch.get("property").getAsString().getBytes());
            branchStream.write(branch.get("type").getAsString().getBytes());
            branchStream.write(branch.get("timestamp").getAsString().getBytes());
            //branchStream.write(branch.get("version").getAsString().getBytes());
            branchStream.write(branch.get("versionHistory").getAsJsonArray().get(0)
                    .getAsString().getBytes());
            branchStream.write(branch.get("reference_address").getAsString().getBytes());
            branchStream.write(branch.get("reserve_address").getAsString().getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return branchStream.toByteArray();
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
        state.putSubState("tag",
                branch.get("tag").getAsString(), branchId);

        printSubState();
    }

    private void printSubState() {
        log.info("[StemContract | printSubState] typeState => "
                + state.getSubState("type").toString());
        log.info("[StemContract | printSubState] nameState => "
                + state.getSubState("name").toString());
        log.info("[StemContract | printSubState] propertyState => "
                + state.getSubState("property").toString());
        log.info("[StemContract | printSubState] ownerState => "
                + state.getSubState("owner").toString());
        log.info("[StemContract | printSubState] symbolState => "
                + state.getSubState("symbol").toString());
        log.info("[StemContract | printSubState] tagState => "
                + state.getSubState("tag").toString());
    }
}