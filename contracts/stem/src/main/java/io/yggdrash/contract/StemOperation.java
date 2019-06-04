package io.yggdrash.contract;

public enum StemOperation {
    ADD_VALIDATOR("1"),
    REMOVE_VALIDATOR("2"),
    REPLACE_VALIDATOR("3"),
    UPDATE_VALIDATOR_SET("4")
    ;

    private final String flag;

    StemOperation(String flag) {
        this.flag = flag;
    }

    public String toValue() {
        return this.flag;
    }

    public static StemOperation fromValue(String flag) {
        StemOperation[] values = StemOperation.values();
        for (StemOperation value : values) {
            if (value.flag.equals(flag)) {
                return value;
            }
        }
        return null;

    }
}
