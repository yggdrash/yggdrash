package io.yggdrash.validator.data.pbft;

public class Reply implements Message {

    private static final MessageType TYPE = MessageType.PREPARE;

    private final long viewNumber;
    private final long timestamp;
    private final byte[] clientId; // 20 bytes client address
    private final byte[] replicaId; // 20 bytes replica address
    private final byte[] result;
    private final byte[] signature; // client signature

    public Reply(long viewNumber, long timestamp, byte[] clientId, byte[] replicaId, byte[] result, byte[] signature) {
        this.viewNumber = viewNumber;
        this.timestamp = timestamp;
        this.clientId = clientId;
        this.replicaId = replicaId;
        this.result = result;
        this.signature = signature;
    }

    public MessageType getType() {
        return this.TYPE;
    }
}
