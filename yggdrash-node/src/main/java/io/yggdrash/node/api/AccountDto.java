package io.yggdrash.node.api;

import io.yggdrash.core.account.Account;
import org.apache.commons.codec.binary.Hex;

public class AccountDto {
    private String address;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public static AccountDto createBy(Account account) {
        AccountDto dto = new AccountDto();
        dto.setAddress(Hex.encodeHexString(account.getAddress()));
        return dto;
    }
}
