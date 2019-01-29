package io.yggdrash.node.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImpl;
import io.yggdrash.common.util.JsonUtil;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.contract.*;
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
    public Object contract(String contractId, String method) {
        ContractInfo contractInfo = new ContractInfo(contractManager);
        try {
            if (contractId.getBytes().length > 0) {
                if (!contractManager.isContract(ContractId.of(contractId))) return false;
                return contractInfo.getClass().getMethod(method, ContractId.class)
                        .invoke(contractInfo, ContractId.of(contractId));
            } else {
                return contractInfo.getClass().getMethod(method).invoke(contractInfo);
            }
        } catch (Exception e) {
            throw new FailedOperationException(e);
        }
    }
}
