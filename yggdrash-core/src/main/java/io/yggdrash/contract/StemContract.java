package io.yggdrash.contract;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.yggdrash.crypto.HashUtil;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    /**
     * Returns the id of a registered branch
     *
     * @param branch   The branch.json to register on the stem
     */
    public String create(JsonObject branch) {
        log.info("[StemContract | create] branch => " + branch);
        // 1. The type of the branch must be one of types.
        // 2. The reference_address of the branch must be contained to branchStore.
        //    (In case of the branch has the reference_address)

        String refAddress = branch.get("reference_address").getAsString();
        String type = branch.get("type").getAsString();

        if (verify(refAddress, type)) {
            String branchId = Hex.encodeHexString(getBranchHash(branch));
            state.put(branchId, branch);
            log.info("[StemContract | create] branchId => " + branchId);
            return branchId;
        }
        return null;
    }

    /**
     * Returns the id of a updated branch
     *
     * @param branchId The Id of the branch to update.
     * @param branch   The branch.json to update on the stem
     */
    public String update(String branchId, JsonObject branch) {
        if (isBranchHonest(branchId, branch)) {
            if (isVersionHistoryUpdated(branchId, branch)) {
                log.info("[StemContract | update] branchId => "
                        + branchId + "\nbranch => " + branch);
                state.put(branchId, branch);
                return branchId;
            }
        }
        return null;
    }


    /*
    public List<JsonObject> search(String key, String element) {
        List<JsonObject> branchList = new ArrayList<>();
        for (JsonObject branch : state.getAll()) {
            if (element.equals(branch.get(key).getAsString())) {
                branchList.add(branch);
            }
        }
        log.info("[StemContract | search] branchList => " + branchList);
        return branchList;
    }
    */

    /**
     * Returns a list of branch.json (query)
     *
     * param key       type, name, property, owner, tag or symbol
     * param element   content of the key
     */
    public List<JsonObject> search(JsonArray params) {
        List<JsonObject> branchList = new ArrayList<>();
        JsonObject param = params.get(0).getAsJsonObject();
        log.info("[StemContract | search] param =>  : " + param);

        String key = "";
        String element = "";

        for (Map.Entry<String, JsonElement> entry : param.entrySet()) {
            key = entry.getKey();
            element = entry.getValue().getAsString();
        }

        for (JsonObject branch : state.getAll()) {
            if (!key.isEmpty() && element.equals(branch.get(key).getAsString())) {
                branchList.add(branch);
            }
        }
        return branchList;
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
            Integer index = versionHistory.size() - 1;

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

    private boolean verify(String refAddress, String type) {
        if (isRefAddressValid(refAddress) && isTypeValid(type)) {
            return true;
        }
        return false;
    }

    private boolean isBranchExist(String branchId) {
        if (state.get(branchId) != null) {
            return true;
        }
        return false;
    }

    private boolean isOwnerValid(String owner) {
        return false;
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

    private boolean isBranchHonest(String branchId, JsonObject branch) {
        if (branchId.equals(getBranchHashStr(getBranchHash(branch)))) {
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

    private String getBranchHashStr(byte[] rawBranchHash) {
        return Hex.encodeHexString(rawBranchHash);
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
}