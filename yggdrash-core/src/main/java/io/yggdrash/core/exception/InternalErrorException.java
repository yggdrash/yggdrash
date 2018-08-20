package io.yggdrash.core.exception;

public class InternalErrorException extends InternalError {
    public static final int code = -10005;
    public static final String msg = "Internal error";

    public InternalErrorException() {
        super(msg);
    }
}
