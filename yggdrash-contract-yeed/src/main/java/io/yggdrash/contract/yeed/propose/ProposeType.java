package io.yggdrash.contract.yeed.propose;

public enum ProposeType {
    YEED_TO_ETHER(1),
    YEED_TO_ETHER_TOKEN(2),
    ETHER_TO_YEED(3),
    ETHER_TOKEN_TO_YEED(4)
    ;

    private final int value;

    ProposeType(int value) {
        this.value = value;
    }

    public int toValue() {
        return this.value;
    }

    public static ProposeType fromInteger(int x) {
        switch(x) {
            case 1:
                return YEED_TO_ETHER;
            case 2:
                return YEED_TO_ETHER_TOKEN;
            case 3:
                return ETHER_TO_YEED;
            case 4:
                return ETHER_TOKEN_TO_YEED;
            default:
                return null;
        }
    }

}
