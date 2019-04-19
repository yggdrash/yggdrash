package io.yggdrash.core.exception;

public class InvalidSignatureException extends SecurityException {
    public static final int CODE = -10001;
    public static final String MSG = "Invalid signature";

    public InvalidSignatureException() {
        super(MSG);
    }

    public InvalidSignatureException(Throwable cause) {
        super(cause);
    }

}
