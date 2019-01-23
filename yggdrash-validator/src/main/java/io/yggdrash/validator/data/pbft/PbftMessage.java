package io.yggdrash.validator.data.pbft;

import io.yggdrash.core.blockchain.Block;

public class PbftMessage {
    private final MessageType type;
    private final long viewNumber;
    private final long seqNumber;
    private final byte[] blockHash;
    private final byte[] clientId;
    private final byte[] result;
    private final byte[] signature;
    private final Block block;

    public PbftMessage(MessageType type,
                       long viewNumber,
                       long seqNumber,
                       byte[] blockHash,
                       byte[] clientId,
                       byte[] result,
                       byte[] signature,
                       Block block) {
        this.type = type;
        this.viewNumber = viewNumber;
        this.seqNumber = seqNumber;
        this.blockHash = blockHash;
        this.clientId = clientId;
        this.result = result;
        this.signature = signature;
        this.block = block;
    }

}
