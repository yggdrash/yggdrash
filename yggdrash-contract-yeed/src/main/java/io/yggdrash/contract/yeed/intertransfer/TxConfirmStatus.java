package io.yggdrash.contract.yeed.intertransfer;

public enum TxConfirmStatus {
    VALIDATE_REQUIRE(1),
    DONE(2),
    VALID(3),
    NOT_EXIST(4)
    ;

    private final int value;

    TxConfirmStatus(int value) {
        this.value = value;
    }

    public int toValue() {
        return this.value;
    }

    public static TxConfirmStatus fromValue(int value) {
        for(TxConfirmStatus ps : values()) {
            if (ps.toValue() == value) {
                return ps;
            }
        }
        return null;
    }
}
