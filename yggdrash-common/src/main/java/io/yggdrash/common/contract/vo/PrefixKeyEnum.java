package io.yggdrash.common.contract.vo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PrefixKeyEnum {
    GENESIS("genesis"),
    GOVERNANCE("g-"),
    ACCOUNT("ac-"),
    APPROVE("ap-"),
    PROPOSE_VALIDATORS("pv-"),
    PROPOSE_CONTRACTS("pc-"),
    VALIDATORS("vl-"),
    PROPOSE_INTER_CHAIN("pi-")
    ;

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
            case "ap-":
                return APPROVE;
            case "rv-":
                return PROPOSE_VALIDATORS;
            case "pc-":
                return PROPOSE_CONTRACTS;
            case "vl-":
                return VALIDATORS;
            case "pi-":
                return PROPOSE_INTER_CHAIN;
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
