package io.yggdrash.core.contract;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class CoinStandardStateTable {
    private BigDecimal myBalance;
    private Map<String, BigDecimal> allowance;

    public CoinStandardStateTable() {
        this.myBalance = BigDecimal.ZERO;
        this.allowance = new HashMap<>();
    }

    public BigDecimal getMyBalance() {
        return myBalance;
    }

    public void setMyBalance(BigDecimal myBalance) {
        this.myBalance = myBalance;
    }

    public Map<String, BigDecimal> getAllowance() {
        return allowance;
    }

    public BigDecimal getAllowedAmount(String allowedTo) {
        if (allowance.containsKey(allowedTo)) {
            return allowance.get(allowedTo);
        }
        return BigDecimal.ZERO;
    }

    public void setAllowance(String allowedTo, BigDecimal amount) {
        if (allowance.get(allowedTo) != null) {
            allowance.replace(allowedTo, amount);
        } else {
            allowance.put(allowedTo, amount);
        }
    }

    public void setAllowance(Map<String, BigDecimal> allowance) {
        this.allowance = allowance;
    }
}
