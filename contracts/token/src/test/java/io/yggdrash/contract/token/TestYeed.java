package io.yggdrash.contract.token;

import com.google.gson.JsonObject;
import io.yggdrash.common.contract.vo.PrefixKeyEnum;
import io.yggdrash.common.crypto.HashUtil;
import io.yggdrash.common.crypto.HexUtil;
import io.yggdrash.common.utils.ByteUtil;
import io.yggdrash.contract.core.ExecuteStatus;
import io.yggdrash.contract.core.Receipt;
import io.yggdrash.contract.core.annotation.ContractChannelMethod;
import io.yggdrash.contract.core.annotation.ContractQuery;
import io.yggdrash.contract.core.annotation.InvokeTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public class TestYeed {
    private static final Logger log = LoggerFactory.getLogger(TestYeed.class);

    Map<String, BigInteger> amount = new HashMap<>();
    private Receipt txReceipt;

    public TestYeed() {
        amount.put("c91e9d46dd4b7584f0b6348ee18277c10fd7cb94", BigInteger.TEN.pow(40));
        amount.put("1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e", BigInteger.TEN.pow(40));
        amount.put("101167aaf090581b91c08480f6e559acdd9a3ddd", BigInteger.TEN.pow(40));
        amount.put("5244d8163ea6fdd62aa08ae878b084faa0b013be", BigInteger.TEN.pow(40));
        amount.put("1111111111111111111111111111111111111111",
                BigInteger.valueOf(1234).multiply(BigInteger.TEN.pow(18)));
    }

    public void setTxReceipt(Receipt txReceipt) {
        this.txReceipt = txReceipt;
    }

    @ContractQuery
    public BigInteger totalSupply() {
        return null;
    }

    @ContractQuery
    public BigInteger balanceOf(JsonObject params) {
        String addr = params.get("address").getAsString();
        return getBalance(addr);
    }

    @ContractQuery
    public BigInteger allowance(JsonObject params) {
        String approveKey = approveKey(params.get("owner").getAsString(), params.get("spender").getAsString());
        return getBalance(approveKey);
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
                txReceipt.setStatus(ExecuteStatus.SUCCESS);
            } else {
                txReceipt.setStatus(ExecuteStatus.FALSE);
                txReceipt.addLog("has no balance");
            }
        } else {
            txReceipt.setStatus(ExecuteStatus.FALSE);
            txReceipt.addLog("has no balance");
        }

        return txReceipt.getStatus() == ExecuteStatus.SUCCESS;
    }


    @InvokeTransaction
    public boolean approve(JsonObject params) {
        String sender = txReceipt.getIssuer();
        BigInteger amount = params.get("amount").getAsBigInteger();
        BigInteger fee = params.has("fee") ? params.get("fee").getAsBigInteger() : BigInteger.ZERO;
        String spender = params.get("spender").getAsString().toLowerCase();

        if (getBalance(sender).compareTo(BigInteger.ZERO) <= 0) {
            txReceipt.addLog(String.format("%s has no balance.", sender));
            txReceipt.setStatus(ExecuteStatus.ERROR);
            return false;
        }

        if (fee.compareTo(BigInteger.ZERO) != 0) {
            transfer(sender, sender, BigInteger.ZERO, fee);
        }

        BigInteger senderBalance = getBalance(sender);

        if (isTransferable(senderBalance, amount)) {
            String approveKey = approveKey(sender, spender);
            this.amount.put(approveKey, amount);
            txReceipt.setStatus(ExecuteStatus.SUCCESS);
        } else {
            txReceipt.setStatus(ExecuteStatus.ERROR);
            txReceipt.addLog("Insufficient funds");
            return false;
        }

        return true;
    }

    @InvokeTransaction
    public boolean transferFrom(JsonObject params) {
        String from = params.get("from").getAsString().toLowerCase();
        String to = params.get("to").getAsString().toLowerCase();
        BigInteger amount = params.get("amount").getAsBigInteger();

        return transferFrom(from, to, amount, BigInteger.ZERO);
    }

    private boolean transferFrom(String from, String to, BigInteger amount, BigInteger fee) {
        String sender = txReceipt.getIssuer();
        String approveKey = approveKey(from, sender);
        BigInteger approveAmount = getBalance(approveKey);

        // Check approved amount
        if (approveAmount.compareTo(BigInteger.ZERO) == 0) {
            txReceipt.setStatus(ExecuteStatus.ERROR);
            txReceipt.addLog(String.format("Insufficient approved funds in the account : %s", from));
            return false;
        }

        BigInteger senderAmount = getBalance(sender);

        // Check fee
        if (senderAmount.compareTo(fee) < 0) {
            txReceipt.setStatus(ExecuteStatus.ERROR);
            txReceipt.addLog(String.format("Insufficient funds in the account : %s", from));
            return false;
        }

        BigInteger fromAmount = getBalance(from);
        BigInteger amountFee = amount.add(fee);

        if (isTransferable(fromAmount, amountFee) && isTransferable(approveAmount, amountFee)) {
            boolean isTransfer = transfer(from, to, amount, fee);
            if (isTransfer) {
                approveAmount = approveAmount.subtract(amountFee);
                this.amount.put(approveKey, approveAmount);
                // TODO check fee governance
                //senderAmount = senderAmount.subtract(fee);
                //this.amount.put(sender, senderAmount);
            }
            txReceipt.setStatus(isTransfer ? ExecuteStatus.SUCCESS : ExecuteStatus.FALSE);
            txReceipt.addLog("Transfer from failed");
        } else {
            txReceipt.setStatus(ExecuteStatus.ERROR);
            txReceipt.addLog("Insufficient funds");
            return false;
        }
        return true;
    }

    @ContractChannelMethod
    public boolean transferFromChannel(JsonObject params) {
        log.info("transferFromChannel");
        String contractName = "STEM";
        String contractAccount = String.format("%s%s", PrefixKeyEnum.CONTRACT_ACCOUNT, contractName);
        String from = params.get("from").getAsString();
        String to = params.get("to").getAsString();
        BigInteger amount = params.get("amount").getAsBigInteger();
        BigInteger serviceFee = params.has("serviceFee") ? params.get("serviceFee").getAsBigInteger() : BigInteger.ZERO;

        if (to.equalsIgnoreCase(contractName)) { // deposit
            return transferFrom(from, contractAccount, amount, serviceFee);
        } else if (from.equalsIgnoreCase(contractName)) { // withdraw
            return transferFrom(contractAccount, to, amount, serviceFee);
        }

        return false;
    }

    private String approveKey(String from, String sender) {
        String approveHexKey = HexUtil.toHexString(HashUtil.sha3(ByteUtil.merge(from.getBytes(), sender.getBytes())));
        return String.format("%s%s", PrefixKeyEnum.APPROVE.toValue(), approveHexKey);
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
    public boolean isTransferable(JsonObject params) {
        String address = params.get("address").getAsString();
        BigInteger fee = params.get("amount").getAsBigInteger();

        return isTransferable(getBalance(address), fee);
    }

    private boolean isTransferable(BigInteger targetBalance, BigInteger amount) {
        // less is -1, same is  0, more is 1
        return targetBalance.subtract(amount).compareTo(BigInteger.ZERO) >= 0;
    }

    @ContractChannelMethod
    public BigInteger getContractBalanceOf(JsonObject params) {
        String contractAccount
                = String.format("%s%s", PrefixKeyEnum.CONTRACT_ACCOUNT, params.get("contractName").getAsString());
        return getBalance(contractAccount);
    }

    @ContractChannelMethod
    public boolean transferChannel(JsonObject params) { // call other contract to transfer

        String contractName = "TOKEN"; // contract Name base
        BigInteger amount = params.get("amount").getAsBigInteger();
        String fromAccount = params.get("from").getAsString();
        String toAccount = params.get("to").getAsString();
        String contractAccount = String.format("%s%s", PrefixKeyEnum.CONTRACT_ACCOUNT, contractName);

        // TODO FIX
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

    private BigInteger getBalance(String address) {
        return amount.getOrDefault(address, BigInteger.ZERO);
    }
}
