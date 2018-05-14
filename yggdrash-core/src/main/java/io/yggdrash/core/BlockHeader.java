package io.yggdrash.core;

import io.yggdrash.crypto.Signature;
import io.yggdrash.util.HashUtils;
import io.yggdrash.util.SerializeUtils;
import io.yggdrash.util.TimeUtils;
import org.apache.commons.codec.binary.Hex;

import java.io.IOException;
import java.io.Serializable;

public class BlockHeader implements Serializable {

    // <Variable>
    private byte version;
    private byte[] payload;
    private long index;
    private long timestamp;
    private byte[] pre_block_hash;
    private byte[] author;
    private byte[] merkle_root;
    private long data_size;
    private byte[] signature;


    // <Constructor>

    public BlockHeader(Account author, BlockChain bc, Transactions txs) throws IOException {
        this.version = 0x00;
        this.payload = new byte[7];

        makeBlockHeader(author, bc, txs);
    }

    public BlockHeader(Account author, byte[] pre_block_hash, long index, Transactions txs) throws IOException {
        this.version = 0x00;
        this.payload = new byte[7];

        makeBlockHeader(author, pre_block_hash, index, txs);
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

    public byte[] getPre_block_hash() {
        return pre_block_hash;
    }

    public void setPre_block_hash(byte[] pre_block_hash) {
        this.pre_block_hash = pre_block_hash;
    }

    public byte[] getAuthor() {
        return author;
    }

    public void setAuthor(byte[] author) {
        this.author = author;
    }

    public byte[] getMerkle_root() {
        return merkle_root;
    }

    public void setMerkle_root(byte[] merkle_root) {
        this.merkle_root = merkle_root;
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

        // 1. set pre_block_info(index, pre_block_hash)
        if(bc == null || bc.getPreviousBlock() == null) {
            this.index = 0;
            this.pre_block_hash = null;
        } else {
            this.index = bc.getPreviousBlock().getHeader().getIndex() + 1;
            this.pre_block_hash = bc.getPreviousBlock().getHeader().getHash();
        }

        // 2. set author
        this.author = author.getKey().getPub_key();

        // 3. set txs info (merkle_root, data_size)
        if(txs == null) {
            this.merkle_root = null;
            this.data_size = 0;
        } else {
            this.merkle_root = txs.getMerkleRoot();
            this.data_size = txs.getSize();
        }

        // 4. set signature (with timestamp)
        this.timestamp = TimeUtils.getCurrenttime();
        this.signature = null;
        this.signature = Signature.sign(author.getKey(), SerializeUtils.serialize(this));
    }

    public void makeBlockHeader(Account author, byte[] pre_block_hash, long index, Transactions txs) throws IOException {

        // 1. set pre_block_info(index, pre_block_hash)
        if(index == 0 && pre_block_hash == null) {
            this.index = 0;
            this.pre_block_hash = null;
        } else {
            this.index = index;
            this.pre_block_hash = pre_block_hash;
        }

        // 2. set author
        this.author = author.getKey().getPub_key();

        // 3. set txs info (merkle_root, data_size)
        this.merkle_root = null;
        this.data_size = 0;

        // 4. set signature (with timestamp)
        this.timestamp = TimeUtils.getCurrenttime();
        this.signature = null;
        this.signature = Signature.sign(author.getKey(), SerializeUtils.serialize(this));
    }

    public byte[] getHash() throws IOException {
        return HashUtils.sha256(SerializeUtils.serialize(this));
    }

    public void printBlockHeader() {
        System.out.println("<BlockHeader>");
        System.out.println("version=" + Integer.toHexString(this.version));
        if(this.payload != null) System.out.println("payload=" + Hex.encodeHexString(this.payload));
        System.out.println("index=" + this.index);
        System.out.println("timestamp=" + this.timestamp);
        if(this.pre_block_hash != null) System.out.println("pre_block_hash="+Hex.encodeHexString(this.pre_block_hash));
        if(this.author != null) System.out.println("author=" + Hex.encodeHexString(this.author));
        if(this.merkle_root != null) System.out.println("merkle_root=" + Hex.encodeHexString(this.merkle_root));
        System.out.println("data_size=" + this.data_size);
        if(this.signature != null) System.out.println("signature=" + Hex.encodeHexString(this.signature));
    }


}
