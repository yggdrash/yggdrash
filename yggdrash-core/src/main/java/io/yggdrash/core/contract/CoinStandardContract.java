package io.yggdrash.core.contract;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.math.BigDecimal;

public class CoinStandardContract extends BaseContract<BigDecimal> implements CoinStandard {
    /**
     * @return Total amount of coin in existence
     */
    @Override
    public BigDecimal totalsupply(JsonArray params) {
        return null;
    }

    /**
     * Gets the balance of the specified address
     * param owner   The address to query the balance of
     *
     * @return A BigDecimal representing the amount owned by the passed address
     */
    @Override
    public BigDecimal balanceof(JsonArray params) {
        String address = params.get(0).getAsJsonObject().get("address").getAsString().toLowerCase();
        if (state.get(address) != null) {
            return state.get(address);
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
        log.info("\n allowance :: params => " + params);
        String owner = params.get(0).getAsJsonObject().get("owner").getAsString().toLowerCase();
        String spender = params.get(0).getAsJsonObject().get("spender").getAsString().toLowerCase();

        TransactionReceipt txReceipt = new TransactionReceipt();
        txReceipt.putLog("owner", owner);
        txReceipt.putLog("spender", spender);

        return null;
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
        log.info("\n transfer :: params => " + params);
        String to = params.get(0).getAsJsonObject().get("address").getAsString().toLowerCase();
        BigDecimal amount = params.get(0).getAsJsonObject().get("amount").getAsBigDecimal();

        TransactionReceipt txReceipt = new TransactionReceipt();
        txReceipt.putLog("from", sender);
        txReceipt.putLog("to", to);
        txReceipt.putLog("amount", String.valueOf(amount));

        if (state.get(sender) != null) {
            BigDecimal balanceOfFrom = state.get(sender);

            if (balanceOfFrom.subtract(amount).compareTo(BigDecimal.ZERO) < 0) {
                log.info("\n[ERR] " + sender + " has no enough balance!");
            } else {
                state.replace(sender, balanceOfFrom.subtract(amount));
                if (state.get(to) != null) {
                    state.replace(to, state.get(to).add(amount));
                } else {
                    state.put(to, amount);
                }
                txReceipt.setStatus(TransactionReceipt.SUCCESS);
                log.info(
                        "\nBalance of From : " + state.get(sender)
                                + "\nBalance of To   : " + state.get(to));
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
        // Map<spender, Map<owner, Set<amount>>

        return null;
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
        log.info("\n transferfrom :: params => " + params);
        String from = params.get(0).getAsJsonObject().get("from").getAsString().toLowerCase();
        String to = params.get(0).getAsJsonObject().get("to").getAsString().toLowerCase();
        BigDecimal amount = params.get(0).getAsJsonObject().get("amount").getAsBigDecimal();

        TransactionReceipt txReceipt = new TransactionReceipt();
        txReceipt.putLog("from", from);
        txReceipt.putLog("to", to);
        txReceipt.putLog("amount", String.valueOf(amount));
        return null;
    }

    /**
     * Pre-allocate yeed to addresses
     * param frontier The Frontier is the first live release of the Yggdrash network
     * param balance  The balance of frontier
     *
     * @return TransactionReceipt
     */
    public TransactionReceipt genesis(JsonArray params) {
        TransactionReceipt txReceipt = new TransactionReceipt();
        if (state.getState().size() == 0) {
            log.info("\n genesis :: params => " + params);
            for (int i = 0; i < params.size(); i++) {
                JsonObject jsonObject = params.get(i).getAsJsonObject();
                String frontier = jsonObject.get("frontier").getAsString();
                BigDecimal balance = jsonObject.get("balance").getAsBigDecimal();
                txReceipt.putLog(String.format("frontier[%d]", i), frontier);
                txReceipt.putLog(String.format("balance[%d]", i), balance);
                state.put(frontier, balance);
                txReceipt.setStatus(TransactionReceipt.SUCCESS);
                log.info("\nAddress of Frontier : " + frontier
                        + "\nBalance of Frontier : " + balance);
            }
        }
        return txReceipt;
    }
}
