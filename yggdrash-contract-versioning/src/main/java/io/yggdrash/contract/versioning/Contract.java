package io.yggdrash.contract.versioning;

import java.io.Serializable;

public class Contract implements Serializable, Comparable<Contract> {

    private String targetVersion;
    private ProposeContractSet.Votable votedHistory;
    private boolean isUpgrade;
    private long targetBlockHeight;
    private byte[] updateContract;
    private String txId;

    public Contract() {
    }

    public Contract(String txId) {
        this.txId = txId;
        this.isUpgrade = false;
    }

    public String getTargetVersion() {
        return targetVersion;
    }

    public void setTargetVersion(String targetVersion) {
        this.targetVersion = targetVersion;
    }

    public ProposeContractSet.Votable getVotedHistory() {
        return votedHistory;
    }

    public void setVotedHistory(ProposeContractSet.Votable votedHistory) {
        this.votedHistory = votedHistory;
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
    public int compareTo(Contract o) {
        return targetVersion.compareTo(o.targetVersion);

    }
}
