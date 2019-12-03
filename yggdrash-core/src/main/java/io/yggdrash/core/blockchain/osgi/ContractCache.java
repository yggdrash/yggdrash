package io.yggdrash.core.blockchain.osgi;

import io.yggdrash.contract.core.channel.ContractMethodType;

import java.lang.reflect.Method;
import java.util.Map;

public interface ContractCache {
    void cacheContract(String contractVersion, Object service);

    Map<String, Method> getContractMethodMap(String contractVersion, ContractMethodType type, Object service);

    void flush(String contractVersion);
}
