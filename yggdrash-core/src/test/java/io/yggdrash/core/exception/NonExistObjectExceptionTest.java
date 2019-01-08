package io.yggdrash.core.exception;

import org.junit.Test;

public class NonExistObjectExceptionTest {

    @Test(expected = NonExistObjectException.class)
    public void test() {
        throw new NonExistObjectException("branch");
    }
}