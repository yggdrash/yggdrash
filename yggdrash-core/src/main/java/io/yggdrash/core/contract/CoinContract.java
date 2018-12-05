package io.yggdrash.core.contract;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.math.BigDecimal;
import java.util.Map;

public class CoinContract extends BaseContract<CoinContractStateValue>
        implements CoinStandard {


    private final String totalSupplyKey = "TOTAL_SUPPLY";

    /**
     * @return Total amount of coin in existence
     */
    @Override
    public BigDecimal totalsupply(JsonArray params) {
        log.info("\ntotalsupply :: params => " + params);
        return state.get(totalSupplyKey).getBalance();
    }

    /**
     * Gets the balance of the specified address
     * param owner   The address to query the balance of
     *
     * @return A BigDecimal representing the amount owned by the passed address
     */
    @Override
    public BigDecimal balanceof(JsonArray params) {
        log.info("\nbalanceof :: params => " + params);

        String address = params.get(0).getAsJsonObject().get("address").getAsString().toLowerCase();
        if (state.get(address) != null) {
            CoinContractStateValue value = state.get(address);
            return value.getBalance();
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
    public BigDecimal allowance(JsonArray params) {
        log.info("\nallowance :: params => " + params);

        String owner = params.get(0).getAsJsonObject().get("owner").getAsString().toLowerCase();
        String spender = params.get(0).getAsJsonObject().get("spender").getAsString().toLowerCase();

        if (state.get(owner) != null) {
            return state.get(owner).getAllowedAmount(spender);
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
    public TransactionReceipt transfer(JsonArray params) {
        log.info("\ntransfer :: params => " + params);

        String to = params.get(0).getAsJsonObject().get("to").getAsString().toLowerCase();
        BigDecimal amount = params.get(0).getAsJsonObject().get("amount").getAsBigDecimal();

        TransactionReceipt txReceipt = new TransactionReceipt();
        txReceipt.putLog("to", to);
        txReceipt.putLog("amount", String.valueOf(amount));

        if (!state.contains(sender)) {
            log.info("\n[ERR] " + sender + " has no balance!");
            return txReceipt;
        }

        CoinContractStateValue senderValue = state.get(sender);

        if (senderValue.isTransferable(amount)) {
            senderValue.subtractBalance(amount);
            addBalanceTo(to, amount);
            txReceipt.setStatus(TransactionReceipt.SUCCESS);
            log.info("\n[Transferred] Transfer " + amount + " from " + sender + " to " + to);
            log.info("\nBalance of From (" + sender + ") : " + senderValue.getBalance()
                    + "\nBalance of To   (" + to + ") : " + state.get(to).getBalance());
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
    public TransactionReceipt approve(JsonArray params) {
        log.info("\napprove :: params => " + params);

        String spender = params.get(0).getAsJsonObject().get("spender").getAsString().toLowerCase();
        BigDecimal amount = params.get(0).getAsJsonObject().get("amount").getAsBigDecimal();

        TransactionReceipt txReceipt = new TransactionReceipt();
        txReceipt.putLog("spender", spender);
        txReceipt.putLog("amount", String.valueOf(amount));

        if (!state.contains(sender)) {
            log.info("\n[ERR] " + sender + " has no balance!");
            return txReceipt;
        }

        CoinContractStateValue senderValue = state.get(sender);

        if (senderValue.isTransferable(amount)) {
            senderValue.addAllowedAmount(spender, amount);
            txReceipt.setStatus(TransactionReceipt.SUCCESS);
            log.info("\n[Approved] Approve " + spender + " to "
                    + senderValue.getAllowedAmount(spender) + " from " + sender);
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
    public TransactionReceipt transferfrom(JsonArray params) {
        log.info("\ntransferfrom :: params => " + params);

        String from = params.get(0).getAsJsonObject().get("from").getAsString().toLowerCase();
        String to = params.get(0).getAsJsonObject().get("to").getAsString().toLowerCase();
        BigDecimal amount = params.get(0).getAsJsonObject().get("amount").getAsBigDecimal();

        TransactionReceipt txReceipt = new TransactionReceipt();
        txReceipt.putLog("from", from);
        txReceipt.putLog("to", to);
        txReceipt.putLog("amount", String.valueOf(amount));

        if (!state.contains(from)) {
            log.info("\n[ERR] " + from + " has no balance!");
            return txReceipt;
        }

        CoinContractStateValue fromValue = state.get(from);

        if (fromValue.isTransferable(amount)) {
            if (fromValue.isEnoughAllowedAmount(sender, amount)) {
                fromValue.subtractBalance(amount);
                fromValue.subtractAllowedAmount(sender, amount);
                addBalanceTo(to, amount);
                txReceipt.setStatus(TransactionReceipt.SUCCESS);
                log.info("\n[Transferred] Transfer " + amount + " from " + from + " to " + to);
                log.info("\nAllowed amount of Sender (" + sender + ") : "
                        + fromValue.getAllowedAmount(sender));
                log.info("\nBalance of From (" + from + ") : " + fromValue.getBalance()
                        + "\nBalance of To   (" + to + ") : " + state.get(to).getBalance());
            } else {
                log.info("\n[ERR] " + from + " has no enough amount allowed by from to sender");
            }
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
    public TransactionReceipt genesis(JsonArray params) {
        log.info("\ngenesis :: params => " + params);

        TransactionReceipt txReceipt = new TransactionReceipt();
        if (state.getState().size() > 0) {
            return txReceipt;
        }

        //totalSupply 는 alloc 의 balance 를 모두 더한 값으로 세팅
        BigDecimal totalSupply = BigDecimal.ZERO;
        JsonObject json = params.get(0).getAsJsonObject();
        JsonObject alloc = json.get("alloc").getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : alloc.entrySet()) {
            String frontier = entry.getKey();
            JsonObject value = entry.getValue().getAsJsonObject();
            BigDecimal balance = value.get("balance").getAsBigDecimal();
            totalSupply = totalSupply.add(balance);
            addBalanceTo(frontier, balance);

            txReceipt.putLog(frontier, balance);
            txReceipt.setStatus(TransactionReceipt.SUCCESS);
            log.info("\nAddress of Frontier : " + frontier
                    + "\nBalance of Frontier : " + state.get(frontier).getBalance());
        }
        CoinContractStateValue totalSupplyValue = new CoinContractStateValue();
        totalSupplyValue.addBalance(totalSupply);
        state.put(totalSupplyKey, totalSupplyValue);
        txReceipt.putLog("TotalSupply", totalSupply);
        //log.info("\n[Genesis]\nTotalSupply : " + state.getTotalSupply());

        return txReceipt;
    }

    private void addBalanceTo(String to, BigDecimal amount) {
        CoinContractStateValue toValue = state.get(to);
        if (toValue == null) {
            toValue = new CoinContractStateValue();
            state.put(to, toValue);
        }
        toValue.addBalance(amount);
    }
}
