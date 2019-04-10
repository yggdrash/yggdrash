package io.yggdrash.contract.yeed.propose;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.google.gson.JsonObject;
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
    long blockHeight;
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
        this.receiveAddress = object.get("receiveAddress").getAsString();
        this.receiveEth = object.get("receiveEth").getAsBigInteger();
        this.receiveChainId = object.get("receiveChainId").getAsInt();
        this.proposeType = ProposeType.fromValue(object.get("proposeType").getAsInt());
        this.senderAddress = object.get("senderAddress").getAsString();
        this.inputData = object.get("inputData").isJsonNull() ? null : object.get("inputData").getAsString();
        this.stakeYeed = object.get("stakeYeed").getAsBigInteger();
        this.blockHeight = object.get("blockHeight").getAsLong();
        this.fee = object.get("fee").getAsBigInteger();
        this.issuer = object.get("issuer").getAsString();
        generateProposeId();
    }


    public ProposeInterChain(String transactionId, String receiveAddress, BigInteger receiveEth,
                             int receiveChainId, ProposeType proposeType, String senderAddress,
                             String inputData, BigInteger stakeYeed, long blockHeight,
                             BigInteger fee, String issuer) {
        this.transactionId = transactionId;
        this.receiveAddress = receiveAddress;
        this.receiveEth = receiveEth;
        this.receiveChainId = receiveChainId;
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
            baos.write(receiveAddress.getBytes());
            baos.write(receiveEth.toByteArray());
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
        byte[] proposalID = HashUtil.sha3(proposalData);
        //System.out.println(Base64.getEncoder().encode(proposalID));

        this.proposeId = HexUtil.toHexString(proposalID);
    }

    public JsonObject toJsonObject() {
        JsonObject proposal = new JsonObject();
        proposal.addProperty("proposeId", proposeId);
        proposal.addProperty("transactionId", transactionId);
        proposal.addProperty("receiveAddress", receiveAddress);
        proposal.addProperty("receiveEth", receiveEth);
        proposal.addProperty("receiveChainId", receiveChainId);
        proposal.addProperty("proposeType", proposeType.toValue());
        proposal.addProperty("senderAddress", senderAddress);
        proposal.addProperty("inputData", inputData);
        proposal.addProperty("stakeYeed", stakeYeed);
        proposal.addProperty("blockHeight", blockHeight);
        proposal.addProperty("fee", fee);
        proposal.addProperty("issuer", issuer);

        return proposal;
    }

}
