package io.yggdrash.core.type;

import io.yggdrash.core.type.enumeration.SerialEnum;

import java.io.Serializable;
import java.math.BigInteger;

public class Account implements Serializable {
    private static final long serialVersionUID = SerialEnum.ACCOUNT.toValue();

    private String addr;
    private BigInteger balance;

    public Account() {

    }

    public Account(String addr, BigInteger balance) {
        this.addr = addr;
        this.balance = balance;
    }

    public String getAddr() {
        return addr;
    }

    public void setAddr(String addr) {
        this.addr = addr;
    }

    public BigInteger getBalance() {
        return balance;
    }

    public void setBalance(BigInteger balance) {
        this.balance = balance;
    }
}
