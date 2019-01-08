package io.yggdrash.core.exception;

import org.junit.Test;

public class InvalidSignatureExceptionTest {

    @Test(expected = InvalidSignatureException.class)
    public void test() {
        throw new InvalidSignatureException();
    }
}