package io.yggdrash.core.exception;

public class NotValidateException extends RuntimeException {

    public NotValidateException() {
        super();
    }

    public NotValidateException(String s) {
        super(s);
    }

    public NotValidateException(Throwable cause) {
        super(cause);
    }
}
