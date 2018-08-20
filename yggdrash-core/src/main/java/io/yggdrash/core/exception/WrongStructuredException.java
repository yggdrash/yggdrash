package io.yggdrash.core.exception;

public class WrongStructuredException extends IllegalArgumentException {
    public static final int code = -10002;
    public static final String msg = "The size of data is not appropriate";

    public WrongStructuredException() {
        super(msg);
    }
}
