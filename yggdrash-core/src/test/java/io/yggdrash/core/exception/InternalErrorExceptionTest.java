package io.yggdrash.core.exception;

import org.junit.Test;

public class InternalErrorExceptionTest {

    @Test(expected = InternalErrorException.class)
    public void test1() {
        throw new InternalErrorException();
    }

    @Test(expected = InternalErrorException.class)
    public void test2() {
        throw new InternalErrorException("error");
    }
}