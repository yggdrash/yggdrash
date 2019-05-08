package io.yggdrash.core.contract;

import com.google.gson.JsonObject;
import io.yggdrash.common.contract.standard.CoinStandard;
import io.yggdrash.contract.core.ExecuteStatus;
import io.yggdrash.contract.core.TransactionReceipt;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public class TestYeed implements CoinStandard {

    Map<String, BigInteger> amount = new HashMap<>();
    private TransactionReceipt txReceipt;

    public TestYeed() {
        amount.put("c91e9d46dd4b7584f0b6348ee18277c10fd7cb94", new BigInteger("100000000000"));
        amount.put("1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e", new BigInteger("100000000000"));
    }

    public void setTxReceipt(TransactionReceipt txReceipt) {
        this.txReceipt = txReceipt;
    }

    @Override
    public BigInteger totalSupply() {
        return null;
    }

    @Override
    public BigInteger balanceOf(JsonObject params) {
        return amount.get(params.get("address").getAsString());
    }

    @Override
    public BigInteger allowance(JsonObject params) {
        return null;
    }

    @Override
    public TransactionReceipt transfer(JsonObject params) {
        String from = txReceipt.getIssuer();
        String to = params.get("to").getAsString();
        BigInteger transferAmount = params.get("amount").getAsBigInteger();

        if (amount.get(from) != null) {
            BigInteger fromAmount = amount.get(from);
            BigInteger toAmount = BigInteger.ZERO;
            if (amount.containsKey(to)) {
                toAmount = amount.get(to);
            }

            fromAmount = fromAmount.subtract(transferAmount);
            toAmount = toAmount.add(transferAmount);

            if (fromAmount.compareTo(BigInteger.ZERO) >= 0) {
                amount.put(from, fromAmount);
                amount.put(to, toAmount);
            } else {
                txReceipt.setStatus(ExecuteStatus.FALSE);
                txReceipt.addLog("has no balance");
            }
        } else {
            txReceipt.setStatus(ExecuteStatus.FALSE);
            txReceipt.addLog("has no balance");
        }

        return txReceipt;
    }

    @Override
    public TransactionReceipt approve(JsonObject params) {
        return null;
    }

    @Override
    public TransactionReceipt transferFrom(JsonObject params) {
        return null;
    }
}
