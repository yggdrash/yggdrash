package io.yggdrash.core;

public class Block {
    Long index;
    String hash;
    String previousHash;
    Long timestamp;
    String data;

    public Block(Long index, String previousHash, Long timestamp, String data) {
        this.index = index;
        this.previousHash = previousHash;
        this.timestamp = timestamp;
        this.data = data;
        this.hash = calculateHash();
    }

    public Long nextIndex() {
        return this.index + 1;
    }

    public String calculateHash() {
        return HashUtils.hashString(mergeData());
    }

    public String mergeData() {
        return index + previousHash + timestamp + data;
    }

    void setData(String data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "Block{" +
                "index=" + index +
                ", hash='" + hash + '\'' +
                ", previousHash='" + previousHash + '\'' +
                ", timestamp=" + timestamp +
                ", data='" + data + '\'' +
                '}';
    }
}
