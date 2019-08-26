package io.yggdrash.core.blockchain.osgi;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.yggdrash.contract.core.channel.ContractChannel;
import io.yggdrash.contract.core.channel.ContractMethodType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

public class ContractChannelCoupler implements ContractChannel {
    private static final Logger log = LoggerFactory.getLogger(ContractChannelCoupler.class);

    private Map<String, Object> serviceMap;
    private ContractCache cache;

    public void setContract(Map<String, Object> serviceMap, ContractCache cache) {
        this.serviceMap = serviceMap;
        this.cache = cache;
    }

    @Override
    public JsonObject call(String contractVersion, ContractMethodType type, String methodName, JsonObject params) {
        log.trace("Call {} {} {} ", contractVersion, type, methodName);
        Object service = serviceMap.get(contractVersion);
        if (service == null) {
            log.error("This service that contract version {} is not registered", contractVersion);
            return null;
        }

        Method method = cache.getContractMethodMap(contractVersion, type, service).get(methodName);

        if (method == null) {
            log.error("Not found contract method: {}", methodName);
            return null;
        }

        Object result = null;
        try {
            if (method.getParameterCount() == 0) {
                result = method.invoke(service);
            } else {
                result = method.invoke(service, params);
            }
        } catch (IllegalAccessException e) {
            log.error("CallContractMethod : {} and bundle {} ", methodName, contractVersion);
        } catch (InvocationTargetException e) {
            log.error("{} occurred error in {} caused by {}", methodName, contractVersion, e.getCause());
        }

        if (result instanceof JsonObject) {
            return (JsonObject) result;
        } else if (result != null) {
            Gson gson = new Gson();
            JsonElement element = gson.toJsonTree(result);
            JsonObject resultObject = new JsonObject();
            resultObject.add("result", element);
            return resultObject;
        }
        return null;
    }
}
