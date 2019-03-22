package io.yggdrash.contract.versioning;

import io.yggdrash.common.contract.vo.dpoa.ProposeValidatorSet;

import java.io.Serializable;

public class Contract implements Serializable, Comparable<Contract> {

    private String version;
    private ProposeValidatorSet.Votable votedHistory;
    private boolean isUpgrade;
    private long targetBlockHeight;
    private byte[] updateContract;
    private String txId;

    public Contract() {
    }

    public Contract(String version) { this.version = version; }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public ProposeValidatorSet.Votable getVotedHistory() {
        return votedHistory;
    }

    public void setVotedHistory(ProposeValidatorSet.Votable votedHistory) {
        this.votedHistory = votedHistory;
    }

    boolean isUpgrade() {
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

    public String getTxId() {
        return this.txId;
    }

    public void setTxId(String txId) {
        this.txId = txId;
    }

    @Override
    public int compareTo(Contract o) {
        return version.compareTo(o.version);

    }
}
