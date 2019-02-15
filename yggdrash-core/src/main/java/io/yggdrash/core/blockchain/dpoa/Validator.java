package io.yggdrash.core.blockchain.dpoa;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.yggdrash.core.blockchain.SerialEnum;

import java.io.Serializable;

public class Validator implements Serializable, Comparable<Validator> {
    private static final long serialVersionUID = SerialEnum.VALIDATOR.toValue();

    private String addr;

    private ProposeValidatorSet.Votable votedHistory;

    private boolean isFreezing;
    private FreezingType freezingType;
    private long freezingBlockHeight;
    private int disconnectCnt;

    public Validator() {

    }

    public Validator(String addr) {
        this.addr = addr;
    }

    public Validator(String addr, ProposeValidatorSet.Votable votedHistory) {
        this.addr = addr;
        this.votedHistory = votedHistory;
    }

    public String getAddr() {
        return addr;
    }

    public void setAddr(String addr) {
        this.addr = addr;
    }

    public ProposeValidatorSet.Votable getVotedHistory() {
        return votedHistory;
    }

    public void setVotedHistory(ProposeValidatorSet.Votable votedHistory) {
        this.votedHistory = votedHistory;
    }

    public boolean isFreezing() {
        return isFreezing;
    }

    public void setFreezing(boolean freezing) {
        isFreezing = freezing;
    }

    public FreezingType getFreezingType() {
        return freezingType;
    }

    public void setFreezingType(FreezingType freezingType) {
        this.freezingType = freezingType;
    }

    public long getFreezingBlockHeight() {
        return freezingBlockHeight;
    }

    public void setFreezingBlockHeight(long freezingBlockHeight) {
        this.freezingBlockHeight = freezingBlockHeight;
    }

    public int getDisconnectCnt() {
        return disconnectCnt;
    }

    public void setDisconnectCnt(int disconnectCnt) {
        this.disconnectCnt = disconnectCnt;
    }

    @Override
    public int compareTo(Validator o) {
        return addr.compareTo(o.addr);
    }

    public enum FreezingType {
        BYZANTINE(1), DISCONNECTED(2);

        private int value;

        FreezingType(int value) {
            this.value = value;
        }

        @JsonCreator
        public static FreezingType fromValue(int value) {
            switch (value) {
                case 1:
                    return BYZANTINE;
                case 2:
                    return DISCONNECTED;
                default:
                    return null;
            }
        }

        @JsonValue
        public int toValue() {
            return this.value;
        }
    }
}
