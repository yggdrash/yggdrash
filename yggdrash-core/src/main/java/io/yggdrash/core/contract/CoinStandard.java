package io.yggdrash.core.contract;

import com.google.gson.JsonArray;

import java.math.BigDecimal;

public interface CoinStandard {

    // Query
    BigDecimal totalsupply(JsonArray params);

    BigDecimal balanceof(JsonArray params);

    BigDecimal allowance(JsonArray params);


    // Transaction
    TransactionReceipt transfer(JsonArray params);

    TransactionReceipt approve(JsonArray params);

    TransactionReceipt transferfrom(JsonArray params);
}