package io.yggdrash.core;

import io.yggdrash.crypto.Signature;
import io.yggdrash.util.HashUtils;
import io.yggdrash.util.SerializeUtils;
import io.yggdrash.util.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private long dataSize;
    private byte[] signature;

    public BlockHeader(Account author, byte[] prevBlockHash, long index, BlockBody txs) {
        this.version = 0x00;
        this.payload = new byte[7];

        makeBlockHeader(author, prevBlockHash, index, txs);
    }

    public BlockHeader(Account author, Block prevBlock, BlockBody transactionList) {
        // TODO 정합성 검토
        this.version = 0x00;
        this.payload = new byte[7];
        if (prevBlock == null) {
            // genesis block
            makeBlockHeader(author, null, 0, transactionList);
        } else {
            makeBlockHeader(author, prevBlock.getBlockByteHash(), prevBlock.getIndex() + 1,
                    transactionList);
        }
    }

    /*
     * Getter & Setter
     *
     * 객체를 최대한 캡슐화 하기 위해서 getter, setter 는 최소한으로 작성. 특히 setter 는 지양
     */
    public long getIndex() {
        return index;
    }

    public byte[] getBlockHash() {
        return HashUtils.sha256(SerializeUtils.serialize(this));
    }

    public byte[] getPrevBlockHash() {
        return prevBlockHash;
    }

    /*
     * Methods
     *
     */
    public void makeBlockHeader(Account author, BlockChain bc, BlockBody txs) {

        // 1. set pre_block_info(index, prevBlockHash)
        if (bc == null || bc.getPrevBlock() == null) {
            // Genesis Block
            this.index = 0;
            this.prevBlockHash = null;
        } else {
            log.debug(bc.getPrevBlock().getBlockHash());
            this.index = bc.getPrevBlock().getIndex() + 1;
            this.prevBlockHash = bc.getPrevBlock().getBlockByteHash();
        }

        // 2. set author
        this.author = author.getKey().getPub_key();

        // 3. set txs info (merkleRoot, dataSize)
        if (txs == null) {
            this.merkleRoot = null;
            this.dataSize = 0;
        } else {
            this.merkleRoot = txs.getMerkleRoot();
            this.dataSize = txs.getSize();
        }

        // 4. set signature (with timestamp)
        this.timestamp = TimeUtils.getCurrenttime();
        this.signature = null;
        this.signature = Signature.sign(author.getKey(), SerializeUtils.serialize(this));
    }

    private void makeBlockHeader(Account author, byte[] pre_block_hash, long index, BlockBody txs) {
        // 1. set pre_block_info(index, prevBlockHash)
        if (index == 0 && pre_block_hash == null) {
            this.index = 0;
            this.prevBlockHash = null;
        } else {
            this.index = index;
            this.prevBlockHash = pre_block_hash;
        }

        // 2. set author
        this.author = author.getKey().getPub_key();

        // 3. set txs info (merkleRoot, dataSize)
        this.merkleRoot = null;
        this.dataSize = 0;

        // 4. set signature (with timestamp)
        this.timestamp = TimeUtils.getCurrenttime();
        this.signature = null;
        this.signature = Signature.sign(author.getKey(), SerializeUtils.serialize(this));
    }
}
