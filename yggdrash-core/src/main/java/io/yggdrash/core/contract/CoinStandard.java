package io.yggdrash.core.contract;

import com.google.gson.JsonObject;

import java.math.BigInteger;

public interface CoinStandard {

    // Query
    BigInteger totalSupply();

    BigInteger balanceOf(JsonObject params);

    BigInteger allowance(JsonObject params);


    // Transaction
    TransactionReceipt transfer(JsonObject params);

    TransactionReceipt approve(JsonObject params);

    TransactionReceipt transferFrom(JsonObject params);
}