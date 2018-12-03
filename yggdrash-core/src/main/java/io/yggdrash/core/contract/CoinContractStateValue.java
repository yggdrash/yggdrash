package io.yggdrash.core.contract;

import com.google.gson.JsonObject;
import io.yggdrash.common.util.Utils;
import io.yggdrash.core.store.VisibleStateValue;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class CoinContractStateValue implements VisibleStateValue {
    private final Map<String, BigDecimal> allowance = new HashMap<>();
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
        if (allowance.containsKey(allowedTo)) {
            return allowance.get(allowedTo);
        }
        return BigDecimal.ZERO;
    }

    public void addAllowedAmount(String allowedTo, BigDecimal amount) {
        BigDecimal allowedToValue = getAllowedAmount(allowedTo);
        updateAllowedAmount(allowedTo, allowedToValue.add(amount));
    }

    public void subtractAllowedAmount(String allowedTo, BigDecimal amount) {
        BigDecimal allowedToValue = getAllowedAmount(allowedTo);
        updateAllowedAmount(allowedTo, allowedToValue.subtract(amount));
    }

    private void updateAllowedAmount(String allowedTo, BigDecimal amount) {
        if (allowance.get(allowedTo) != null) {
            allowance.replace(allowedTo, amount);
        } else {
            allowance.put(allowedTo, amount);
        }
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
        return Utils.convertObjToJsonObject(this);
    }
}
