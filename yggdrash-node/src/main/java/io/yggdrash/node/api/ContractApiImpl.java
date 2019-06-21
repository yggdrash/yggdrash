package io.yggdrash.node.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImpl;
import io.yggdrash.common.utils.JsonUtil;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.BranchId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
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
    public Object query(String branchId, String contractVersion, String method, Map params) {
        JsonObject jsonParams = null;

        if (params != null && !params.isEmpty()) {
            jsonParams = JsonUtil.convertMapToJson(params);
        }

        Object result = branchGroup.query(BranchId.of(branchId), contractVersion, method, jsonParams);
        if (result instanceof JsonElement) {
            return JsonUtil.convertJsonToMap((JsonElement)result);
        } else if (result instanceof BigInteger) {
            return String.valueOf(result);
        }

        return result;
    }

}
