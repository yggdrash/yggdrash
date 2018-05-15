package io.yggdrash.core;

import io.yggdrash.util.HashUtils;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
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

//    public Block(Account author, BlockChain bc, Transactions txs) throws IOException {
//        this.header = new BlockHeader(author, bc, txs);
//        this.data = txs;
//    }

    public Block(Account author, Block prevBlock, Transactions transactionList) throws IOException {
        if(prevBlock == null){
            this.header = new BlockHeader(author, null, transactionList);
        }else{
            this.header = new BlockHeader(author, prevBlock.getHeader(), transactionList);
        }

        this.data = transactionList;

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

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public void printBlock() {
        System.out.println("<Block>");
        this.header.printBlockHeader();
        System.out.println("BlockBody=");
        if(this.data != null) this.data.printTransactions();
    }



}
