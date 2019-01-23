package io.yggdrash.validator.data.pbft;

public class PrePrepare implements Message {

    private static final MessageType TYPE = MessageType.PRE_PREPARE;

    private final long viewNumber;
    private final long seqNumber;
    private final byte[] hashOfRequest;
    private final byte[] signature;
    private final Request request;

    public PrePrepare(long viewNumber, long seqNumber, byte[] hashOfRequest, byte[] signature, Request request) {
        this.viewNumber = viewNumber;
        this.seqNumber = seqNumber;
        this.hashOfRequest = hashOfRequest;
        this.signature = signature;
        this.request = request;
    }

    public MessageType getType() {
        return this.TYPE;
    }
}
