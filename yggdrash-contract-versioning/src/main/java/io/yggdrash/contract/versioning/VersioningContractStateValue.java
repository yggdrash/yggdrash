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

    private static final Long BLOCK_PEORID = 60480L; // default 7 days
    private static final Long HOUR = 1440L; // 1hour to minute
    private static final Long DAYS = 8640L; // 1days block
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
        ProposeContractSet.Votable votable = contractSet.getVotedState();
        if (contractVote.isAgree()) {
            votable.setAgreeCnt(votable.getAgreeCnt() + 1);
        } else {
            votable.setDisagreeCnt(votable.getDisagreeCnt() + 1);
        }
        votable.getVotedHistory().get(issuer).setAgree(contractVote.isAgree());
        votable.getVotedHistory().get(issuer).setVoted(true);
        contractSet.setVotedState(votable);

        if (contractSet.getVotedState().status()
                .equals(ProposeContractSet.Votable.VoteStatus.AGREE)) {
            contractSet.setUpgradable(true);
        }
        convertJson();
    }

    public ContractSet getContractSet() {
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
        Long targetBlockHeight = blockHeight + BLOCK_PEORID;
        contractSet.setTargetBlockHeight(targetBlockHeight);
        contractSet.setApplyBlockHeight(targetBlockHeight + DAYS);
        convertJson();
    }

    public void setBlockHeight(Long blockHeight, Long peorid) {
        Long targetBlockHeight = blockHeight + convertTargetBlockHeight(peorid);
        contractSet.setTargetBlockHeight(targetBlockHeight);
        contractSet.setApplyBlockHeight(targetBlockHeight + DAYS);
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

    private Long convertTargetBlockHeight(Long peorid) {
        // default more than 3 dyas
        if (peorid < 3) {
            return BLOCK_PEORID;
        }
        Long hour = peorid * HOUR;
        return hour * 6;
    }
}