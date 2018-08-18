package io.yggdrash.core.exception;

public class NonExistObjectException extends NullPointerException {
    public static final int code = -10000;

    public NonExistObjectException(String msg) {
        super(msg + " doesn't exist");
    }
}
