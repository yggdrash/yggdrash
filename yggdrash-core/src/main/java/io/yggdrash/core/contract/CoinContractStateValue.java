package io.yggdrash.core.contract;

import com.google.gson.JsonObject;
import io.yggdrash.core.store.VisibleStateValue;

import java.math.BigDecimal;

public class CoinContractStateValue implements VisibleStateValue {
    private BigDecimal balance = BigDecimal.ZERO;

    public BigDecimal getBalance() {
        return balance;
    }

    public void addBalance(BigDecimal amount) {
        this.balance = balance.add(amount);
    }

    public void subtractBalance(BigDecimal amount) {
        this.balance = balance.subtract(amount);
    }

    public boolean isTransferable(BigDecimal amount) {
        return balance.subtract(amount).compareTo(BigDecimal.ZERO) >= 0;
    }

    @Override
    public JsonObject getValue() {
        JsonObject value = new JsonObject();
        value.addProperty("balance", balance);

        JsonObject json = new JsonObject();
        json.add("value", value);
        return json;
    }
}
