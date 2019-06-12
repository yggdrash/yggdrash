package io.yggdrash.contract;

import com.google.gson.JsonObject;
import io.yggdrash.common.contract.standard.CoinStandard;
import io.yggdrash.common.contract.vo.PrefixKeyEnum;
import io.yggdrash.contract.core.ExecuteStatus;
import io.yggdrash.contract.core.TransactionReceipt;
import io.yggdrash.contract.core.annotation.ContractChannelMethod;
import io.yggdrash.contract.core.annotation.ContractQuery;
import io.yggdrash.contract.core.annotation.InvokeTransaction;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public class TestYeed  {

    Map<String, BigInteger> amount = new HashMap<>();
    private TransactionReceipt txReceipt;

    public TestYeed() {
        amount.put("c91e9d46dd4b7584f0b6348ee18277c10fd7cb94", new BigInteger("100000000000"));
        amount.put("1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e", new BigInteger("100000000000"));
        amount.put("101167aaf090581b91c08480f6e559acdd9a3ddd", new BigInteger("100000000000000"));
    }

    public void setTxReceipt(TransactionReceipt txReceipt) {
        this.txReceipt = txReceipt;
    }

    @ContractQuery
    public BigInteger totalSupply() {
        return null;
    }

    @ContractQuery
    public BigInteger balanceOf(JsonObject params) {
        return amount.get(params.get("address").getAsString());
    }

    @ContractQuery
    public BigInteger allowance(JsonObject params) {
        return null;
    }

    @InvokeTransaction
    public boolean transfer(JsonObject params) {
        String from = txReceipt.getIssuer();
        String to = params.get("to").getAsString();
        BigInteger transferAmount = params.get("amount").getAsBigInteger();

        return transfer(from, to, transferAmount, BigInteger.ZERO);
    }


    private boolean transfer(String from, String to, BigInteger transferAmount, BigInteger fee) {

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
            txReceipt.setStatus(ExecuteStatus.SUCCESS);
        } else {
            txReceipt.setStatus(ExecuteStatus.FALSE);
            txReceipt.addLog("has no balance");
        }

        return txReceipt.getStatus() == ExecuteStatus.SUCCESS;
    }


    @InvokeTransaction
    public TransactionReceipt approve(JsonObject params) {
        return null;
    }

    @InvokeTransaction
    public TransactionReceipt transferFrom(JsonObject params) {
        return null;
    }


    @ContractChannelMethod
    public boolean contractWithdraw(JsonObject params) {
        // TODO contract withdraw by name (not version)
        String contractVersion = this.txReceipt.getContractVersion();

        // add contract version
        String from = contractVersion;
        String to = params.get("to").getAsString();
        BigInteger amount = params.get("amount").getAsBigInteger();

        // check account
        BigInteger contractBalance = this.amount.get(from);

        if (contractBalance.compareTo(amount) > 0) {
            BigInteger toBalance = this.amount.get(to);
            toBalance = toBalance.add(amount);
            contractBalance = contractBalance.subtract(amount);

            this.amount.put(contractVersion, contractBalance);
            this.amount.put(from, toBalance);
            return true;
        } else {
            return false;
        }
    }

    @ContractChannelMethod
    public boolean transferChannel(JsonObject params) {
        // call other contract to transfer

        // contract Name base
        String contractName = "STEM";

        // deposit or withdraw
        String fromAccount = params.get("from").getAsString();
        String toAccount = params.get("to").getAsString();
        BigInteger amount = params.get("amount").getAsBigInteger();
        String contractAccount = String.format("%s%s", PrefixKeyEnum.CONTRACT_ACCOUNT, contractName);

        if (toAccount.equalsIgnoreCase(contractName)) { // deposit
            // check from is issuer
            if (fromAccount.equalsIgnoreCase(this.txReceipt.getIssuer())) {
                return transfer(fromAccount, contractAccount, amount, BigInteger.ZERO);
            } else {
                return false;
            }

        } else if (fromAccount.equalsIgnoreCase(contractName)) { // withdraw
            return transfer(contractAccount, toAccount, amount, BigInteger.ZERO);
        }
        // if not contract call deposit or withdraw
        return false;
    }

}
