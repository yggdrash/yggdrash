package io.yggdrash.core.exception;

import io.yggdrash.common.exception.FailedOperationException;
import org.junit.Test;

public class FailedOperationExceptionTest {

    @Test(expected = FailedOperationException.class)
    public void test1() {
        throw new FailedOperationException(new RuntimeException());
    }

    @Test(expected = FailedOperationException.class)
    public void test2() {
        throw new FailedOperationException("error");
    }
}