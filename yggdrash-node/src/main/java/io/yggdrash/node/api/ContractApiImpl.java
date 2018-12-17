package io.yggdrash.node.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImpl;
import io.yggdrash.common.util.Utils;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.BranchId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@AutoJsonRpcServiceImpl
public class ContractApiImpl implements ContractApi {

    private final BranchGroup branchGroup;

    @Autowired
    public ContractApiImpl(BranchGroup branchGroup) {
        this.branchGroup = branchGroup;
    }

    @Override
    public Object query(String branchId, String method, Map params) {
        JsonObject jsonParams = Utils.convertMapToJson(params);
        Object result = branchGroup.query(BranchId.of(branchId), method, jsonParams);
        if (result instanceof JsonElement) {
            return Utils.convertJsonToMap((JsonElement)result);
        }
        return result;
    }
}
