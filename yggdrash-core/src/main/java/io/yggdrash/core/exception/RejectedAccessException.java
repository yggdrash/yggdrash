package io.yggdrash.core.exception;

public class RejectedAccessException extends RuntimeException {
    public static final int CODE = -10003;
    public static final String MSG = "Rejected";

    public RejectedAccessException() {
        super(MSG);
    }
}
