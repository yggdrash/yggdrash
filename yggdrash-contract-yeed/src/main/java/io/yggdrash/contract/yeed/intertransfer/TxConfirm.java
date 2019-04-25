package io.yggdrash.contract.yeed.intertransfer;

import com.google.gson.JsonObject;
import io.yggdrash.common.crypto.HashUtil;
import io.yggdrash.common.crypto.HexUtil;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;

public class TxConfirm {
    String txConfirmId;
    String proposeId;
    String txId;

    String sendAddress;
    BigInteger transferYeed;

    long blockHeight;
    long lastBlockHeight;
    int index;

    TxConfirmStatus status;

    public String getTxConfirmId() {
        return txConfirmId;
    }

    public String getProposeId() {
        return proposeId;
    }

    public String getTxId() {
        return txId;
    }

    public String getSendAddress() {
        return sendAddress;
    }

    public BigInteger getTransferYeed() {
        return transferYeed;
    }

    public long getBlockHeight() {
        return blockHeight;
    }

    public void setBlockHeight(long blockHeight) {
        this.blockHeight = blockHeight;
    }

    public long getLastBlockHeight() {
        return lastBlockHeight;
    }

    public void setLastBlockHeight(long lastBlockHeight) {
        this.lastBlockHeight = lastBlockHeight;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public TxConfirmStatus getStatus() {
        return status;
    }

    public void setStatus(TxConfirmStatus status) {
        this.status = status;
    }

    public TxConfirm(String proposeId, String txId, String sendAddress, BigInteger transferYeed) {
        this.proposeId = proposeId;
        this.txId = txId;
        this.sendAddress = sendAddress;
        this.transferYeed = transferYeed;
        generateTxConfirmId();

        status = TxConfirmStatus.VALIDATE_REQUIRE;
    }

    public TxConfirm(JsonObject json) {
        this.proposeId = json.get("proposeId").getAsString();
        this.txId = json.get("txId").getAsString();
        this.sendAddress = json.get("sendAddress").getAsString();
        this.transferYeed = json.get("transferYeed").getAsBigInteger();
        generateTxConfirmId();

        this.blockHeight = json.get("blockHeight").getAsLong();
        this.lastBlockHeight = json.get("lastBlockHeight").getAsLong();
        this.index = json.get("index").getAsInt();

        this.status = TxConfirmStatus.fromValue(json.get("status").getAsInt());
    }

    private void generateTxConfirmId() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] transactionConfirmData;

        try {
            baos.write(proposeId.getBytes());
            baos.write(txId.getBytes());
            baos.write(sendAddress.getBytes());
            baos.write(transferYeed.toByteArray());

            transactionConfirmData = baos.toByteArray();
            baos.close();
        } catch (IOException e) {
            throw new RuntimeException("TxConfirmId is not valid");
        }

        byte[] txConfirmData = HashUtil.sha3(transactionConfirmData);
        this.txConfirmId = HexUtil.toHexString(txConfirmData);
    }

    public JsonObject toJsonObject() {
        JsonObject txConfirmJson = new JsonObject();
        txConfirmJson.addProperty("txConfirmId", txConfirmId);
        txConfirmJson.addProperty("proposeId", proposeId);
        txConfirmJson.addProperty("txId", txId);
        txConfirmJson.addProperty("sendAddress", sendAddress);
        txConfirmJson.addProperty("transferYeed", transferYeed);

        txConfirmJson.addProperty("blockHeight", blockHeight);
        txConfirmJson.addProperty("lastBlockHeight", lastBlockHeight);
        txConfirmJson.addProperty("index", index);
        txConfirmJson.addProperty("status", status.toValue());

        return txConfirmJson;

    }
}
