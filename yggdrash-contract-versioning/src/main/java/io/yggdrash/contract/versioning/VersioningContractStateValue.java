package io.yggdrash.contract.versioning;

import com.google.gson.JsonObject;
import io.yggdrash.common.utils.JsonUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * updatable contract version
 *
 */
public class VersioningContractStateValue {

    private static ContractSet contractSet;
    private static JsonObject json;
    private static String txId;
    private static ProposeContractSet proposeContractSet;
    private static ProposeContractSet.Votable votable;
    private static Map<String, ContractSet> contractMap = new HashMap<>();

    public VersioningContractStateValue(String txId) {
        this.txId = txId;
    }

    public VersioningContractStateValue(ContractSet contractSet) {
        this.contractSet = contractSet;
    }

    public void init() {
        contractSet = new ContractSet();
        proposeContractSet = new ProposeContractSet();
        contractSet.setTargetBlockHeight(0L);
        convertJson();
    }

    public JsonObject getJson() {
        return this.json;
    }

    public void upgrade() {

    }

    public void voting(ContractVote contractVote, String issuer) {
        //TODO 시간이 지나고 벨리데이터 셋 변경사항 체크
        ProposeContractSet.Votable votable = contractSet.getVotedState();
        if (contractVote.isAgree()) {
            votable.setAgreeCnt(votable.getAgreeCnt() + 1);
        } else {
            votable.setDisagreeCnt(votable.getDisagreeCnt() + 1);
        }
        votable.getVotedHistory().get(issuer).setAgree(contractVote.isAgree());
        votable.getVotedHistory().get(issuer).setVoted(true);

        //TODO set ContractSet votable
        contractSet.setVotedState(votable);
        convertJson();
    }

    public ContractSet getContract() {
        return contractSet;
    }

    public Map<String, ContractSet> getContractMap() {
        return contractMap;
    }

    public void setTargetContractVersion(String targetVersion) {
        contractSet.setTargetVersion(targetVersion);
        convertJson();
    }

    public void setBlockHeight(Long blockHeight) {
        contractSet.setTargetBlockHeight(blockHeight);
        convertJson();
    }

    public void setUpdateContract(byte[] updateContract) {
        contractSet.setUpdateContract(updateContract);
        convertJson();
    }

    public void setContract(ContractSet c) {
        contractSet = c;
        convertJson();
    }

    public void setVotable(String txId, Set<String> v) {
        ProposeContractSet.Votable votable = new ProposeContractSet.Votable(v);
        proposeContractSet.getContractVote().put(txId, votable);
        contractSet.setVotedState(votable);
        convertJson();
    }

    public static VersioningContractStateValue of(String txId) {
        return new VersioningContractStateValue(txId);
    }
    public static VersioningContractStateValue of(ContractSet contractSet) {
        return new VersioningContractStateValue(contractSet);
    }

    private void convertJson() {
        contractMap.put(txId, contractSet);
        json = JsonUtil.convertMapToJson(contractMap);

    }
}