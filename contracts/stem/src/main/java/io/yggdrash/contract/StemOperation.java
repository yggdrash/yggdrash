package io.yggdrash.contract;

import java.util.Arrays;

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
        return Arrays.stream(StemOperation.values())
                .filter(value -> value.flag.equals(flag))
                .findFirst()
                .orElse(null);
    }
}
