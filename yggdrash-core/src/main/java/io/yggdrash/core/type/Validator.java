package io.yggdrash.core.type;

import io.yggdrash.core.type.enumeration.SerialEnum;

import java.io.Serializable;

public class Validator implements Serializable {
    private static final long serialVersionUID = SerialEnum.VALIDATOR.toValue();

    private String addr;
    private boolean isFreezing;

    public Validator() {

    }

    public Validator(String addr, boolean isFreezing) {
        this.addr = addr;
        this.isFreezing = isFreezing;
    }

    public String getAddr() {
        return addr;
    }

    public void setAddr(String addr) {
        this.addr = addr;
    }

    public boolean isFreezing() {
        return isFreezing;
    }

    public void setFreezing(boolean freezing) {
        isFreezing = freezing;
    }
}
