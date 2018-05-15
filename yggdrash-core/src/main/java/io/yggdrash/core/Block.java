package io.yggdrash.core;

import java.io.IOException;
import java.io.Serializable;

public class Block implements Cloneable, Serializable {

    private BlockHeader header;
    private Transactions data;


    public Block(BlockHeader header, Transactions data) {
        this.header = header;
        this.data = data;
    }

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
