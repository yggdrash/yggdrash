package io.yggdrash.node.exception;

public class FailedOperationException extends IllegalStateException {
    public static final int code = -10004;

    public FailedOperationException(String msg) {
        super(msg + " not created");
    }
}
