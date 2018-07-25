package io.yggdrash.node.mock;

import com.fasterxml.jackson.annotation.JsonRootName;

@JsonRootName(value = "header")
public class TransactionHeaderMock {
    public final int type;
    public final int version;
    public final String dataHash;
    public final String timestamp;
    public final int dataSize;
    public final String signature;

    public TransactionHeaderMock(int type, int version, String dataHash, String timestamp,
                                 int dataSize, String signature) {
        this.type = type;
        this.version = version;
        this.dataHash = dataHash;
        this.timestamp = timestamp;
        this.dataSize = dataSize;
        this.signature = signature;
    }

    public int getType() {
        return type;
    }

    public int getVersion() {
        return version;
    }

    public String getDataHash() {
        return dataHash;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public int getDataSize() {
        return dataSize;
    }

    public String getSignature() {
        return signature;
    }
}

