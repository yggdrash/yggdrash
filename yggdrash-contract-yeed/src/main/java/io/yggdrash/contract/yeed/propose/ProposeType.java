package io.yggdrash.contract.yeed.propose;

public enum ProposeType {
    ETHER(1),
    ETHER_TOKEN(2);

    private final int value;

    ProposeType(int value) {
        this.value = value;
    }

    public int toValue() {
        return this.value;
    }
}
