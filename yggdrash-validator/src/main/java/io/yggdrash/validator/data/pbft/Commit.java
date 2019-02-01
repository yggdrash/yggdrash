package io.yggdrash.validator.data.pbft;

public class Commit implements Message {

    private static final MessageType TYPE = MessageType.COMMIT;

    private final long viewNumber;
    private final long seqNumber;
    private final byte[] blockHash;
    private final byte[] signature;

    public Commit(long viewNumber, long seqNumber, byte[] blockHash, byte[] signature) {
        this.viewNumber = viewNumber;
        this.seqNumber = seqNumber;
        this.blockHash = blockHash;
        this.signature = signature;
    }

    public MessageType getType() {
        return this.TYPE;
    }
}
