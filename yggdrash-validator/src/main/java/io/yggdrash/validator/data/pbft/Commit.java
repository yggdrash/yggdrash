package io.yggdrash.validator.data.pbft;

public class Commit implements Message {

    private static final MessageType TYPE = MessageType.PREPARE;

    private final long viewNumber;
    private final long seqNumber;
    private final byte[] hashOfRequest;
    private final byte[] replicaId; // 20 bytes replica address
    private final byte[] signature;

    public Commit(long viewNumber, long seqNumber, byte[] hashOfRequest,
                  byte[] replicaId, byte[] signature) {
        this.viewNumber = viewNumber;
        this.seqNumber = seqNumber;
        this.hashOfRequest = hashOfRequest;
        this.replicaId = replicaId;
        this.signature = signature;
    }

    public MessageType getType() {
        return this.TYPE;
    }
}
