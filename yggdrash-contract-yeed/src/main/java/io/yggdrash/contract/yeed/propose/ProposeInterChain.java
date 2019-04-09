package io.yggdrash.contract.yeed.propose;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import io.yggdrash.common.crypto.HashUtil;
import io.yggdrash.common.crypto.HexUtil;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;

public class ProposeInterChain {

    String transactionId;
    String proposeId;

    String receiveAddress;
    BigInteger receiveEth;

    // Ethereum chain Id
    int receiveChainId;
    ProposeType proposeType;

    String senderAddress;
    String inputData;

    BigInteger stakeYeed;
    long targetBlockHeight;
    BigInteger fee;

    String issuer;

    public String getTransactionId() {
        return transactionId;
    }

    public String getProposeId() {
        return proposeId;
    }

    public String getReceiveAddress() {
        return receiveAddress;
    }

    public BigInteger getReceiveEth() {
        return receiveEth;
    }

    public int getReceiveChainId() {
        return receiveChainId;
    }

    public ProposeType getProposeType() {
        return proposeType;
    }

    public String getSenderAddress() {
        return senderAddress;
    }

    public String getInputData() {
        return inputData;
    }

    public BigInteger getStakeYeed() {
        return stakeYeed;
    }

    public long getTargetBlockHeight() {
        return targetBlockHeight;
    }

    public BigInteger getFee() {
        return fee;
    }

    public String getIssuer() {
        return issuer;
    }

    public ProposeInterChain(String transactionId, String receiveAddress, BigInteger receiveEth,
                             int receiveChainId, ProposeType proposeType, String senderAddress,
                             String inputData, BigInteger stakeYeed, long targetBlockHeight,
                             BigInteger fee, String issuer) {
        this.transactionId = transactionId;
        this.receiveAddress = receiveAddress;
        this.receiveEth = receiveEth;
        this.receiveChainId = receiveChainId;
        this.proposeType = proposeType;
        this.senderAddress = senderAddress;
        this.inputData = inputData;
        this.stakeYeed = stakeYeed;
        this.targetBlockHeight = targetBlockHeight;
        this.fee = fee;
        this.issuer = issuer;

        generateProposeId();
    }

    private void generateProposeId() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] proposalData;
        try {
            baos.write(transactionId.getBytes());
            baos.write(Ints.toByteArray(receiveChainId));
            baos.write(Ints.toByteArray(proposeType.toValue()));

            baos.write(issuer.getBytes());
            baos.write(receiveAddress.getBytes());
            baos.write(receiveEth.toByteArray());
            // Stake YEED
            baos.write(stakeYeed.toByteArray());
            // Target Block Height
            baos.write(Longs.toByteArray(targetBlockHeight));
            // sender is option
            if (senderAddress != null) {
                baos.write(senderAddress.getBytes());
            }

            // data is option
            if (inputData != null) {
                baos.write(inputData.getBytes());
            }

            proposalData = baos.toByteArray();
            baos.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("ProposeId is not valid");
        }

        // 32byte proposal ID
        byte[] proposalID = HashUtil.sha3(proposalData);
        this.proposeId = HexUtil.toHexString(proposalID);
    }
}
