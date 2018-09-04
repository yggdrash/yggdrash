package io.yggdrash.node.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.googlecode.jsonrpc4j.JsonRpcError;
import com.googlecode.jsonrpc4j.JsonRpcErrors;
import com.googlecode.jsonrpc4j.JsonRpcService;
import io.yggdrash.core.exception.NonExistObjectException;
import io.yggdrash.core.exception.WrongStructuredException;
import io.yggdrash.node.controller.TransactionDto;

import java.util.List;

@JsonRpcService("/api/branch")
public interface BranchApi {
    /**
     * Create a new branch
     *
     * @param tx branch creation transaction
     * @return branch id
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = WrongStructuredException.class,
                    code = WrongStructuredException.code)})
    String createBranch(TransactionDto tx);

    /**
     * Update a branch
     *
     * @param tx branch update transaction
     * @return branch id
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = WrongStructuredException.class,
                    code = WrongStructuredException.code)})
    String updateBranch(TransactionDto tx);

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
