package io.yggdrash.node.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImpl;
import io.yggdrash.core.BranchGroup;
import io.yggdrash.core.exception.NonExistObjectException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@AutoJsonRpcServiceImpl
public class ContractApiImpl implements ContractApi {

    private static final Logger log = LoggerFactory.getLogger(ContractApiImpl.class);
    private final BranchGroup branchGroup;

    @Autowired
    public ContractApiImpl(BranchGroup branchGroup) {
        this.branchGroup = branchGroup;
    }

    @Override
    public String query(String data) {
        JsonParser jsonParser = new JsonParser();
        JsonObject query = (JsonObject) jsonParser.parse(data);
        if (!query.has("address")) {
            throw new NonExistObjectException("Address (BranchId) is required");
        }
        return branchGroup.query(query).toString();
    }
}
