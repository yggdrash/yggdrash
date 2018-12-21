package io.yggdrash.core.contract;

import com.google.gson.JsonObject;

import java.math.BigDecimal;

public interface CoinStandard {

    // Query
    BigDecimal totalsupply();

    BigDecimal balanceof(JsonObject params);

    BigDecimal allowance(JsonObject params);


    // Transaction
    TransactionReceipt transfer(JsonObject params);

    TransactionReceipt approve(JsonObject params);

    TransactionReceipt transferfrom(JsonObject params);
}