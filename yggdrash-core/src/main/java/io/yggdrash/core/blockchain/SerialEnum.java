package io.yggdrash.core.blockchain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SerialEnum {
    VALIDATOR(1),
    VALIDATOR_SET(2),
    PROPOSE_VALIDATOR_SET(3),
    TX_VALIDATOR_PROPOSE(4),
    TX_VALIDATOR_VOTE(5);

    private int value;

    SerialEnum(int value) {
        this.value = value;
    }

    @JsonCreator
    public static SerialEnum fromValue(int value) {
        switch (value) {
            case 1:
                return VALIDATOR;
            case 2:
                return VALIDATOR_SET;
            case 3:
                return PROPOSE_VALIDATOR_SET;
            case 4:
                return TX_VALIDATOR_PROPOSE;
            case 5:
                return TX_VALIDATOR_VOTE;
            default:
                return null;
        }
    }

    @JsonValue
    public int toValue() {
        return this.value;
    }
}
