package io.yggdrash.core.blockchain.osgi;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.yggdrash.contract.core.channel.ContractChannel;
import io.yggdrash.contract.core.channel.ContractMethodType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

public class ContractChannelCoupler implements ContractChannel {

    Map<String, Object> contractMap;
    ContractCache cache;

    public void setContract(Map<String, Object> contractMap, ContractCache cache) {
        this.contractMap = contractMap;
        this.cache = cache;
    }

    @Override
    public JsonObject call(String contractVersion, ContractMethodType type, String methodName, JsonObject params) {
        Map<String, Method> contractMethodMap = null;
        switch (type) {
            case QUERY:
                contractMethodMap = cache.getQueryMethods().get(contractVersion);
                break;
            case INVOKE:
                contractMethodMap = cache.getInvokeTransactionMethods().get(contractVersion);
                break;
        }
        if (contractMethodMap != null) {
            Method targetMethod = contractMethodMap.get(methodName);
            Object contract = contractMap.get(contractVersion);
            if (targetMethod != null) {
                try {
                    Object result = targetMethod.invoke(contract, params);
                    if (result instanceof JsonObject) {
                        return (JsonObject) result;
                    } else if (result != null) {
                        Gson gson = new Gson();
                        JsonElement element = gson.toJsonTree(result);
                        JsonObject resultObject = new JsonObject();
                        resultObject.add("result", element);
                        return resultObject;
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
}
