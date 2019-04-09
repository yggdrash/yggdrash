package io.yggdrash.contract.yeed.propose;

public enum ProposeStatus {
    ISSUED(1),
    PROCESSING(2),
    DONE(3),
    CONGLUTINATION(4),
    RETURN(5);

    private final int value;

    ProposeStatus(int value) {
        this.value = value;
    }

    public int toValue() {
        return this.value;
    }
}
