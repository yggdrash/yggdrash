package io.yggdrash.core;

import io.yggdrash.crypto.Signature;
import io.yggdrash.util.HashUtils;
import io.yggdrash.util.SerializeUtils;
import io.yggdrash.util.TimeUtils;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;

public class BlockHeader implements Serializable {
    private static final Logger log = LoggerFactory.getLogger(BlockHeader.class);

    private byte version;
    private byte[] payload;
    private long index;
    private long timestamp;
    private byte[] prevBlockHash;
    private byte[] author;
    private byte[] merkleRoot;
    private long data_size;
    private byte[] signature;

    public BlockHeader(Account author, byte[] prevBlockHash, long index, Transactions txs) throws IOException {
        this.version = 0x00;
        this.payload = new byte[7];

        makeBlockHeader(author, prevBlockHash, index, txs);
    }

    public BlockHeader(Account author, BlockHeader prevBlockHeader, Transactions transactionList) throws IOException {
        // TODO 정합성 검토
        this.version = 0x00;
        this.payload = new byte[7];
        if(prevBlockHeader == null) {
            // genensis block
            makeBlockHeader(author, null, 0, transactionList);
        }else{
            makeBlockHeader(author, prevBlockHeader.getBlockHash(), prevBlockHeader.getIndex()+1, transactionList);
        }

    }

    // <Get_Set Method>

    public byte getVersion() {
        return version;
    }

    public void setVersion(byte version) {
        this.version = version;
    }

    public byte[] getPayload() {
        return payload;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    public long getIndex() {
        return index;
    }

    public void setIndex(long index) {
        this.index = index;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public byte[] getPrevBlockHash() {
        return prevBlockHash;
    }

    public void setPrevBlockHash(byte[] prevBlockHash) {
        this.prevBlockHash = prevBlockHash;
    }

    public byte[] getAuthor() {
        return author;
    }

    public void setAuthor(byte[] author) {
        this.author = author;
    }

    public byte[] getMerkleRoot() {
        return merkleRoot;
    }

    public void setMerkleRoot(byte[] merkleRoot) {
        this.merkleRoot = merkleRoot;
    }

    public long getData_size() {
        return data_size;
    }

    public void setData_size(long data_size) {
        this.data_size = data_size;
    }

    public byte[] getSignature() {
        return signature;
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }


    // <Method>

    public void makeBlockHeader(Account author, BlockChain bc, Transactions txs) throws IOException {

        // 1. set pre_block_info(index, prevBlockHash)
        if(bc == null || bc.getPrevBlock() == null) {
            // Genesys Block
            this.index = 0;
            this.prevBlockHash = null;
        } else {
            log.debug(bc.getPrevBlock().getBlockHash());
            this.index = bc.getPrevBlock().getHeader().getIndex() + 1;
            this.prevBlockHash = bc.getPrevBlock().getHeader().getBlockHash();
        }

        // 2. set author
        this.author = author.getKey().getPub_key();

        // 3. set txs info (merkleRoot, data_size)
        if(txs == null) {
            this.merkleRoot = null;
            this.data_size = 0;
        } else {
            this.merkleRoot = txs.getMerkleRoot();
            this.data_size = txs.getSize();
        }

        // 4. set signature (with timestamp)
        this.timestamp = TimeUtils.getCurrenttime();
        this.signature = null;
        this.signature = Signature.sign(author.getKey(), SerializeUtils.serialize(this));
    }

    public void makeBlockHeader(Account author, byte[] pre_block_hash, long index, Transactions txs) throws IOException {

        // 1. set pre_block_info(index, prevBlockHash)
        if(index == 0 && pre_block_hash == null) {
            this.index = 0;
            this.prevBlockHash = null;
        } else {
            this.index = index;
            this.prevBlockHash = pre_block_hash;
        }

        // 2. set author
        this.author = author.getKey().getPub_key();

        // 3. set txs info (merkleRoot, data_size)
        this.merkleRoot = null;
        this.data_size = 0;

        // 4. set signature (with timestamp)
        this.timestamp = TimeUtils.getCurrenttime();
        this.signature = null;
        this.signature = Signature.sign(author.getKey(), SerializeUtils.serialize(this));
    }

    public byte[] getBlockHash() {
        byte[] bytes = new byte[0];

        try {
            bytes = HashUtils.sha256(SerializeUtils.serialize(this));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return bytes;
    }


    public void printBlockHeader() {
        System.out.println("<BlockHeader>");
        System.out.println("version=" + Integer.toHexString(this.version));
        if(this.payload != null) System.out.println("payload=" + Hex.encodeHexString(this.payload));
        System.out.println("index=" + this.index);
        System.out.println("timestamp=" + this.timestamp);
        if(this.prevBlockHash != null) System.out.println("prevBlockHash="+Hex.encodeHexString(this.prevBlockHash));
        if(this.author != null) System.out.println("author=" + Hex.encodeHexString(this.author));
        if(this.merkleRoot != null) System.out.println("merkleRoot=" + Hex.encodeHexString(this.merkleRoot));
        System.out.println("data_size=" + this.data_size);
        if(this.signature != null) System.out.println("signature=" + Hex.encodeHexString(this.signature));
    }
}
