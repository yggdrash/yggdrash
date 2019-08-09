package io.yggdrash.core.blockchain.osgi;

import io.yggdrash.core.exception.errorcode.SystemError;

public class ExecutorException extends Exception {

    private final SystemError code;

    public ExecutorException(SystemError code) {
        super(code.toString());
        this.code = code;
    }

    public SystemError getCode() {
        return this.code;
    }

}
