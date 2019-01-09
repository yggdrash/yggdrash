package io.yggdrash.core.exception;

import org.junit.Test;

public class WrongStructuredExceptionTest {

    @Test(expected = WrongStructuredException.class)
    public void test() {
        throw new WrongStructuredException();
    }
}