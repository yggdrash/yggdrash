package io.yggdrash.validator.data.pbft;

public class Request<T> implements Message {

    private static final MessageType TYPE = MessageType.REQUEST;

    private final T block;
    private long timestamp;
    private final byte[] clientId; // 20 bytes validator address
    private byte[] signature;

    public Request(T block, long timestamp, byte[] clientId) {
        this.block = block;
        this.timestamp = timestamp;
        this.clientId = clientId;
    }

    public MessageType getType() {
        return this.TYPE;
    }
}
