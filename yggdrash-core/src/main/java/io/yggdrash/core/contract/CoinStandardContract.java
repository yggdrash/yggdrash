package io.yggdrash.core.contract;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.math.BigDecimal;
import java.util.Map;

public class CoinStandardContract extends BaseContract<CoinStandardStateTable>
        implements CoinStandard {

    /**
     * @return Total amount of coin in existence
     */
    @Override
    public BigDecimal totalsupply(JsonArray params) {
        log.info("\ntotalsupply :: params => " + params);
        return state.getTotalSupply();
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
            CoinStandardStateTable table = state.get(address);
            return table.getMyBalance();
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

        if (state.get(sender) != null) {
            CoinStandardStateTable senderTable = state.get(sender);
            BigDecimal senderBalance = senderTable.getMyBalance();

            if (senderBalance.subtract(amount).compareTo(BigDecimal.ZERO) < 0) {
                log.info("\n[ERR] " + sender + " has no enough balance!");
            } else {
                senderTable.setMyBalance(senderBalance.subtract(amount));

                addBalanceTo(to, amount);

                txReceipt.setStatus(TransactionReceipt.SUCCESS);
                log.info("\n[Transferred] Transfer " + amount + " from " + sender + "to " + sender);
                log.info("\nBalance of From (" + sender + ") : " + state.get(sender).getMyBalance()
                        + "\nBalance of To   (" + to + ") : " + state.get(to).getMyBalance());
            }
        } else {
            log.info("\n[ERR] " + sender + " has no balance!");
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

        if (state.get(sender) != null) {
            CoinStandardStateTable senderTable = state.get(sender);
            BigDecimal senderBalance = senderTable.getMyBalance();

            if (senderBalance.subtract(amount).compareTo(BigDecimal.ZERO) < 0) {
                log.info("\n[ERR] " + sender + " has no enough balance!");
            } else {
                BigDecimal allowedAmountBySender = senderTable.getAllowedAmount(spender);
                senderTable.setAllowance(spender, amount.add(allowedAmountBySender));

                txReceipt.setStatus(TransactionReceipt.SUCCESS);
                log.info("\n[Approved] Approve " + spender + " to "
                        + state.get(sender).getAllowedAmount(spender) + " from " + sender);
            }
        } else {
            log.info("\n[ERR] " + sender + " has no balance!");
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

        if (state.get(from) != null) {
            CoinStandardStateTable senderTable = state.get(from);
            BigDecimal senderBalance = senderTable.getMyBalance();

            if (senderBalance.subtract(amount).compareTo(BigDecimal.ZERO) < 0) {
                log.info("\n[ERR] " + from + " has no enough balance!");
            } else {
                BigDecimal allowedAmountBySender = senderTable.getAllowedAmount(to);
                if (allowedAmountBySender.subtract(amount).compareTo(BigDecimal.ZERO) < 0) {
                    log.info("\n[ERR] " + from + "has no enough amount allowed by from");
                } else {
                    senderTable.setMyBalance(senderBalance.subtract(amount));
                    senderTable.setAllowance(to, allowedAmountBySender.subtract(amount));

                    addBalanceTo(to, amount);
                }
                txReceipt.setStatus(TransactionReceipt.SUCCESS);
                log.info("\n[Transferred] Transfer " + amount + " from " + from + " to " + to);
                log.info("\nBalance of From (" + from + ") : " + state.get(from).getMyBalance()
                        + "\nBalance of To   (" + to + ") : " + state.get(to).getMyBalance());
            }
        } else {
            log.info("\n[ERR] " + sender + " has no balance!");
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
            CoinStandardStateTable frontierTable = new CoinStandardStateTable();
            frontierTable.setMyBalance(balance);
            state.put(frontier, frontierTable);

            txReceipt.putLog(frontier, balance);
            txReceipt.setStatus(TransactionReceipt.SUCCESS);
            log.info("\nAddress of Frontier : " + frontier
                    + "\nBalance of Frontier : " + state.get(frontier).getMyBalance());
        }
        state.setTotalSupply(totalSupply);
        txReceipt.putLog("TotalSupply", totalSupply);
        log.info("\n[Genesis]\nTotalSupply : " + state.getTotalSupply());

        return txReceipt;
    }

    private void addBalanceTo(String to, BigDecimal amount) {
        if (state.get(to) != null) {
            CoinStandardStateTable toTable = state.get(to);
            BigDecimal toBalance = toTable.getMyBalance();
            toTable.setMyBalance(toBalance.add(amount));
        } else {
            CoinStandardStateTable toTable = new CoinStandardStateTable();
            toTable.setMyBalance(amount);
            state.put(to, toTable);
        }
    }
}
