package io.yggdrash.core.exception;

public class SerializeException extends RuntimeException {

    public SerializeException() {
        super();
    }

    public SerializeException(String s) {
        super(s);
    }

    public SerializeException(Throwable cause) {
        super(cause);
    }
}
