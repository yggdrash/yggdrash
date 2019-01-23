package io.yggdrash.validator.data.pbft;

import io.yggdrash.core.blockchain.Block;

public class PrePrepare implements Message {

    private static final MessageType TYPE = MessageType.PRE_PREPARE;

    private final long viewNumber;
    private final long seqNumber;
    private final byte[] blockHash;
    private final byte[] signature;
    private final Block block;

    public PrePrepare(long viewNumber, long seqNumber, byte[] blockHash, byte[] signature, Block block) {
        this.viewNumber = viewNumber;
        this.seqNumber = seqNumber;
        this.blockHash = blockHash;
        this.signature = signature;
        this.block = block;
    }

    public MessageType getType() {
        return this.TYPE;
    }
}
