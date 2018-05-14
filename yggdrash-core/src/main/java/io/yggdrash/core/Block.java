package io.yggdrash.core;

import io.yggdrash.util.HashUtils;

import java.io.IOException;
import java.io.Serializable;
import java.util.Objects;

public class Block implements Cloneable, Serializable {

    // <Variable>
//    Long index;
//    String hash;
//    String previousHash;
//    Long timestamp;
//    String data;

    private BlockHeader header;
    private Transactions data;


    // <Constuctor>
//    public Block(Long index, String previousHash, Long timestamp, String data) {
//        this.index = index;
//        this.previousHash = previousHash;
//        this.timestamp = timestamp;
//        this.data = data;
//        this.hash = calculateHash();
//    }

    public Block(BlockHeader header, Transactions data) {
        this.header = header;
        this.data = data;
    }

    public Block(Account author, BlockChain bc, Transactions txs) throws IOException {
        this.header = new BlockHeader(author, bc, txs);
        this.data = txs;
    }

    public Block(Account author, byte[] pre_block_hash, long index, Transactions txs) throws IOException {
        this.header = new BlockHeader(author, pre_block_hash, index, txs);
        this.data = txs;
    }

    // <Get_Set Method>
    public BlockHeader getHeader() {
        return header;
    }

    public void setHeader(BlockHeader header) {
        this.header = header;
    }

    public Transactions getData() {
        return data;
    }

    public void setData(Transactions data) {
        this.data = data;
    }

//    void setData(String data) {
//        this.data = data;
//        this.header.sethash = calculateHash();
//    }


    // <Method>

//    public Long nextIndex() {
//        return this.index + 1;
//    }

//    public String calculateHash() {
//        return HashUtils.hashString(mergeData());
//    }

//    public String mergeData() {
//        return index + previousHash + timestamp + data;
//    }

//    @Override
//    public String toString() {
//        return "Block{" +
//                "index=" + index +
//                ", hash='" + hash + '\'' +
//                ", previousHash='" + previousHash + '\'' +
//                ", timestamp=" + timestamp +
//                ", data='" + data + '\'' +
//                '}';
//    }

//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (o == null || getClass() != o.getClass()) return false;
//        Block block = (Block) o;
//        return Objects.equals(index, block.index) &&
//                Objects.equals(hash, block.hash) &&
//                Objects.equals(previousHash, block.previousHash) &&
//                Objects.equals(timestamp, block.timestamp) &&
//                Objects.equals(data, block.data);
//    }

//    @Override
//    public int hashCode() {
//        return Objects.hash(index, hash, previousHash, timestamp, data);
//    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public void makeBlock(Account author, BlockChain bc, Transactions txs) throws IOException {

        // 1. set data
        this.data = txs;

        // 2. make header
        this.header = new BlockHeader(author, bc, txs);
    }

    public void printBlock() {
        System.out.println("<Block>");
        this.header.printBlockHeader();
        System.out.println("BlockBody=");
        if(this.data != null) this.data.printTransactions();
    }



}
