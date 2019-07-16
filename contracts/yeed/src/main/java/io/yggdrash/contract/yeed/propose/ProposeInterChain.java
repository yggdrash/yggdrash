package io.yggdrash.contract.yeed.propose;

import com.google.common.base.Strings;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.google.gson.JsonObject;
import io.yggdrash.common.crypto.HashUtil;
import io.yggdrash.common.crypto.HexUtil;
import io.yggdrash.common.utils.JsonUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;

public class ProposeInterChain {

    String transactionId;
    String proposeId;

    String targetAddress;
    String receiveAddress;
    BigInteger receiveAsset;

    // Ethereum chain Id
    int receiveChainId;

    // add ethereum network block height - all process is work on network blockHeight
    long networkBlockHeight;

    ProposeType proposeType;

    String senderAddress;
    String inputData;

    BigInteger stakeYeed;
    long blockHeight;
    BigInteger fee;

    String issuer;

    public String getTransactionId() {
        return transactionId;
    }

    public String getProposeId() {
        return proposeId;
    }

    public String getTargetAddress() {
        return targetAddress;
    }

    public String getReceiveAddress() {
        return receiveAddress;
    }

    public BigInteger getReceiveAsset() {
        return receiveAsset;
    }

    public int getReceiveChainId() {
        return receiveChainId;
    }

    public long getNetworkBlockHeight() {
        return networkBlockHeight;
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

    public long getBlockHeight() {
        return blockHeight;
    }

    public BigInteger getFee() {
        return fee;
    }

    public String getIssuer() {
        return issuer;
    }

    public ProposeInterChain(JsonObject object) {
        this.transactionId = object.get("transactionId").getAsString();
        this.targetAddress = JsonUtil.parseString(object, "targetAddress", "");
        this.receiveAddress = object.get("receiveAddress").getAsString();
        this.receiveAsset = object.get("receiveAsset").getAsBigInteger();
        this.receiveChainId = object.get("receiveChainId").getAsInt();
        this.networkBlockHeight = object.get("networkBlockHeight").getAsLong();
        this.proposeType = ProposeType.fromValue(object.get("proposeType").getAsInt());
        this.senderAddress = object.get("senderAddress").getAsString();
        this.inputData = object.get("inputData").isJsonNull() ? null : object.get("inputData").getAsString();
        this.stakeYeed = object.get("stakeYeed").getAsBigInteger();
        this.blockHeight = object.get("blockHeight").getAsLong();
        this.fee = object.get("fee").getAsBigInteger();
        this.issuer = object.get("issuer").getAsString();
        generateProposeId();
    }


    public ProposeInterChain(String transactionId, String targetAddress, String receiveAddress, BigInteger receiveAsset,
                             int receiveChainId, long networkBlockHeight, ProposeType proposeType, String senderAddress,
                             String inputData, BigInteger stakeYeed, long blockHeight,
                             BigInteger fee, String issuer) {
        this.transactionId = transactionId;
        this.targetAddress = targetAddress;
        this.receiveAddress = receiveAddress;
        this.receiveAsset = receiveAsset;
        this.receiveChainId = receiveChainId;
        this.networkBlockHeight = networkBlockHeight;
        this.proposeType = proposeType;
        this.senderAddress = senderAddress;
        this.inputData = inputData;
        this.stakeYeed = stakeYeed;
        this.blockHeight = blockHeight;
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
            baos.write(targetAddress.getBytes());
            baos.write(receiveAddress.getBytes());
            baos.write(receiveAsset.toByteArray());
            baos.write(Longs.toByteArray(networkBlockHeight));
            // Stake YEED
            baos.write(stakeYeed.toByteArray());
            // Target Block Height
            baos.write(Longs.toByteArray(blockHeight));
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
        byte[] proposalId = HashUtil.sha3(proposalData);
        //System.out.println(Base64.getEncoder().encode(proposalID));

        this.proposeId = HexUtil.toHexString(proposalId);
    }

    public JsonObject toJsonObject() {
        JsonObject proposal = new JsonObject();
        proposal.addProperty("proposeId", proposeId);
        proposal.addProperty("transactionId", transactionId);
        proposal.addProperty("receiveAddress", receiveAddress);
        proposal.addProperty("receiveAsset", receiveAsset);
        proposal.addProperty("receiveChainId", receiveChainId);
        proposal.addProperty("networkBlockHeight", networkBlockHeight);
        proposal.addProperty("proposeType", proposeType.toValue());
        proposal.addProperty("senderAddress", senderAddress);
        proposal.addProperty("inputData", inputData);
        proposal.addProperty("stakeYeed", stakeYeed);
        proposal.addProperty("blockHeight", blockHeight);
        proposal.addProperty("fee", fee);
        proposal.addProperty("issuer", issuer);

        return proposal;
    }


    public int verificationProposeProcess(ProcessTransaction pt) {
        int checkProcess = 0;

        // check Send Address
        if (!Strings.isNullOrEmpty(getSenderAddress())) {
            checkProcess |= ProposeErrorCode.addCode(
                    getSenderAddress().equals(pt.getSendAddress()),
                    ProposeErrorCode.PROPOSE_SENDER_ADDRESS_INVALID);
        }

        // check Receive Address
        checkProcess |= ProposeErrorCode.addCode(
                getReceiveAddress().equals(pt.getReceiveAddress()),
                ProposeErrorCode.PROPOSE_RECEIVE_ADDRESS_INVALID);

        // Check Chain Id
        if (getReceiveChainId() != -1) {
            checkProcess |= ProposeErrorCode.addCode(
                    getReceiveChainId() == pt.getChainId(),
                    ProposeErrorCode.PROPOSE_RECEIVE_CHAIN_ID_INVALID);
        }

        // Check target Address - target (Token address or branch Address)
        if (!Strings.isNullOrEmpty(getTargetAddress())) {
            checkProcess |= ProposeErrorCode.addCode(
                    getTargetAddress().equals(pt.getTargetAddress()),
                    ProposeErrorCode.PROPOSE_RECEIVE_TARGET_INVALID);
        }

        if (pt.getAsset().compareTo(BigInteger.ZERO) == 0) {
            checkProcess |= ProposeErrorCode.PROPOSE_RECEIVE_TARGET_INVALID.toValue();
        }


        return checkProcess;
    }

    public boolean proposeSender(String senderAddress) {
        if (!Strings.isNullOrEmpty(this.senderAddress)) {
            return senderAddress.equals(this.senderAddress);
        }
        return false;
    }
}
