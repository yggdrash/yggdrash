package io.yggdrash.core;

import java.util.Objects;

public class Block implements Cloneable {
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
        this.hash = calculateHash();
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Block block = (Block) o;
        return Objects.equals(index, block.index) &&
                Objects.equals(hash, block.hash) &&
                Objects.equals(previousHash, block.previousHash) &&
                Objects.equals(timestamp, block.timestamp) &&
                Objects.equals(data, block.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, hash, previousHash, timestamp, data);
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
