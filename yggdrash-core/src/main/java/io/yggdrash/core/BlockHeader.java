package io.yggdrash.core;

import java.io.Serializable;

public class BlockHeader implements Serializable {

    // Variable
    private byte version;
    private byte[] payload;
    private byte[] index;
    private long timestamp;
    private byte[] pre_block_hash;
    private byte[] author;
    private byte[] merkle_root;
    private long data_size;
    private byte[] signature;


    // Constructor

    public BlockHeader() {
        this.version = 0x00;
        this.payload = new byte[]{0x00,0x00,0x00,0x00,0x00,0x00,0x00};
    }


    // Method

    public void makeBlockHeader(){

//        // 1. get config (version, payload)
//        this.version = 0x00;
//        this.payload = {0x00,0x00,0x00,0x00,0x00,0x00,0x00};
//
//        // 2. get pre_block_info(pre_index, pre_block_hash)
//        this.index = getNextBlockIndex();
//        this.pre_block_hash = getPreBlockHash();
//
//        // 3. get account info(author)
//        this.author = getAccount();
//
//        // 4. get tx info (merkle_root, data_size)
//        this.merkle_root = getMerkleRoot(this.transactions);
//        this.data_size = this.transactions.size();
//
//        // 5. get signature (with timestamp)
//        this.timestamp = getCurrentTime();
//        this.signature = getSignature(this.transactions);


    }

}
