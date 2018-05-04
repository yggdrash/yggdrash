package io.yggdrash.core;

public class Block {
    Long index;
    String hash;
    String previousHash;
    Long timestamp;
    String data;

    public Block(Long index, String hash, String previousHash, Long timestamp, String data) {
        this.index = index;
        this.hash = hash;
        this.previousHash = previousHash;
        this.timestamp = timestamp;
        this.data = data;
    }

    public String getHash() {
        return calculateHash();
    }

    @Override
    public String toString() {
        return index + hash + previousHash + timestamp + data;
    }

    private String calculateHash() {
        return HashUtils.sha256Hex(toString());
    }
}
