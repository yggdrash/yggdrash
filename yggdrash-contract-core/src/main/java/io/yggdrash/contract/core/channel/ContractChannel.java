package io.yggdrash.contract.core.channel;

import com.google.gson.JsonObject;

public interface ContractChannel {
    JsonObject call (String contractVersion, ContractMethodType type, String methodName, JsonObject params);
}
