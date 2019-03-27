package io.yggdrash.contract.versioning;

import java.io.Serializable;

public class ContractSet implements Serializable, Comparable<ContractSet> {

    private String targetVersion;
    private ProposeContractSet.Votable votedState;
    private boolean isUpgrade;
    private long targetBlockHeight;
    private byte[] updateContract;
    private String txId;

    public ContractSet() {
    }

    public ContractSet(String txId) {
        this.isUpgrade = false;
    }

    public String getTargetVersion() {
        return targetVersion;
    }

    public void setTargetVersion(String targetVersion) {
        this.targetVersion = targetVersion;
    }

    public ProposeContractSet.Votable getVotedState() {
        return votedState;
    }

    public void setVotedState(ProposeContractSet.Votable votedState) {
        this.votedState = votedState;
    }

    public boolean isUpgrade() {
        return isUpgrade;
    }

    public void setUpgrade(boolean upgrade) {
        isUpgrade = upgrade;
    }

    public long gettargetBlockHeight() {
        return targetBlockHeight;
    }

    public void setTargetBlockHeight(long targetBlockHeight) {
        this.targetBlockHeight = targetBlockHeight;
    }

    public byte[] getUpdateContract() {
        return updateContract;
    }

    public void setUpdateContract(byte[] updateContract) {
        this.updateContract = updateContract;
    }

    @Override
    public int compareTo(ContractSet o) {
        return targetVersion.compareTo(o.targetVersion);

    }
}
