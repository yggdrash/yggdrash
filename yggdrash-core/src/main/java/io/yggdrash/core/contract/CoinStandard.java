package io.yggdrash.core.contract;

public interface CoinStandard {

    float totalSupply();

    float balanceOf(String owner);

    float allowance(String owner, String spender);

    void transfer(String to, String amount);

    void approve(String spender, float amount);

    void transferFrom(String from, String to, float amount);
}