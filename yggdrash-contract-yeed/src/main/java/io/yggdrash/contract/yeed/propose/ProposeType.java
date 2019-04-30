package io.yggdrash.contract.yeed.propose;

public enum ProposeType {
    YEED_TO_ETHER(1),
    ETHER_TO_YEED(2),
    YEED_TO_ETHER_TOKEN(3),
    ETHER_TOKEN_TO_YEED(4),
    YEED_TO_YGGDRASH_CHAIN(5),
    YGGDRASH_CHAIN_TO_YEED(6)
    ;

    private final int value;

    ProposeType(int value) {
        this.value = value;
    }

    public int toValue() {
        return this.value;
    }

    public static ProposeType fromValue(int x) {
        switch (x) {
            case 1:
                return YEED_TO_ETHER;
            case 2:
                return ETHER_TO_YEED;
            case 3:
                return YEED_TO_ETHER_TOKEN;
            case 4:
                return ETHER_TOKEN_TO_YEED;
            case 5:
                return YEED_TO_YGGDRASH_CHAIN;
            case 6:
                return YGGDRASH_CHAIN_TO_YEED;
            default:
                return null;
        }
    }

}
