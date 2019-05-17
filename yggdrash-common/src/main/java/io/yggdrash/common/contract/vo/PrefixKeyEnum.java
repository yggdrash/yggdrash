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
    PROPOSE_INTER_CHAIN("pi-"),
    PROPOSE_INTER_CHAIN_STATUS("pis-"),
    TRANSACTION_CONFIRM("tc-"),
    STEM_BRANCH("sb-"),
    STEM_META("sm-"),
    STEM_BRANCH_VALIDATOR("sbv-"),

    ;

    private final String value;

    PrefixKeyEnum(String value) {
        this.value = value;
    }

    @JsonCreator
    public static PrefixKeyEnum fromValue(String value) {
        return valueOf(value);
    }

    @JsonValue
    public String toValue() {
        return this.value;
    }

    public static String getAccountKey(String accountAddr) {
        return String.format("%s%s", PrefixKeyEnum.ACCOUNT.toValue(), accountAddr);
    }
}
