package io.yggdrash.contract;

import com.google.gson.JsonObject;

import java.util.List;

public class StemContract extends BaseContract<JsonObject> {

    /**
     * Returns the id of a registered branch
     * @param branch   The branch.json to register on the stem
     */
    public String create(JsonObject branch) {
        return null;
    }

    /**
     * Returns the id of a updated branch
     *
     * @param branch   The branch.json to update on the stem
     */
    public String update(JsonObject branch) {
        return null;
    }

    /**
     * Returns a list of branch.json (query)
     *
     * @param element   type, name, property, owner, tag or symbol
     */
    public List<JsonObject> search(String element) {
        return null;
    }

    /**
     * Returns branch.json as JsonString (query)
     *
     * @param branchId   branchId
     */
    public String view(String branchId) {
        return null;
    }

}