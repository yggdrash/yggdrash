package io.yggdrash.node.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImpl;
import io.yggdrash.contract.StemContract;
import io.yggdrash.core.Runtime;
import io.yggdrash.node.controller.TransactionDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AutoJsonRpcServiceImpl
public class BranchApiImpl implements BranchApi {

    private static final Logger log = LoggerFactory.getLogger(BranchApiImpl.class);
    private final Runtime runtime;

    @Autowired
    public BranchApiImpl(Runtime runtime) {
        this.runtime = runtime;
    }

    @Override
    public String createBranch(TransactionDto tx) {
        return null;
    }

    @Override
    public String updateBranch(TransactionDto tx) {
        return null;
    }

    @Override
    public List<JsonObject> searchBranch(String key, String value) {
        return null;
    }

    @Override
    public String viewBranch(String branchId) {
        return null;
    }

    @Override
    public String getAllBranchName(String data) throws Exception {
        return queryOf(data);
        //return "getAllBranchName!";
    }

    @Override
    public String getCurrentVersionOfBranch(String branchId) {
        return null;
    }

    @Override
    public JsonArray getVersionHistoryOfBranch(String branchId) {
        return null;
    }

    private String queryOf(String data) throws Exception {
        log.debug("BranchAPI :: queryOf => " + data);
        JsonParser jsonParser = new JsonParser();
        JsonObject query = (JsonObject) jsonParser.parse(data);
        return runtime.query(new StemContract(), query).toString();
        //return query;
    }
}
