package io.yggdrash.node.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.googlecode.jsonrpc4j.JsonRpcError;
import com.googlecode.jsonrpc4j.JsonRpcErrors;
import com.googlecode.jsonrpc4j.JsonRpcService;
import io.yggdrash.core.exception.NonExistObjectException;
import io.yggdrash.core.exception.WrongStructuredException;

import java.util.List;

@JsonRpcService("/api/branch")
public interface BranchApi {
    /**
     * Create a new branch
     * @param branch branch.json
     * @return branch id
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = WrongStructuredException.class,
                    code = WrongStructuredException.code)})
    String createBranch(JsonObject branch);

    /**
     * Update a branch
     * @param branchId branch id to update
     * @param branch branch.json
     * @return branch id
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = WrongStructuredException.class,
                    code = WrongStructuredException.code)})
    String updateBranch(String branchId, JsonObject branch);

    /**
     * Search for branches by key (attribute)
     * @param key   attribute
     * @param value value of attribute
     * @return list of branch
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = NonExistObjectException.class,
                    code = NonExistObjectException.code)})
    List<JsonObject> searchBranch(String key, String value);

    /**
     * View a branch in detail with branchId
     * @param branchId branch id
     * @return branch
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = NonExistObjectException.class,
                    code = NonExistObjectException.code)})
    String viewBranch(String branchId);

    /**
     * Get the current contract address of the branch by branchId
     * @param branchId branch id
     * @return current version
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = NonExistObjectException.class,
                    code = NonExistObjectException.code)})
    String getCurrentVersionOfBranch(String branchId);

    /**
     * Get the versionHistory of the branch by branchId
     * @param branchId branch id
     * @return versionHistory
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = NonExistObjectException.class,
                    code = NonExistObjectException.code)})
    JsonArray getVersionHistoryOfBranch(String branchId);
}
