package io.yggdrash.core.type.enumeration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SerialEnum {
    ACCOUNT(1),
    VALIDATOR(2),
    TRANSACTION_BONDING(3),
    TRANSACTION_DELEGATING(4);

    private int value;

    SerialEnum(int value) {
        this.value = value;
    }

    @JsonCreator
    public static SerialEnum fromValue(int value) {
        switch (value) {
            case 1:
                return ACCOUNT;
            case 2:
                return VALIDATOR;
            case 3:
                return TRANSACTION_BONDING;
            case 4:
                return TRANSACTION_DELEGATING;
        }
        return null;
    }

    @JsonValue
    public int toValue() {
        return this.value;
    }
}
