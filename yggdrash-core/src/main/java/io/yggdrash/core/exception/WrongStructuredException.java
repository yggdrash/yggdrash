package io.yggdrash.core.exception;

public class WrongStructuredException extends IllegalArgumentException {
    public static final int CODE = -10002;
    public static final String MSG = "The size of data is not appropriate";

    WrongStructuredException() {
        super(MSG);
    }
}
