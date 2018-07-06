package io.yggdrash.node.mock;

import com.google.gson.JsonObject;
import io.yggdrash.core.Account;
import io.yggdrash.core.Transaction;
import io.yggdrash.core.TransactionHeader;
import io.yggdrash.core.format.TransactionFormat;
import io.yggdrash.crypto.ECKey;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.math.BigInteger;

public class TransactionMock implements TransactionFormat {

    public int version;
    public int type;
    public String timestamp;
    public String from;
    public String dataHash;
    public int dataSize;
    public String signature;
    public String transactionHash;
    public String transactionData;

    public TransactionMock() {
        super();
    }

    public TransactionMock(int version, int type, String timestamp, String from,
                           String dataHash, int dataSize, String signature,
                           String transactionHash, String transactionData) {
        this.version = version;
        this.type = type;
        this.timestamp = timestamp;
        this.from = from;
        this.dataHash = dataHash;
        this.dataSize = dataSize;
        this.signature = signature;
        this.transactionHash = transactionHash;
        this.transactionData = transactionData;
    }

    public int getVersion() {
        return version;
    }

    public int getType() {
        return type;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getFrom() {
        return from;
    }

    public String getDataHash() {
        return dataHash;
    }

    public int getDataSize() {
        return dataSize;
    }

    public String getSignature() {
        return signature;
    }

    public String getTransactionHash() {
        return transactionHash;
    }

    public String getTransactionData() {
        return transactionData;
    }

    @Override
    public String getHashString() throws IOException {
        return null;
    }

    @Override
    public byte[] getHash() throws IOException {
        return new byte[0];
    }

    @Override
    public String getData() {
        return null;
    }

    @Override
    public TransactionHeader getHeader() {
        return null;
    }


    public Transaction retTxMock() throws IOException {

        String privString = "8373419eea00d202d1752813c588109cb9560e7c8dccdc05c8b8fc5b74949438";
        BigInteger privateKey = new BigInteger(privString, 16);
        ECKey key = ECKey.fromPrivate(privateKey);

        Account from = new Account(key);
        byte[] pubKey = from.getAddress();
        String fromAddress = Hex.toHexString(from.getAddress());

        JsonObject txObj = new JsonObject();
        JsonObject txData = new JsonObject();

        txObj.addProperty("version", "0");
        txObj.addProperty("type", "00000000000000");
        txObj.addProperty("timestamp", "155810745733540");
        txObj.addProperty("from", fromAddress);
        txObj.addProperty("dataHash",
                "ba5f3ea40e95f49bce11942f375ebd3882eb837976eda5c0cb78b9b99ca7b485");
        txObj.addProperty("dataSize", "13");
        txObj.addProperty("signature",
                "b86e02880e12c575e56c5d15e1f491595219295076721a5bfb6042463d6a2d"
                        + "768331691db0b8de852390305c0f2b218e596e4a59bf54029cf6a8b9afdbb274104");
        txObj.addProperty("transactionHash",
                "c6b5e583ec18891e9de0e29c3f0358a5c99c474bc3ee78e90c618db72193c0");
        txObj.addProperty("transactionData", txData.toString());

        Transaction tx = new Transaction(from, txObj);

        return tx;
    }

    @Override
    public String toString() {
        return "TransactionMock{"
                + "version=" + version
                + ", type=" + type
                + ", timestamp='" + timestamp + '\''
                + ", from='" + from + '\''
                + ", dataHash='" + dataHash + '\''
                + ", dataSize=" + dataSize
                + ", signature='" + signature + '\''
                + ", transactionHash='" + transactionHash + '\''
                + ", transactionData='" + transactionData + '\''
                + '}';
    }
}
