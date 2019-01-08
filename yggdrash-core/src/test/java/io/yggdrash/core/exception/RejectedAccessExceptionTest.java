package io.yggdrash.core.exception;

import org.junit.Test;

public class RejectedAccessExceptionTest {

    @Test(expected = RejectedAccessException.class)
    public void test() {
        throw new RejectedAccessException();
    }
}