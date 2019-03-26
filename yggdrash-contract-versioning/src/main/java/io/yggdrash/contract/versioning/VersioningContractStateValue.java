package io.yggdrash.contract.versioning;

import com.google.gson.JsonObject;
import io.yggdrash.common.contract.vo.dpoa.ValidatorSet;
import io.yggdrash.common.utils.JsonUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * updatable contract version
 *
 */
public class VersioningContractStateValue {

    private static Contract contract;
    private static JsonObject json;
    private static String txId;
    private static ProposeContractSet proposeContractSet;
    private static ProposeContractSet.Votable votable;
    private static Map<String, Contract> contractMap = new HashMap<>();

    public VersioningContractStateValue(String txId) {
        this.txId = txId;
    }

    public void init() {
        contract = new Contract(txId);
        proposeContractSet = new ProposeContractSet();
        contract.setTargetBlockHeight(0L);
        convertJson();
    }

    public JsonObject getJson() {
        return this.json;
    }

    public void upgrade() {

    }

    public void voting() {

    }

    public Contract getContract() {
        return contract;
    }

    public Map<String, Contract> getContractMap() {
        return contractMap;
    }

    public void setTargetContractVersion(String targetVersion) {
        contract.setTargetVersion(targetVersion);
        convertJson();
    }

    public void setBlockHeight(Long blockHeight) {
        contract.setTargetBlockHeight(blockHeight);
        convertJson();
    }

    public void setUpdateContract(byte[] updateContract) {
        contract.setUpdateContract(updateContract);
        convertJson();
    }

    public void setContract(Contract c) {
        contract = c;
        convertJson();
    }

    public void setVotable(String txId, ValidatorSet v) {
        ProposeContractSet.Votable votable = new ProposeContractSet.Votable(txId, v);
        proposeContractSet.getContractVote().put(txId, votable);
        contract.setVotedHistory(votable);
    }

    public static VersioningContractStateValue of(String txId) {
        return new VersioningContractStateValue(txId);
    }

    private void convertJson() {
        contractMap.put(txId, contract);
        json = JsonUtil.convertMapToJson(contractMap);

    }
}