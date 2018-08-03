package io.yggdrash.node.exception;

public class InvalidSignatureException extends SecurityException {
    public static final int code = -10001;
    public static final String msg = "Invalid signature";

    public InvalidSignatureException() {
        super(msg);
    }
}
