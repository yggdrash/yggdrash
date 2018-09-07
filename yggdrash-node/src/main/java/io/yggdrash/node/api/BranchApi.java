package io.yggdrash.node.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.googlecode.jsonrpc4j.JsonRpcError;
import com.googlecode.jsonrpc4j.JsonRpcErrors;
import com.googlecode.jsonrpc4j.JsonRpcParam;
import com.googlecode.jsonrpc4j.JsonRpcService;
import io.yggdrash.core.exception.NonExistObjectException;
import io.yggdrash.core.exception.WrongStructuredException;
import io.yggdrash.node.controller.TransactionDto;

import java.util.List;
import java.util.Map;

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
     * @param data query with branch id
     * @return branch
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = NonExistObjectException.class,
                    code = NonExistObjectException.code)})
    String viewBranch(String data) throws Exception;

    /**
     * Get the current contract address of the branch by branchId
     * @param data query with branch id
     * @return current version
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = NonExistObjectException.class,
                    code = NonExistObjectException.code)})
    String getCurrentVersionOfBranch(String data) throws Exception;

    /**
     * Get the versionHistory of the branch by branchId
     * @param data query with branch id
     * @return versionHistory
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = NonExistObjectException.class,
                    code = NonExistObjectException.code)})
    String getVersionHistoryOfBranch(String data) throws Exception;

    /**
     * Get all branch id
     * @param data query
     * @return list of all branch id
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = NonExistObjectException.class,
                    code = NonExistObjectException.code)})
    String getAllBranchId(String data) throws Exception;
}
