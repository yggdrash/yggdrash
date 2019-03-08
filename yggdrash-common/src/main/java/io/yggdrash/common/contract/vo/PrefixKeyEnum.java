package io.yggdrash.common.contract.vo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PrefixKeyEnum {
    GENESIS("genesis"),
    GOVERNANCE("g-"),
    ACCOUNT("ac-"),
    PROPOSE_VALIDATORS("pv-"),
    VALIDATORS("vl-");

    private final String value;

    PrefixKeyEnum(String value) {
        this.value = value;
    }

    @JsonCreator
    public static PrefixKeyEnum fromValue(String value) {
        switch (value) {
            case "genesis":
                return GENESIS;
            case "g-":
                return GOVERNANCE;
            case "ac-":
                return ACCOUNT;
            case "rv-":
                return PROPOSE_VALIDATORS;
            case "vl-":
                return VALIDATORS;
            default:
                return null;
        }
    }

    @JsonValue
    public String toValue() {
        return this.value;
    }

    public static String getAccountKey(String accountAddr) {
        return String.format("%s%s", PrefixKeyEnum.ACCOUNT.toValue(), accountAddr);
    }
}
