package io.yggdrash.node.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImpl;
import io.yggdrash.common.util.JsonUtil;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.contract.ContractId;
import io.yggdrash.core.contract.ContractManager;
import io.yggdrash.core.contract.ContractMeta;
import io.yggdrash.core.exception.FailedOperationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    public Object contract(String contractId, String method) {
        try {
            if (contractId.getBytes().length > 0) {
                if (!contractManager.isContract(ContractId.of(contractId))) return false;
                Object result = contractManager.getClass().getMethod(method, ContractId.class)
                        .invoke(contractManager, ContractId.of(contractId));
                if (result instanceof ContractMeta) {
                    return JsonUtil.convertJsonToMap(((ContractMeta) result).toJsonObject());
                }
                return result;
            } else {
                Object result = contractManager.getClass().getMethod(method).invoke(contractManager);
                if (result instanceof ContractMeta) {
                    return JsonUtil.convertJsonToMap(((ContractMeta) result).toJsonObject());
                }
                return result;
            }
        } catch (Exception e) {
            throw new FailedOperationException(e);
        }
    }
}
