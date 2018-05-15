package io.yggdrash.core;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;

public class Block implements Cloneable, Serializable {
    private final static Logger log = LoggerFactory.getLogger(Block.class);

    private BlockHeader header;
    private Transactions data;

    public Block(BlockHeader header, Transactions data) {
        this.header = header;
        this.data = data;
    }

    public Block(Account author, Block prevBlock, Transactions transactionList) throws IOException {
        if (prevBlock == null) {
            this.header = new BlockHeader(author, null, transactionList);
        } else {
            this.header = new BlockHeader(author, prevBlock.getHeader(), transactionList);
        }

        this.data = transactionList;
    }

    public Block(Account author, byte[] pre_block_hash, long index, Transactions txs) throws IOException {
        this.header = new BlockHeader(author, pre_block_hash, index, txs);
        this.data = txs;
    }

    public BlockHeader getHeader() {
        return header;
    }

    public String getBlockHash() {
        return bytesToHexString(header.getBlockHash());
    }

    public String getPrevBlockHash() {
        return bytesToHexString(header.getPrevBlockHash());
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public void printBlock() {
        // TODO toString overwrite
        System.out.println("<Block>");
        this.header.printBlockHeader();
        System.out.println("BlockBody=");
        if (this.data != null) this.data.printTransactions();
    }

    private String bytesToHexString(byte[] bytes) {
        return Hex.encodeHexString(bytes);
    }
}
