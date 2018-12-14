package io.yggdrash.core.contract;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.yggdrash.common.crypto.HashUtil;
import io.yggdrash.common.util.ByteUtil;
import org.spongycastle.util.encoders.Hex;

import java.math.BigDecimal;
import java.util.Map;


public class CoinContract extends BaseContract<JsonObject>
        implements CoinStandard {


    private final String totalSupplyKey = "TOTAL_SUPPLY";

    /**
     * @return Total amount of coin in existence
     */
    @Override
    public BigDecimal totalsupply(JsonObject param) {
        log.info("\ntotalsupply :: param => " + param);
        return getBalance(totalSupplyKey);
    }

    /**
     * Gets the balance of the specified address
     * param owner   The address to query the balance of
     *
     * @return A BigDecimal representing the amount owned by the passed address
     */
    @Override
    public BigDecimal balanceof(JsonObject param) {
        log.info("\nbalanceof :: param => " + param);

        String address = param.get("address").getAsString().toLowerCase();
        if (state.get(address) != null) {
            return getBalance(address);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Function to check the amount of coin that an owner allowed to a spender
     * param owner    The address which owns the funds.
     * param spender  The address which will spend the funds
     *
     * @return A BigDecimal specifying the amount of coin still available for the spender
     */
    @Override
    public BigDecimal allowance(JsonObject param) {
        log.info("\nallowance :: param => " + param);

        String owner = param.get("owner").getAsString().toLowerCase();
        String spender = param.get("spender").getAsString().toLowerCase();
        String approveKey = approveKey(owner, spender);

        if (state.get(owner) != null && state.get(approveKey) != null) {
            return getBalance(approveKey);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Transfer token for a specified address
     * param to      The address to transfer to
     * param amount  The amount to be transferred
     *
     * @return TransactionReceipt
     */
    @Override
    public TransactionReceipt transfer(JsonObject param) {
        log.info("\ntransfer :: param => " + param);

        String to = param.get("to").getAsString().toLowerCase();
        BigDecimal amount = param.get("amount").getAsBigDecimal();

        TransactionReceipt txReceipt = new TransactionReceipt();
        txReceipt.putLog("to", to);
        txReceipt.putLog("amount", String.valueOf(amount));

        if (getBalance(sender).compareTo(BigDecimal.ZERO) == 0) {
            log.info("\n[ERR] " + sender + " has no balance!");
            return txReceipt;
        }

        BigDecimal senderBallance = getBalance(sender);
        log.debug("sender : "+senderBallance);
        if (isTransferable(senderBallance, amount)) {
            senderBallance = senderBallance.subtract(amount);
            addBalanceTo(to, amount);
            putBalance(sender, senderBallance);
            txReceipt.setStatus(TransactionReceipt.SUCCESS);
            log.info("\n[Transferred] Transfer " + amount + " from " + sender + " to " + to);
            log.info("\nBalance of From (" + sender + ") : " + getBalance(sender)
                    + "\nBalance of To   (" + to + ") : " + getBalance(to));
        } else {
            log.info("\n[ERR] " + sender + " has no enough balance!");
        }
        return txReceipt;
    }

    /**
     * Approve the passed address to spend the specified amount of tokens on behalf of tx.sender
     * param spender  The address which will spend the funds
     * param amount   The amount of tokens to be spent
     *
     * @return TransactionReceipt
     */
    @Override
    public TransactionReceipt approve(JsonObject param) {
        log.info("\napprove :: param => " + param);

        String spender = param.get("spender").getAsString().toLowerCase();
        BigDecimal amount = param.get("amount").getAsBigDecimal();

        TransactionReceipt txReceipt = new TransactionReceipt();
        txReceipt.putLog("spender", spender);
        txReceipt.putLog("amount", String.valueOf(amount));

        if (getBalance(sender).compareTo(BigDecimal.ZERO) == 0) {
            log.info("\n[ERR] " + sender + " has no balance!");
            return txReceipt;
        }

        BigDecimal senderBalance = getBalance(sender);
        if (isTransferable(senderBalance, amount)) {
            String approveKey = approveKey(sender, spender);
            putBalance(approveKey, amount);
            log.debug("approve Key : " + approveKey);
            txReceipt.setStatus(TransactionReceipt.SUCCESS);
            log.info("\n[Approved] Approve " + spender + " to "
                    + getBalance(approveKey) + " from " + sender);
        } else {
            log.info("\n[ERR] " + sender + " has no enough balance!");
        }

        return txReceipt;
    }

    /**
     * Transfer tokens from one address to another
     * param from    The address which you want to send tokens from
     * param to      The address which you want to transfer to
     * param amount  The amount of tokens to be transferred
     *
     * @return TransactionReceipt
     */
    @Override
    public TransactionReceipt transferfrom(JsonObject param) {
        log.info("\ntransferfrom :: param => " + param);

        String from = param.get("from").getAsString().toLowerCase();
        String to = param.get("to").getAsString().toLowerCase();
        BigDecimal amount = param.get("amount").getAsBigDecimal();

        TransactionReceipt txReceipt = new TransactionReceipt();
        txReceipt.putLog("from", from);
        txReceipt.putLog("to", to);
        txReceipt.putLog("amount", String.valueOf(amount));

        String approveKey = approveKey(from, sender);
        log.debug("approve Key : " + approveKey);
        if (getBalance(approveKey).compareTo(BigDecimal.ZERO) == 0) {
            log.info("\n[ERR] " + from + " has no balance!");
            return txReceipt;
        }
        // check from amount
        BigDecimal fromValue = getBalance(from);
        BigDecimal approveValue = getBalance(approveKey);

        if (isTransferable(fromValue, amount) && isTransferable(approveValue, amount)) {
            fromValue = fromValue.subtract(amount);
            approveValue = approveValue.subtract(amount);

            addBalanceTo(to, amount);
            putBalance(from, fromValue);
            putBalance(approveKey, approveValue);
            txReceipt.setStatus(TransactionReceipt.SUCCESS);
            log.info("\n[Transferred] Transfer " + amount + " from " + from + " to " + to);
            log.debug("\nAllowed amount of Sender (" + sender + ") : "
                    + approveValue);
            log.debug("\nBalance of From (" + from + ") : " + fromValue
                    + "\nBalance of To   (" + to + ") : " + getBalance(to));
        } else {
            log.info("\n[ERR] " + from + " has no enough balance!");
        }
        return txReceipt;
    }

    /**
     * Pre-allocate yeed to addresses
     * param frontier The Frontier is the first live release of the Yggdrash network
     * param balance  The balance of frontier
     *
     * @return TransactionReceipt
     */
    public TransactionReceipt genesis(JsonObject param) {
        log.info("\ngenesis :: param => " + param);

        TransactionReceipt txReceipt = new TransactionReceipt();
        if (state.getStateSize() > 0L) {
            return txReceipt;
        }

        //totalSupply 는 alloc 의 balance 를 모두 더한 값으로 세팅
        BigDecimal totalSupply = BigDecimal.ZERO;
        JsonObject alloc = param.get("alloc").getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : alloc.entrySet()) {
            String frontier = entry.getKey();
            JsonObject value = entry.getValue().getAsJsonObject();
            BigDecimal balance = value.get("balance").getAsBigDecimal();
            totalSupply = totalSupply.add(balance);
            addBalanceTo(frontier, balance);

            putBalance(frontier, balance);

            txReceipt.putLog(frontier, balance);
            txReceipt.setStatus(TransactionReceipt.SUCCESS);
            log.info("\nAddress of Frontier : " + frontier
                    + "\nBalance of Frontier : " + getBalance(frontier));
        }
        // FIXME convert to Json or something
        try {
            putBalance(totalSupplyKey, totalSupply);
        } catch (Exception e) {
            e.printStackTrace();
        }
        txReceipt.putLog("TotalSupply", totalSupply);

        return txReceipt;
    }

    private void addBalanceTo(String to, BigDecimal amount) {
        BigDecimal balance = getBalance(to);
        if (balance == BigDecimal.ZERO) {
            putBalance(to, amount);
        }else {
            balance = balance.add(amount);
        }
        putBalance(to, balance);
    }

    private BigDecimal getBalance(String key) {
        JsonObject storeValue = state.get(key);
        String balance = null;
        if(storeValue.has("balance")) {
            balance = storeValue.get("balance").getAsString();
        }else{
            balance = "0";
        }
        return new BigDecimal(balance);
    }

    private void putBalance(String key, BigDecimal value) {
        JsonObject storeValue = new JsonObject();
        storeValue.addProperty("balance", value);
        state.put(key, storeValue);
    }


    private String approveKey(String sender, String spender) {
        byte[] approveKeyByteArray = ByteUtil.merge(sender.getBytes(), spender.getBytes());
        byte[] approveKey = HashUtil.sha3(approveKeyByteArray);
        return Hex.toHexString(approveKey);
    }

    private boolean isTransferable(BigDecimal targetBalance, BigDecimal ammount) {
        // same is  0, more is 1
        return targetBalance.subtract(ammount).compareTo(BigDecimal.ZERO) >= 0;
    }
}
