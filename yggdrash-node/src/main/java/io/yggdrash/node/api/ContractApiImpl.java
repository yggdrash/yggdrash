package io.yggdrash.node.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImpl;
import io.yggdrash.common.util.JsonUtil;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.contract.ContractId;
import io.yggdrash.core.contract.ContractManager;
import io.yggdrash.core.contract.TransactionReceipt;
import io.yggdrash.core.exception.FailedOperationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@AutoJsonRpcServiceImpl
public class ContractApiImpl implements ContractApi {

    private final BranchGroup branchGroup;
    private final ContractManager contractManager;

    @Autowired
    public ContractApiImpl(BranchGroup branchGroup, ContractManager contractManager) {
        this.branchGroup = branchGroup;
        this.contractManager = contractManager;
    }

    @Override
    public Object query(String branchId, String method, Map params) {
        JsonObject jsonParams = null;
        if (params != null && !params.isEmpty()) {
            jsonParams = JsonUtil.convertMapToJson(params);
        }
        Object result = branchGroup.query(BranchId.of(branchId), method, jsonParams);
        if (result instanceof JsonElement) {
            return JsonUtil.convertJsonToMap((JsonElement)result);
        }
        return result;
    }

    @Override
    public Object contract(String method, Map params) {
        try {
            if (params.size() > 0) {
                return contractManager.getClass().getMethod(method, JsonObject.class)
                        .invoke(contractManager, JsonUtil.convertMapToJson(params));
            } else {
                return contractManager.getClass().getMethod(method).invoke(contractManager);
            }
        } catch (Exception e) {
            throw new FailedOperationException(e);
        }
    }

}
