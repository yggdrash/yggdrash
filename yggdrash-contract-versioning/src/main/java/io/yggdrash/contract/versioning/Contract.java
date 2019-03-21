package io.yggdrash.contract.versioning;

import io.yggdrash.common.contract.vo.dpoa.ProposeValidatorSet;

import java.io.Serializable;

public class Contract implements Serializable, Comparable<Contract> {

    private String version;
    private ProposeValidatorSet.Votable votedHistory;
    private boolean isUpgrade;
    private long targetBlockHeight;

    public Contract() {
    }

    public Contract(String version) { this.version = version; }

    public String getAddr() {
        return version;
    }

    public void setAddr(String addr) {
        this.version = addr;
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

    public void settargetBlockHeight(long targetBlockHeight) {
        this.targetBlockHeight = targetBlockHeight;
    }

    @Override
    public int compareTo(Contract o) {
        return version.compareTo(o.version);

    }
}
