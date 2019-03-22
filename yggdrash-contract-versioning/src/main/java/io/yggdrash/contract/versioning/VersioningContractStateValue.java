package io.yggdrash.contract.versioning;

import com.google.gson.JsonObject;
import io.yggdrash.common.utils.JsonUtil;

/**
 * updatable contract version
 *
 */
public class VersioningContractStateValue {

    private static String targetContractVersion;
    private static ContractSet contractSet = new ContractSet();
    private static Contract contract = new Contract();
    private static JsonObject json;

    public VersioningContractStateValue(JsonObject json) {
        targetContractVersion = json.get("contractVersion").getAsString();
    }

    public void init() {
        contract = new Contract(targetContractVersion);
        contract.setTargetBlockHeight(0L);
        convertJson();
    }

    public JsonObject getJson() {
        return this.json;
    }

    public void updateContract() {

    }

    public void setBlockHeight(Long blockHeight) {
        contract.setTargetBlockHeight(blockHeight);
        convertJson();
    }

    public void setUpdateContract(byte[] updateContract) {
        contract.setUpdateContract(updateContract);
        convertJson();
    }

    public void setTxId(String txId) {
        contract.setTxId(txId);
        convertJson();
    }

    public static VersioningContractStateValue of(JsonObject json) {
        return new VersioningContractStateValue(json.deepCopy());
    }

    private void convertJson() {
        contractSet.getContractMap().put(targetContractVersion, contract);
        json = JsonUtil.parseJsonObject(JsonUtil.convertObjToString(contractSet.getContractMap()));
    }
}