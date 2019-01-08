package io.yggdrash.core.exception;

import org.junit.Test;

public class NotValidateExceptionTest {

    @Test(expected = NotValidateException.class)
    public void test1() {
        throw new NotValidateException();
    }

    @Test(expected = NotValidateException.class)
    public void test2() {
        throw new NotValidateException("notValid");
    }

    @Test(expected = NotValidateException.class)
    public void test3() {
        throw new NotValidateException(new RuntimeException());
    }

}