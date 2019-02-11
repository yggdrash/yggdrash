package io.yggdrash.core.exception;

public class CreateStoreException extends RuntimeException {

    public CreateStoreException() {
        super();
    }

    public CreateStoreException(String s) {
        super(s);
    }

    public CreateStoreException(Throwable cause) {
        super(cause);
    }
}
