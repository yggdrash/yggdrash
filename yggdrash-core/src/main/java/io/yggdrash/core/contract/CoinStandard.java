package io.yggdrash.core.contract;

import com.google.gson.JsonObject;

import java.math.BigDecimal;

public interface CoinStandard {

    // Query
    BigDecimal totalsupply(JsonObject param);

    BigDecimal balanceof(JsonObject param);

    BigDecimal allowance(JsonObject param);


    // Transaction
    TransactionReceipt transfer(JsonObject param);

    TransactionReceipt approve(JsonObject param);

    TransactionReceipt transferfrom(JsonObject param);
}