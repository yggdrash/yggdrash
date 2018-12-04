package io.yggdrash.core.contract;

import com.google.gson.JsonObject;
import io.yggdrash.core.store.VisibleStateValue;

import java.math.BigDecimal;

public class CoinContractStateValue implements VisibleStateValue {
    private final JsonObject allowance = new JsonObject();
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

    public BigDecimal getAllowedAmount(String allowedTo) {
        if (allowance.has(allowedTo)) {
            return allowance.get(allowedTo).getAsBigDecimal();
        }
        return BigDecimal.ZERO;
    }

    public void addAllowedAmount(String allowedTo, BigDecimal amount) {
        BigDecimal allowedToValue = getAllowedAmount(allowedTo);
        allowance.addProperty(allowedTo, allowedToValue.add(amount));
    }

    public void subtractAllowedAmount(String allowedTo, BigDecimal amount) {
        BigDecimal allowedToValue = getAllowedAmount(allowedTo);
        allowance.addProperty(allowedTo, allowedToValue.subtract(amount));
    }

    public boolean isEnoughAllowedAmount(String allowedTo, BigDecimal amount) {
        BigDecimal allowedToValue = getAllowedAmount(allowedTo);
        return allowedToValue.subtract(amount).compareTo(BigDecimal.ZERO) >= 0;
    }

    public boolean isTransferable(BigDecimal amount) {
        return balance.subtract(amount).compareTo(BigDecimal.ZERO) >= 0;
    }

    @Override
    public JsonObject getValue() {
        JsonObject value = new JsonObject();
        value.addProperty("balance", balance);
        value.add("allowance", allowance);

        JsonObject json = new JsonObject();
        json.add("value", value);
        return json;
    }
}
