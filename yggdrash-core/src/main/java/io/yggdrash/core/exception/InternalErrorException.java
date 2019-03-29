package io.yggdrash.core.exception;

public class InternalErrorException extends RuntimeException {
    public static final int CODE = -10005;
    public static final String MSG = "Internal error";

    public InternalErrorException() {
        super(MSG);
    }

    public InternalErrorException(String message) {
        super(message);
    }
}
