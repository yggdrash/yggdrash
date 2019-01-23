package io.yggdrash.validator.data.pbft;

public class Prepare implements Message {

    private static final MessageType TYPE = MessageType.PREPARE;

    private final long viewNumber;
    private final long seqNumber;
    private final byte[] blockHash;
    private final byte[] signature;

    public Prepare(long viewNumber, long seqNumber, byte[] blockHash, byte[] signature) {
        this.viewNumber = viewNumber;
        this.seqNumber = seqNumber;
        this.blockHash = blockHash;
        this.signature = signature;
    }

    public MessageType getType() {
        return this.TYPE;
    }
}
