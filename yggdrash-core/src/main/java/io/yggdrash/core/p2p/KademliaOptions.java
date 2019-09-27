package io.yggdrash.core.p2p;

public class KademliaOptions {
    public static int CLOSEST_SIZE = 16;
    public static int BUCKET_SIZE = 5; // 16
    public static final int ALPHA = 3;
    public static int BINS = 160;
    public static int MAX_STEPS = 5; // 8
    public static int BROADCAST_SIZE = 30;

    public static final long REQ_TIMEOUT = 7000;
    public static final long BUCKET_REFRESH = 60;     //bucket refreshing interval in seconds
    public static final long DISCOVER_CYCLE = 30;       //dht cycle interval in seconds

    private KademliaOptions() {
        throw new IllegalStateException("Constant class");
    }
}
