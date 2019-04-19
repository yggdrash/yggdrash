package io.yggdrash.contract.yeed.propose;

import java.math.BigInteger;

public class ProcessTransaction {
    String sendAddress;
    String receiveAddress;
    int chainId;
    String targetAddress;
    BigInteger asset;

    public String getSendAddress() {
        return sendAddress;
    }

    public void setSendAddress(String sendAddress) {
        this.sendAddress = sendAddress;
    }

    public String getReceiveAddress() {
        return receiveAddress;
    }

    public void setReceiveAddress(String receiveAddress) {
        this.receiveAddress = receiveAddress;
    }

    public int getChainId() {
        return chainId;
    }

    public void setChainId(int chainId) {
        this.chainId = chainId;
    }

    public String getTargetAddress() {
        return targetAddress;
    }

    public void setTargetAddress(String targetAddress) {
        this.targetAddress = targetAddress;
    }

    public BigInteger getAsset() {
        return asset;
    }

    public void setAsset(BigInteger asset) {
        this.asset = asset;
    }
}
