package io.yggdrash.core.blockchain.osgi.service;

import java.io.Serializable;

public class ContractSet implements Serializable, Comparable<ContractSet> {

    private String targetVersion;
    private ProposeContractSet.Votable votedState;
    private boolean isUpgradable;
    private long targetBlockHeight;
    private long applyBlockHeight;
    private byte[] updateContract;
    private String txId;

    public ContractSet() {
    }

    public ContractSet(String txId) {
        this.isUpgradable = false;
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

    public boolean isUpgradable() {
        return isUpgradable;
    }

    public void setUpgradable(boolean isUpgradable) {
        this.isUpgradable = isUpgradable;
    }

    public long getTargetBlockHeight() {
        return targetBlockHeight;
    }

    public void setTargetBlockHeight(long targetBlockHeight) {
        this.targetBlockHeight = targetBlockHeight;
    }

    public long getApplyBlockHeight() {
        return applyBlockHeight;
    }

    public void setApplyBlockHeight(long applyBlockHeight) {
        this.applyBlockHeight = applyBlockHeight;
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
