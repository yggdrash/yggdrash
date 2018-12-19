package io.yggdrash.core.net;

public class KademliaOptions {
    static int BUCKET_SIZE = 16;
    static final int ALPHA = 3;
    static final int BINS = 256;
    static final int MAX_STEPS = 8;

    public static final long REQ_TIMEOUT = 7000;
    public static final long BUCKET_REFRESH = 7200;     //bucket refreshing interval in millis
    public static final long DISCOVER_CYCLE = 30;       //discovery cycle interval in seconds
}
