package io.yggdrash.contract.token;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.yggdrash.common.contract.vo.PrefixKeyEnum;
import io.yggdrash.common.crypto.HashUtil;
import io.yggdrash.common.crypto.HexUtil;
import io.yggdrash.common.store.BranchStateStore;
import io.yggdrash.common.utils.ByteUtil;
import io.yggdrash.contract.core.ExecuteStatus;
import io.yggdrash.contract.core.Receipt;
import io.yggdrash.contract.core.annotation.ContractBranchStateStore;
import io.yggdrash.contract.core.annotation.ContractChannelField;
import io.yggdrash.contract.core.annotation.ContractChannelMethod;
import io.yggdrash.contract.core.annotation.ContractQuery;
import io.yggdrash.contract.core.annotation.ContractReceipt;
import io.yggdrash.contract.core.annotation.ContractStateStore;
import io.yggdrash.contract.core.annotation.Genesis;
import io.yggdrash.contract.core.annotation.InvokeTransaction;
import io.yggdrash.contract.core.channel.ContractChannel;
import io.yggdrash.contract.core.channel.ContractMethodType;
import io.yggdrash.contract.core.store.ReadWriterStore;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Hashtable;

/**
 * The type Token contract.
 */
public class TokenContract implements BundleActivator, ServiceListener {
    private static final String TOKEN_CONTRACT_NAME = "TOKEN";
    private static final String TOKEN_PREFIX = "token-";

    private static final String TOKEN_ID = "tokenId";
    private static final String TOKEN_NAME = "tokenName";
    private static final String TOKEN_OWNER_ACCOUNT = "tokenOwnerAccount";
    private static final String TOKEN_INIT_YEED_STAKE_AMOUNT = "tokenInitYeedStakeAmount";

    private static final String TOKEN_INIT_MINT_AMOUNT = "tokenInitMintAmount";
    private static final String TOKEN_MINTABLE = "tokenMintable";
    private static final String TOKEN_BURNABLE = "tokenBurnable";

    private static final String TOKEN_EX_T2Y_ENABLED = "tokenExT2YEnabled";
    private static final String TOKEN_EX_T2Y_TYPE = "tokenExT2YType";
    private static final String TOKEN_EX_T2Y_TYPE_FIXED = "TOKEN_EX_T2Y_TYPE_FIXED";
    private static final String TOKEN_EX_T2Y_TYPE_LINKED = "TOKEN_EX_T2Y_TYPE_LINKED";
    private static final String TOKEN_EX_T2Y_RATE = "tokenExT2YRate";

    private static final String TOKEN_EX_T2T_RATE = "tokenExT2TRate";
    private static final String TOKEN_EX_T2T_RATE_MAP_PREFIX = "ex-t2t-rate-map-";
    private static final String TOKEN_EX_T2T_TARGET_TOKEN_ID = "tokenExT2TTargetTokenId";

    private static final String TOKEN_PHASE = "tokenPhase";
    private static final String TOKEN_PHASE_PREFIX = "phase-";
    private static final String TOKEN_PHASE_INIT = "INIT";
    private static final String TOKEN_PHASE_RUN = "RUN";
    private static final String TOKEN_PHASE_PAUSE = "PAUSE";
    private static final String TOKEN_PHASE_STOP = "STOP";

    private static final String TOTAL_SUPPLY = "TOTAL_SUPPLY";
    private static final String YEED_STAKE = "YEED_STAKE";
    private static final String ADDRESS = "address";
    private static final String AMOUNT = "amount";
    private static final String BALANCE = "balance";
    private static final String SERVICE_FEE = "serviceFee";

    private static final String SPENDER = "spender";
    private static final String OWNER = "owner";

    private static final BigInteger DEFAULT_SERVICE_FEE = BigInteger.TEN.pow(17);

    private static final Logger log = LoggerFactory.getLogger(TokenContract.class);

    @SuppressWarnings("RedundantThrows")
    @Override
    public void start(BundleContext context) throws Exception {
        log.info("Start Token Contract");

        Hashtable<String, String> props = new Hashtable<>();
        props.put("YGGDRASH", TOKEN_CONTRACT_NAME);
        context.registerService(TokenService.class.getName(), new TokenService(), props);
    }

    @Override
    public void stop(BundleContext context) {
        log.info("Stop Token contract");
    }

    @Override
    public void serviceChanged(ServiceEvent event) {
        log.info("Token contract serviceChanged called");
    }


    public static class TokenService {

        @SuppressWarnings("WeakerAccess")
        @ContractChannelField
        public ContractChannel channel;

        @ContractReceipt
        Receipt txReceipt;

        @ContractStateStore
        ReadWriterStore<String, JsonObject> store;

        @ContractBranchStateStore
        BranchStateStore branchStateStore;

        /**
         * Genesis of CONTRACT not TOKEN
         *
         * @param params the params
         * @return the receipt
         */
        @Genesis
        @InvokeTransaction
        public Receipt init(JsonObject params) {
            setSuccessTxReceipt("Token contract i18n completed successfully.");

            return txReceipt;
        }

        /**
         * Total supply for a token
         *
         * @issuer anonymous
         * @param params the params
         *               @tokenId
         * @return Total amount of token in existence
         */
        @ContractQuery
        public BigInteger totalSupply(JsonObject params) {
            String tokenId = params.get(TOKEN_ID).getAsString().toLowerCase();

            JsonObject token = loadTokenObject(tokenId);

            if (token == null) {
                return null;
            }

            return getBalance(tokenId, TOTAL_SUPPLY);
        }

        /**
         * Balance of an account for a token
         *
         * @issuer anonymous
         * @param params the params
         *               @tokenId
         *               @address account addressO
         * @return Balance of an account for a token
         */
        @ContractQuery
        public BigInteger balanceOf(JsonObject params) {
            String tokenId = params.get(TOKEN_ID).getAsString().toLowerCase();
            String address = params.get(ADDRESS).getAsString().toLowerCase();

            JsonObject token = loadTokenObject(tokenId);
            if (token == null) {
                return null;
            }

            return getBalance(tokenId, address);
        }

        /**
         * Yeed stake for a token
         *
         * @issuer anonymous
         * @param params the params
         *               @tokenId
         * @return Yeed stake for a token
         */
        @ContractQuery
        @ContractChannelMethod
        public BigInteger getYeedBalanceOf(JsonObject params) {
            String tokenId = params.get(TOKEN_ID).getAsString().toLowerCase();
            return getYeedBalanceOfSub(tokenId);
        }

        private BigInteger getYeedBalanceOfSub(String tokenId) {
            String targetAddress =
                    PrefixKeyEnum.ACCOUNT.toValue().concat(getTokenAddress(tokenId, YEED_STAKE));
            JsonObject storeValue = store.get(targetAddress);

            return storeValue != null && storeValue.has(BALANCE)
                    ? storeValue.get(BALANCE).getAsBigInteger() : BigInteger.ZERO;
        }

        private void setYeedBalanceOfSub(String tokenId, BigInteger amount) {
            putBalance(tokenId, YEED_STAKE, amount);
        }

        /**
         * Allowance of a spender by an owner for a token
         *
         * @issuer anonymous
         * @param params the params
         *               @tokenId
         *               @owner
         *               @spender
         * @return Allowance of a spender by an owner for a token
         */
        @ContractQuery
        public BigInteger allowance(JsonObject params) {
            String tokenId = params.get(TOKEN_ID).getAsString().toLowerCase();
            String owner = params.get(OWNER).getAsString().toLowerCase();
            String spender = params.get(SPENDER).getAsString().toLowerCase();

            JsonObject token = loadTokenObject(tokenId);
            if (token == null) {
                return null;
            }

            String approveKey = approveKey(owner, spender);
            return getBalance(tokenId, approveKey);
        }

        /**
         * Create a token
         *
         * @issuer anonymous, is to be the owner of the token
         * @param params the params
         *               @tokenId
         *               @tokenName Name of the token to create
         *               @tokenInitYeedStakeAmount Amount of YEEDs to stake initially
         *               @tokenInitMintAmount Amount of tokens mint initially
         *               @tokenMintable Whether the additional mint of the token is available
         *               @tokenBurnable Whether the burn of the token is available
         *               @tokenExT2YEnabled Whether the exchange between the token and YEED is available
         *               @tokenExT2YType Type of T2Y(Y2T) exchange (FIXED or LINKED)
         *               @tokenExT2YRate Exchange rate for FIXED T2Y(Y2T) exchange
         * @return the receipt
         */
        @InvokeTransaction
        public Receipt createToken(JsonObject params) {
            String tokenId = params.get(TOKEN_ID).getAsString().toLowerCase();
            JsonObject token = loadTokenObject(tokenId);
            if (token != null) {
                setErrorTxReceipt(
                        String.format("Token [%s] already exists!", tokenId));
                return txReceipt;
            }

            String ownerAccount = txReceipt.getIssuer();
            token = makeTokenObject(ownerAccount, params);
            if (token == null) {
                setErrorTxReceipt("Token creation is failed. Check parameters!");
                return txReceipt;
            }

            BigInteger initMintAmount = token.get(TOKEN_INIT_MINT_AMOUNT).getAsBigInteger();
            putBalance(tokenId, ownerAccount, initMintAmount);
            putBalance(tokenId, TOTAL_SUPPLY, initMintAmount);

            // STAKE
            BigInteger stakeAmount = token.get(TOKEN_INIT_YEED_STAKE_AMOUNT).getAsBigInteger();
            boolean isSuccess = depositYeedStakeSub(txReceipt.getIssuer(), stakeAmount);
            if (isSuccess == false) {
                setErrorTxReceipt("Insufficient balance to stake!");
                return txReceipt;
            }

            // create token success!
            store.put(TOKEN_PREFIX.concat(tokenId), token);

            String msg = String.format(
                    "Token [%s] creation completed successfully. Initial mint amount is %s.",
                    tokenId, initMintAmount
            );
            setSuccessTxReceipt(msg);

            return txReceipt;
        }

        private JsonObject makeTokenObject(String ownerAccount, JsonObject params) {
            if (!checkParamsNotNull(params, new String[] {
                    TOKEN_ID, TOKEN_NAME, TOKEN_INIT_YEED_STAKE_AMOUNT, TOKEN_INIT_MINT_AMOUNT,
                    TOKEN_MINTABLE, TOKEN_BURNABLE, TOKEN_EX_T2Y_ENABLED})) {
                return null;
            }
            String tokenId = params.get(TOKEN_ID).getAsString().toLowerCase();

            JsonObject token = new JsonObject();

            token.addProperty(TOKEN_ID, tokenId);
            token.addProperty(TOKEN_NAME, params.get(TOKEN_NAME).getAsString());
            token.addProperty(TOKEN_OWNER_ACCOUNT, ownerAccount);
            token.addProperty(TOKEN_INIT_YEED_STAKE_AMOUNT, params.get(TOKEN_INIT_YEED_STAKE_AMOUNT).getAsBigInteger());
            token.addProperty(TOKEN_INIT_MINT_AMOUNT, params.get(TOKEN_INIT_MINT_AMOUNT).getAsBigInteger());
            token.addProperty(TOKEN_MINTABLE, params.get(TOKEN_MINTABLE).getAsBoolean());
            token.addProperty(TOKEN_BURNABLE, params.get(TOKEN_BURNABLE).getAsBoolean());
            token.addProperty(TOKEN_EX_T2Y_ENABLED, params.get(TOKEN_EX_T2Y_ENABLED).getAsBoolean());

            if (params.get(TOKEN_EX_T2Y_ENABLED).getAsBoolean()) {
                if (params.get(TOKEN_EX_T2Y_TYPE) == null) {
                    return null;
                }
                token.addProperty(TOKEN_EX_T2Y_TYPE, params.get(TOKEN_EX_T2Y_TYPE).getAsString());
                if (TOKEN_EX_T2Y_TYPE_FIXED.equals(params.get(TOKEN_EX_T2Y_TYPE).getAsString())) {
                    BigDecimal exT2YRate = params.get(TOKEN_EX_T2Y_RATE).getAsBigDecimal();
                    if (exT2YRate.compareTo(BigDecimal.ZERO) <= 0) {
                        return null;
                    }
                    token.addProperty(TOKEN_EX_T2Y_RATE, exT2YRate);
                }
            }

            saveTokenPhase(tokenId, TOKEN_PHASE_INIT);
            putBalance(
                    tokenId,
                    YEED_STAKE,
                    params.get(TOKEN_INIT_YEED_STAKE_AMOUNT).getAsBigInteger());

            return token;
        }

        private boolean checkParamsNotNull(JsonObject params, String[] notNullKeys) {
            for (String key : notNullKeys) {
                if (params.get(key) == null) {
                    return false;
                }
            }

            return true;
        }


        /**
         * Deposit 'amount' of YEED to a token.
         * It is possible only by the owner of the token.
         * Token's YEED stake increases by 'amount'
         *
         * @issuer the owner of the token
         * @param params the params
         *               @tokenId
         *               @amount amount of additional YEED stake
         * @return the receipt
         */
        @InvokeTransaction
        public Receipt depositYeedStake(JsonObject params) {
            String issuer = txReceipt.getIssuer();
            String tokenId = params.get(TOKEN_ID).getAsString().toLowerCase();
            JsonObject token = loadTokenObject(tokenId);

            if (token == null) {
                setErrorTxReceipt(
                        String.format("Token [%s] does not exist!", tokenId));
                return txReceipt;
            }

            if (issuer.equals(token.get(TOKEN_OWNER_ACCOUNT).getAsString()) == false) {
                setErrorTxReceipt("Issuer must be token owner!");
                return txReceipt;
            }

            BigInteger amount = params.get(AMOUNT).getAsBigInteger();
            boolean isSuccess = depositYeedStakeSub(issuer, amount);
            if (isSuccess == false) {
                setErrorTxReceipt("Insufficient balance to deposit!");
                return txReceipt;
            }

            BigInteger curStakeOfToken = getBalance(tokenId, YEED_STAKE);
            setYeedBalanceOfSub(tokenId, curStakeOfToken.add(amount));

            String msg = String.format(
                    "Token [%s] yeed stake deposit completed successfully. Amount is %s.",
                    tokenId, amount
            );
            setSuccessTxReceipt(msg);

            return txReceipt;
        }

        /**
         * Withdraws 'amount' of YEED stake from a token's YEED balance
         *
         * @issuer the owner of the token
         * @param params the params
         *               @tokenId
         *               @amount amount of YEED stake to withdraw
         * @return the receipt
         */
        @InvokeTransaction
        public Receipt withdrawYeedStake(JsonObject params) {
            String issuer = txReceipt.getIssuer();
            String tokenId = params.get(TOKEN_ID).getAsString().toLowerCase();
            JsonObject token = loadTokenObject(tokenId);

            if (token == null) {
                setErrorTxReceipt(
                        String.format("Token [%s] does not exist!", tokenId));
                return txReceipt;
            }

            if (issuer.equals(token.get(TOKEN_OWNER_ACCOUNT).getAsString()) == false) {
                setErrorTxReceipt("Issuer must be token owner!");
                return txReceipt;
            }

            BigInteger amount = params.get(AMOUNT).getAsBigInteger();
            boolean isSuccess = withdrawYeedStakeSub(issuer, amount);
            if (isSuccess == false) {
                setErrorTxReceipt("Insufficient balance to withdraw!");
                return txReceipt;
            }

            BigInteger curStakeOfToken = getBalance(tokenId, YEED_STAKE);
            setYeedBalanceOfSub(tokenId, curStakeOfToken.subtract(amount));

            String msg = String.format(
                    "Token [%s] yeed stake withdrawal completed successfully. Amount is %s.",
                    tokenId, amount
            );
            setSuccessTxReceipt(msg);

            return txReceipt;
        }

        /**
         * Move the token phase to "run"
         *
         * @issuer the owner of the token
         * @param params the params
         *               @tokenId
         * @return the receipt
         */
        @InvokeTransaction
        public Receipt movePhaseRun(JsonObject params) {
            String issuer = txReceipt.getIssuer();
            String tokenId = params.get(TOKEN_ID).getAsString().toLowerCase();
            JsonObject token = loadTokenObject(tokenId);

            if (token == null) {
                setErrorTxReceipt(
                        String.format("Token [%s] does not exist!", tokenId));
                return txReceipt;
            }

            if (issuer.equals(token.get(TOKEN_OWNER_ACCOUNT).getAsString()) == false) {
                setErrorTxReceipt("Issuer must be token owner!");
                return txReceipt;
            }

            String phase = loadTokenPhase(tokenId);
            if ((TOKEN_PHASE_INIT.equals(phase) || TOKEN_PHASE_PAUSE.equals(phase)) == false) {
                log.debug(String.format("[movePhaseRun] cur phase = %s", phase));
                setErrorTxReceipt("If you want to move token phase to RUN, current token phase must be INIT or PAUSE!");
                return txReceipt;
            }

            if (getYeedBalanceOfSub(tokenId).compareTo(DEFAULT_SERVICE_FEE) < 0) {
                setErrorTxReceipt("Insufficient yeed stake of the token for service fee!");
                return txReceipt;
            }
            if (burnServiceFeeFromYeedStake(tokenId, DEFAULT_SERVICE_FEE) == false) {
                setErrorTxReceipt("Insufficient service fee in yeed stake!");
                return txReceipt;
            }

            saveTokenPhase(tokenId, TOKEN_PHASE_RUN);

            setSuccessTxReceipt("Token phase was moved to RUN!");

            return txReceipt;
        }

        /**
         * Move the token phase to "pause"
         *
         * @issuer the owner of the token
         * @param params the params
         *               @tokenId
         * @return the receipt
         */
        @InvokeTransaction
        public Receipt movePhasePause(JsonObject params) {
            String issuer = txReceipt.getIssuer();
            String tokenId = params.get(TOKEN_ID).getAsString().toLowerCase();
            JsonObject token = loadTokenObject(tokenId);

            if (token == null) {
                setErrorTxReceipt(
                        String.format("Token [%s] does not exist!", tokenId));
                return txReceipt;
            }

            if (issuer.equals(token.get(TOKEN_OWNER_ACCOUNT).getAsString()) == false) {
                setErrorTxReceipt("Issuer must be token owner!");
                return txReceipt;
            }

            String phase = loadTokenPhase(tokenId);
            if (TOKEN_PHASE_RUN.equals(phase) == false) {
                log.debug(String.format("[movePhasePause] cur phase = %s", phase));
                setErrorTxReceipt("If you want to move token phase to PAUSE, current token phase must be RUN!");
                return txReceipt;
            }

            if (getYeedBalanceOfSub(tokenId).compareTo(DEFAULT_SERVICE_FEE) < 0) {
                setErrorTxReceipt("Insufficient yeed stake of the token for service fee!");
                return txReceipt;
            }
            if (burnServiceFeeFromYeedStake(tokenId, DEFAULT_SERVICE_FEE) == false) {
                setErrorTxReceipt("Insufficient service fee in yeed stake!");
                return txReceipt;
            }

            saveTokenPhase(tokenId, TOKEN_PHASE_PAUSE);

            setSuccessTxReceipt("Token phase was moved to PAUSE!");

            return txReceipt;
        }

        /**
         * Move the token phase to "stop"
         *
         * @issuer the owner of the token
         * @param params the params
         *               @tokenId
         * @return the receipt
         */
        @InvokeTransaction
        public Receipt movePhaseStop(JsonObject params) {
            String issuer = txReceipt.getIssuer();
            String tokenId = params.get(TOKEN_ID).getAsString().toLowerCase();
            JsonObject token = loadTokenObject(tokenId);

            if (token == null) {
                setErrorTxReceipt(
                        String.format("Token [%s] does not exist!", tokenId));
                return txReceipt;
            }

            if (issuer.equals(token.get(TOKEN_OWNER_ACCOUNT).getAsString()) == false) {
                setErrorTxReceipt("Issuer must be token owner!");
                return txReceipt;
            }

            String phase = loadTokenPhase(tokenId);
            if ((TOKEN_PHASE_RUN.equals(phase) || TOKEN_PHASE_PAUSE.equals(phase)) == false) {
                log.debug(String.format("[movePhaseStop] cur phase = %s", phase));
                setErrorTxReceipt("If you want to move token phase to STOP, current token phase must be RUN or PAUSE!");
                return txReceipt;
            }

            if (getYeedBalanceOfSub(tokenId).compareTo(DEFAULT_SERVICE_FEE) < 0) {
                setErrorTxReceipt("Insufficient yeed stake of the token for service fee!");
                return txReceipt;
            }
            if (burnServiceFeeFromYeedStake(tokenId, DEFAULT_SERVICE_FEE) == false) {
                setErrorTxReceipt("Insufficient service fee in yeed stake!");
                return txReceipt;
            }

            saveTokenPhase(tokenId, TOKEN_PHASE_STOP);

            setSuccessTxReceipt("Token phase was moved to STOP!");

            return txReceipt;
        }

        /**
         * Destroy the token
         *
         * @issuer the owner of the token
         * @param params the params
         *               @tokenId
         * @return the receipt
         */
        @InvokeTransaction
        public Receipt destroyToken(JsonObject params) {
            String issuer = txReceipt.getIssuer();
            String tokenId = params.get(TOKEN_ID).getAsString().toLowerCase();
            JsonObject token = loadTokenObject(tokenId);

            if (token == null) {
                setErrorTxReceipt(
                        String.format("Token [%s] does not exist!", tokenId));
                return txReceipt;
            }

            if (issuer.equals(token.get(TOKEN_OWNER_ACCOUNT).getAsString()) == false) {
                setErrorTxReceipt("Issuer must be token owner!");
                return txReceipt;
            }

            String phase = loadTokenPhase(tokenId);
            if (TOKEN_PHASE_STOP.equals(phase) == false) {
                log.debug(String.format("[movePhaseStop] cur phase = %s", phase));
                setErrorTxReceipt("If you want to destroy the token, current token phase must be STOP!");
                return txReceipt;
            }

            // TODO : kevin : 2019-10-08 : return yeed stake to owner account!
            if (getYeedBalanceOfSub(tokenId).compareTo(DEFAULT_SERVICE_FEE) < 0) {
                if (burnServiceFeeFromYeedStake(tokenId, getYeedBalanceOfSub(tokenId))) {
                    setErrorTxReceipt("Burning service fee in yeed stake failed!");
                    return txReceipt;
                }
            }
            else if (burnServiceFeeFromYeedStake(tokenId, DEFAULT_SERVICE_FEE) == false) {
                setErrorTxReceipt("Insufficient service fee in yeed stake!");
                return txReceipt;
            }


            return null;
        }

        /**
         * Transfers 'amount' of issuer's tokens to 'to' account
         *
         * @issuer an account who wants to transfer to another account
         * @param params the params
         *               @tokenId
         *               @to Target account to transfer
         *               @amount Token amount to transfer
         * @return the receipt
         */
        @InvokeTransaction
        public Receipt transfer(JsonObject params) {
            String tokenId = params.get(TOKEN_ID).getAsString().toLowerCase();
            String to = params.get("to").getAsString().toLowerCase();

            JsonElement transferAmountElement = params.get(AMOUNT);
            if (transferAmountElement == null) {
                setErrorTxReceipt("Transfer amount is empty!");
                return txReceipt;
            }
            BigInteger transferAmount = transferAmountElement.getAsBigInteger();

            JsonObject token = loadTokenObject(tokenId);

            if (token == null) {
                setErrorTxReceipt(
                        String.format("Token [%s] does not exist!", tokenId));
                return txReceipt;
            }

            if (isTokenRunning(tokenId) == false) {
                setErrorTxReceipt("Token is not running!");
                return txReceipt;
            }

            String issuer = txReceipt.getIssuer();
            if (to.equals(issuer)) {
                setErrorTxReceipt("Transfer 'to' account must be different from issuer!");
                return txReceipt;
            }

            BigInteger fromBalance = getBalance(tokenId, issuer);

            if (transferAmount.compareTo(BigInteger.ZERO) <= 0) {
                setErrorTxReceipt("Transfer amount must be greater than ZERO!");
                return txReceipt;
            }

            if (fromBalance.compareTo(transferAmount) < 0) {
                setErrorTxReceipt("Insufficient balance to transfer!");
                return txReceipt;
            }

            if (getYeedBalanceOfSub(tokenId).compareTo(DEFAULT_SERVICE_FEE) < 0) {
                setErrorTxReceipt("Insufficient yeed stake of the token for service fee!");
                return txReceipt;
            }
            if (burnServiceFeeFromYeedStake(tokenId, DEFAULT_SERVICE_FEE) == false) {
                setErrorTxReceipt("Insufficient service fee in yeed stake!");
                return txReceipt;
            }

            BigInteger newFromBalance = getBalance(tokenId, issuer).subtract(transferAmount);
            BigInteger newToBalance = getBalance(tokenId, to).add(transferAmount);
            putBalance(tokenId, issuer, newFromBalance);
            putBalance(tokenId, to, newToBalance);

            String msg = String.format(
                    "[Token Transferred] Token [%s] transfer %s from %s to %s.",
                    tokenId, transferAmount, issuer, to
            );
            setSuccessTxReceipt(msg);

            return txReceipt;
        }

        /**
         * Allows 'spender' to use 'amount' of issuer's token at transferFrom
         *
         * @issuer an account, 'sender' who wants to allow another account to spend sender's token
         * @param params the params
         *               @tokenId
         *               @spender who was allowed to use the token at transferFrom
         *               @amount allowed amount of the token to use at transferFrom
         * @return the receipt
         */
        @InvokeTransaction
        public Receipt approve(JsonObject params) {
            String tokenId = params.get(TOKEN_ID).getAsString().toLowerCase();
            JsonObject token = loadTokenObject(tokenId);

            if (token == null) {
                setErrorTxReceipt(
                        String.format("Token [%s] does not exist!", tokenId));
                return txReceipt;
            }

            if (isTokenRunning(tokenId) == false) {
                setErrorTxReceipt("Token is not running!");
                return txReceipt;
            }

            if (getYeedBalanceOfSub(tokenId).compareTo(DEFAULT_SERVICE_FEE) < 0) {
                setErrorTxReceipt("Insufficient yeed stake of the token for service fee!");
                return txReceipt;
            }
            if (burnServiceFeeFromYeedStake(tokenId, DEFAULT_SERVICE_FEE) == false) {
                setErrorTxReceipt("Insufficient service fee in yeed stake!");
                return txReceipt;
            }

            String sender = txReceipt.getIssuer();
            String spender = params.get(SPENDER).getAsString().toLowerCase();
            BigInteger approveAmount = params.get(AMOUNT).getAsBigInteger();
            String approveKey = approveKey(sender, spender);
            putBalance(tokenId, approveKey, approveAmount);

            String msg = String.format(
                    "[Token Approved] Token [%s] approve %s to %s from %s.",
                    tokenId, spender, approveAmount, sender
            );
            setSuccessTxReceipt(msg);

            return txReceipt;
        }

        /**
         * Send 'amount' tokens from account 'from' to account 'to'
         *
         * @issuer an account, who wants to transfer some amount of tokens from 'from' to 'to'
         * @param params the params
         *               @tokenId
         *               @from an account approved issuer to use its tokens at transferFrom
         *               @to a target account
         *               @amount amount of tokens that issuer wants to transfer from 'from' to 'to'
         * @return the receipt
         */
        @InvokeTransaction
        public Receipt transferFrom(JsonObject params) {
            String tokenId = params.get(TOKEN_ID).getAsString().toLowerCase();
            JsonObject token = loadTokenObject(tokenId);

            if (token == null) {
                setErrorTxReceipt(
                        String.format("Token [%s] does not exist!", tokenId));
                return txReceipt;
            }

            if (isTokenRunning(tokenId) == false) {
                setErrorTxReceipt("Token is not running!");
                return txReceipt;
            }

            String from = params.get("from").getAsString().toLowerCase();
            String to = params.get("to").getAsString().toLowerCase();
            BigInteger transferAmount = params.get(AMOUNT).getAsBigInteger();
            if (transferAmount.compareTo(BigInteger.ZERO) <= 0) {
                setErrorTxReceipt("Transfer amount must be greater than ZERO!");
                return txReceipt;
            }

            String sender = txReceipt.getIssuer();
            String approveKey = approveKey(from, sender);
            BigInteger approveBalance = getBalance(tokenId, approveKey);
            if (transferAmount.compareTo(approveBalance) > 0) {
                setErrorTxReceipt("Insufficient approved balance to transferFrom!");
                return txReceipt;
            }

            BigInteger fromBalance = getBalance(tokenId, from);
            if (transferAmount.compareTo(fromBalance) > 0) {
                setErrorTxReceipt("Insufficient balance of from account to transferFrom!");
                return txReceipt;
            }

            if (getYeedBalanceOfSub(tokenId).compareTo(DEFAULT_SERVICE_FEE) < 0) {
                setErrorTxReceipt("Insufficient yeed stake of the token for service fee!");
                return txReceipt;
            }
            if (burnServiceFeeFromYeedStake(tokenId, DEFAULT_SERVICE_FEE) == false) {
                setErrorTxReceipt("Insufficient service fee in yeed stake!");
                return txReceipt;
            }

            BigInteger newApproveBalance = getBalance(tokenId, approveKey).subtract(transferAmount);
            BigInteger newToBalance = getBalance(tokenId, to).add(transferAmount);
            putBalance(tokenId, approveKey, newApproveBalance);
            putBalance(tokenId, to, newToBalance);

            String msg = String.format(
                    "[Token TransferredFrom] Token [%s] transferred %s from %s to %s by %s.",
                    tokenId, transferAmount, from, to, from
            );
            setSuccessTxReceipt(msg);

            return txReceipt;
        }

        /**
         * Mints additional tokens. Only owner can call. Minted tokens will be added to owner's token balance
         *
         * @issuer token owner
         * @param params the params
         *               @tokenId
         *               @amount
         * @return the receipt
         */
        @InvokeTransaction
        public Receipt mint(JsonObject params) {
            String tokenId = params.get(TOKEN_ID).getAsString().toLowerCase();
            JsonObject token = loadTokenObject(tokenId);

            if (token == null) {
                setErrorTxReceipt(
                        String.format("Token [%s] does not exist!", tokenId));
                return txReceipt;
            }

            if (txReceipt.getIssuer().equals(token.get(TOKEN_OWNER_ACCOUNT).getAsString()) == false) {
                setErrorTxReceipt("Issuer must be token owner!");
                return txReceipt;
            }

            boolean mintable = token.get(TOKEN_MINTABLE).getAsBoolean();
            if (mintable == false) {
                setErrorTxReceipt("Token is not mintable!");
                return txReceipt;
            }

            String phase = loadTokenPhase(tokenId);
            if (TOKEN_PHASE_STOP.equals(phase)) {
                log.debug("[mint] cur phase = ".concat(phase));
                setErrorTxReceipt("Token phase is STOP!");
                return txReceipt;
            }

            String tokenOwnerAccount = token.get(TOKEN_OWNER_ACCOUNT).getAsString();
            BigInteger mintAmount = params.get(AMOUNT).getAsBigInteger();

            if (mintAmount.compareTo(BigInteger.ZERO) <= 0) {
                setErrorTxReceipt("Mint amount must be greater than ZERO!");
                return txReceipt;
            }

            if (getYeedBalanceOfSub(tokenId).compareTo(DEFAULT_SERVICE_FEE) < 0) {
                setErrorTxReceipt("Insufficient yeed stake of the token for service fee!");
                return txReceipt;
            }
            if (burnServiceFeeFromYeedStake(tokenId, DEFAULT_SERVICE_FEE) == false) {
                setErrorTxReceipt("Insufficient service fee in yeed stake!");
                return txReceipt;
            }

            BigInteger tokenOwnerAccountBalance = getBalance(tokenId, tokenOwnerAccount);
            BigInteger newTokenOwnerAccountBalance = tokenOwnerAccountBalance.add(mintAmount);

            putBalance(tokenId, tokenOwnerAccount, newTokenOwnerAccountBalance);

            BigInteger totalSupply = getBalance(tokenId, TOTAL_SUPPLY);
            putBalance(tokenId, TOTAL_SUPPLY, totalSupply.add(mintAmount));

            setSuccessTxReceipt(
                    String.format("Token [%s] minted %s.", tokenId, mintAmount));

            return txReceipt;
        }

        /**
         * Burns 'amount' of tokens. Only owner can call.
         *
         * @issuer the owner of the token
         * @param params the params
         *               @tokenId
         *               @amount
         * @return the receipt
         */
        @InvokeTransaction
        public Receipt burn(JsonObject params) {
            String tokenId = params.get(TOKEN_ID).getAsString().toLowerCase();
            JsonObject token = loadTokenObject(tokenId);

            if (token == null) {
                setErrorTxReceipt(
                        String.format("Token [%s] does not exist!", tokenId));
                return txReceipt;
            }

            if (txReceipt.getIssuer().equals(token.get(TOKEN_OWNER_ACCOUNT).getAsString()) == false) {
                setErrorTxReceipt("Issuer must be token owner!");
                return txReceipt;
            }

            boolean burnable = token.get(TOKEN_BURNABLE).getAsBoolean();
            if (burnable == false) {
                setErrorTxReceipt("Token is not burnable!");
                return txReceipt;
            }

            String phase = loadTokenPhase(tokenId);
            if (TOKEN_PHASE_STOP.equals(phase)) {
                log.debug("[burn] cur phase = ".concat(phase));
                setErrorTxReceipt("Token phase is STOP!");
                return txReceipt;
            }

            String tokenOwnerAccount = token.get(TOKEN_OWNER_ACCOUNT).getAsString();
            BigInteger burnAmount = params.get(AMOUNT).getAsBigInteger();

            if (burnAmount.compareTo(BigInteger.ZERO) <= 0) {
                setErrorTxReceipt("Burn amount must be greater than ZERO!");
                return txReceipt;
            }

            BigInteger tokenOwnerAccountBalance = getBalance(tokenId, tokenOwnerAccount);

            if (tokenOwnerAccountBalance.compareTo(burnAmount) < 0) {
                setErrorTxReceipt("Insufficient token owner balance to burn!");
                return txReceipt;
            }

            if (getYeedBalanceOfSub(tokenId).compareTo(DEFAULT_SERVICE_FEE) < 0) {
                setErrorTxReceipt("Insufficient yeed stake of the token for service fee!");
                return txReceipt;
            }
            if (burnServiceFeeFromYeedStake(tokenId, DEFAULT_SERVICE_FEE) == false) {
                setErrorTxReceipt("Insufficient service fee in yeed stake!");
                return txReceipt;
            }

            BigInteger newTokenOwnerAccountBalance = tokenOwnerAccountBalance.subtract(burnAmount);
            putBalance(tokenId, tokenOwnerAccount, newTokenOwnerAccountBalance);

            BigInteger totalSupply = getBalance(tokenId, TOTAL_SUPPLY);
            putBalance(tokenId, TOTAL_SUPPLY, totalSupply.subtract(burnAmount));

            setSuccessTxReceipt(
                    String.format("[Token Burned] Token [%s] burned %s.", tokenId, burnAmount));

            return txReceipt;
        }

        /**
         * Exchange tokens to YEEDs
         *
         * @issuer an account
         * @param params the params
         *               @tokenId
         *               @amount Amount of tokens to exchange to YEED
         * @return the receipt
         */
        @InvokeTransaction
        public Receipt exchangeT2Y(JsonObject params) {
            String tokenId = params.get(TOKEN_ID).getAsString().toLowerCase();

            JsonObject token = loadTokenObject(tokenId);

            if (token == null) {
                setErrorTxReceipt(
                        String.format("Token [%s] does not exist!", tokenId));
                return txReceipt;
            }

            if (isTokenRunning(tokenId) == false) {
                setErrorTxReceipt("Token is not running!");
                return txReceipt;
            }

            boolean exchangeable = token.get(TOKEN_EX_T2Y_ENABLED).getAsBoolean();
            if (exchangeable == false) {
                setErrorTxReceipt("Token is not exchangeable with YEED!");
                return txReceipt;
            }

            String issuer = txReceipt.getIssuer();
            BigInteger issuerTokenBalance = getBalance(tokenId, issuer);
            BigInteger tokenAmount = params.get(AMOUNT).getAsBigInteger();

            if (tokenAmount.compareTo(BigInteger.ZERO) <= 0) {
                setErrorTxReceipt("Exchange amount must be greater than ZERO!");
                return txReceipt;
            }

            if (issuerTokenBalance.compareTo(tokenAmount) < 0) {
                setErrorTxReceipt("Insufficient balance to exchange!");
                return txReceipt;
            }

            String exType = token.get(TOKEN_EX_T2Y_TYPE).getAsString();
            BigDecimal tokenAmountDecimal = new BigDecimal(tokenAmount);
            BigDecimal exRate = BigDecimal.ONE;
            switch (exType) {
                case TOKEN_EX_T2Y_TYPE_FIXED:
                    exRate = token.get(TOKEN_EX_T2Y_RATE).getAsBigDecimal();
                    break;
                case TOKEN_EX_T2Y_TYPE_LINKED:
                    BigDecimal decimalStake = new BigDecimal(getYeedBalanceOf(params));
                    BigDecimal decimalTotalSupply = new BigDecimal(getBalance(tokenId, TOTAL_SUPPLY));
                    exRate = decimalTotalSupply.divide(decimalStake, 18, RoundingMode.HALF_EVEN);
                    break;
                default :
                    break;
            }

            BigInteger yeedAmount = tokenAmountDecimal
                    .divide(exRate, 18, RoundingMode.HALF_EVEN)
                    .setScale(0, RoundingMode.HALF_EVEN).toBigInteger();

            BigInteger yeedStakeAmountOfToken = getBalance(tokenId, YEED_STAKE);
            if (yeedStakeAmountOfToken.compareTo(yeedAmount) < 0) {
                setErrorTxReceipt("Insufficient yeed stake amount of the token to withdraw!");
                return txReceipt;
            }

            boolean isWithdrawalSuccess = withdrawYeedStakeSub(issuer, yeedAmount);
            if (isWithdrawalSuccess == false) {
                setErrorTxReceipt("Insufficient yeed stake amount of the contract to withdraw!");
                return txReceipt;
            }

            if (getYeedBalanceOfSub(tokenId).compareTo(DEFAULT_SERVICE_FEE) < 0) {
                setErrorTxReceipt("Insufficient yeed stake of the token for service fee!");
                return txReceipt;
            }
            if (burnServiceFeeFromYeedStake(tokenId, DEFAULT_SERVICE_FEE) == false) {
                setErrorTxReceipt("Insufficient service fee in yeed stake!");
                return txReceipt;
            }

            BigInteger curYeedStakeOfToken = getBalance(tokenId, YEED_STAKE);
            putBalance(tokenId, YEED_STAKE, curYeedStakeOfToken.subtract(yeedAmount));

            BigInteger curTokenBalance = getBalance(tokenId, issuer);
            putBalance(tokenId, issuer, curTokenBalance.subtract(tokenAmount));

            BigInteger curTotalSupplyOfToken = getBalance(tokenId, TOTAL_SUPPLY);
            putBalance(tokenId, TOTAL_SUPPLY, curTotalSupplyOfToken.subtract(tokenAmount));

            String msg = String.format(
                    "Token [%s] was exchanged to YEED successfully. Token %s was exchanged to YEED %s.",
                    tokenId, tokenAmount, yeedAmount
            );
            setSuccessTxReceipt(msg);

            return txReceipt;
        }

        /**
         * Exchange YEEDs to tokens
         *
         * @issuer an account
         * @param params the params
         *               @tokenId
         *               @amount Amount of YEEDs to exchange to tokens
         * @return the receipt
         */
        @InvokeTransaction
        public Receipt exchangeY2T(JsonObject params) {
            String tokenId = params.get(TOKEN_ID).getAsString().toLowerCase();
            JsonObject token = loadTokenObject(tokenId);

            if (token == null) {
                setErrorTxReceipt(
                        String.format("Token [%s] does not exist!", tokenId));
                return txReceipt;
            }

            if (isTokenRunning(tokenId) == false) {
                setErrorTxReceipt("Token is not running!");
                return txReceipt;
            }

            boolean exchangeable = token.get(TOKEN_EX_T2Y_ENABLED).getAsBoolean();
            if (exchangeable == false) {
                setErrorTxReceipt("Token is not exchangeable with YEED!");
                return txReceipt;
            }

            String issuer = txReceipt.getIssuer();
            BigInteger yeedAmount = params.get(AMOUNT).getAsBigInteger();

            if (yeedAmount.compareTo(BigInteger.ZERO) <= 0) {
                setErrorTxReceipt("Exchange amount must be greater than ZERO!");
                return txReceipt;
            }

            String exType = token.get(TOKEN_EX_T2Y_TYPE).getAsString();
            BigDecimal exRate = BigDecimal.ONE;

            switch (exType) {
                case TOKEN_EX_T2Y_TYPE_FIXED:
                    exRate = token.get(TOKEN_EX_T2Y_RATE).getAsBigDecimal();
                    break;
                case TOKEN_EX_T2Y_TYPE_LINKED:
                    BigDecimal decimalStake = new BigDecimal(getYeedBalanceOf(params));
                    BigDecimal decimalTotalSupply = new BigDecimal(getBalance(tokenId, TOTAL_SUPPLY));
                    exRate = decimalTotalSupply.divide(decimalStake, 18, RoundingMode.HALF_EVEN);
                    break;
                default:
                    break;
            }

            // deposit with service fee in YEED
            boolean isDepositSuccess = depositYeedStakeSub(issuer, yeedAmount);
            if (isDepositSuccess == false) {
                setErrorTxReceipt("Insufficient yeed balance to deposit!");
                return txReceipt;
            }

            BigInteger curYeedStakeOfToken = getBalance(tokenId, YEED_STAKE);
            putBalance(tokenId, YEED_STAKE, curYeedStakeOfToken.add(yeedAmount));

            BigDecimal yeedAmountDecimal = new BigDecimal(yeedAmount);
            BigInteger tokenAmount =
                    yeedAmountDecimal.multiply(exRate).setScale(0, RoundingMode.HALF_EVEN).toBigInteger();

            BigInteger curTokenBalance = getBalance(tokenId, issuer);
            putBalance(tokenId, issuer, curTokenBalance.add(tokenAmount));

            BigInteger curTokenTotalSupply = getBalance(tokenId, TOTAL_SUPPLY);
            putBalance(tokenId, TOTAL_SUPPLY, curTokenTotalSupply.add(tokenAmount));

            String msg = String.format(
                    "Token [%s] was exchanged from YEED successfully. YEED %s was exchanged to token %s.",
                    tokenId, yeedAmount, tokenAmount
            );
            setSuccessTxReceipt(msg);

            return txReceipt;
        }

        // TODO : @kevin : 2019-09-09 : check if YEED stake transfer between tokens needed
        // 현재 로직은 YEED 교환이 허용되지 않은 토큰 간에만 T2T 교환을 허용할 수 밖에 없을 것으로 추정된다.
        // 전체 교환 로직을 만들기 위해서는 교환 시에 YEED stake 이동을 전제해야 한다.
        // 가능한 모든 교환 유형을 상정하여 도식화 하고, 각 유형의 교환 가능 여부와 환율을 체크해야 한다.
        /**
         * Opens the exchange from token A to token B
         *
         * @issuer the owner of token A
         * @param params the params
         *               @tokenId tokenId of token A
         *               @targetTokenId tokenId of token B
         *               @tokenExT2TRate Exchange rate (B = r * A)
         * @return the receipt
         */
        @InvokeTransaction
        public Receipt exchangeT2TOpen(JsonObject params) {
            String tokenId = params.get(TOKEN_ID).getAsString().toLowerCase();
            JsonObject token = loadTokenObject(tokenId);

            if (token == null) {
                setErrorTxReceipt(
                        String.format("Token [%s] does not exist!", tokenId));
                return txReceipt;
            }

            if (txReceipt.getIssuer().equals(token.get(TOKEN_OWNER_ACCOUNT).getAsString()) == false) {
                setErrorTxReceipt("Issuer must be token owner!");
                return txReceipt;
            }

            String targetTokenId = params.get(TOKEN_EX_T2T_TARGET_TOKEN_ID).getAsString().toLowerCase();
            JsonObject targetToken = loadTokenObject(targetTokenId);

            if (targetToken == null) {
                setErrorTxReceipt(
                        String.format("Target token [%s] does not exist!", targetTokenId));
                return txReceipt;
            }

            if (loadExT2TRate(tokenId, targetTokenId) != null) {
                setErrorTxReceipt("Exchange to target token is already open!");
                return txReceipt;
            }

            JsonElement exRateT2TObj = params.get(TOKEN_EX_T2T_RATE);
            if (exRateT2TObj == null) {
                setErrorTxReceipt("Exchange rate is required!");
                return txReceipt;
            }

            BigDecimal exRateT2T = exRateT2TObj.getAsBigDecimal();
            if (exRateT2T.compareTo(BigDecimal.ZERO) <= 0) {
                setErrorTxReceipt("Exchange rate should be greater than ZERO!");
                return txReceipt;
            }

            if (getYeedBalanceOfSub(tokenId).compareTo(DEFAULT_SERVICE_FEE) < 0) {
                setErrorTxReceipt("Insufficient yeed stake of the token for service fee!");
                return txReceipt;
            }
            if (burnServiceFeeFromYeedStake(tokenId, DEFAULT_SERVICE_FEE) == false) {
                setErrorTxReceipt("Insufficient service fee in yeed stake!");
                return txReceipt;
            }

            saveExT2TRate(tokenId, targetTokenId, exRateT2T);

            String msg = String.format(
                    "Token [%s] exchange open to target token %s completed successfully.",
                    tokenId, targetTokenId
            );
            setSuccessTxReceipt(msg);

            return txReceipt;
        }

        /**
         * Close exchange from a token to another token
         *
         * @param params the params
         *               @tokenId
         *               @targetTokenId Exchange target token
         * @return the receipt
         */
        @InvokeTransaction
        public Receipt exchangeT2TClose(JsonObject params) {
            String tokenId = params.get(TOKEN_ID).getAsString().toLowerCase();
            JsonObject token = loadTokenObject(tokenId);

            if (token == null) {
                setErrorTxReceipt(
                        String.format("Token [%s] does not exist!", tokenId));
                return txReceipt;
            }

            if (txReceipt.getIssuer().equals(token.get(TOKEN_OWNER_ACCOUNT).getAsString()) == false) {
                setErrorTxReceipt("Issuer must be token owner!");
                return txReceipt;
            }

            String targetTokenId = params.get(TOKEN_EX_T2T_TARGET_TOKEN_ID).getAsString().toLowerCase();
            JsonObject targetToken = loadTokenObject(targetTokenId);

            if (targetToken == null) {
                setErrorTxReceipt(
                        String.format("Target token [%s] does not exist!", targetTokenId));
                return txReceipt;
            }

            if (loadExT2TRate(tokenId, targetTokenId) == null) {
                setErrorTxReceipt("Target token is already closed!");
                return txReceipt;
            }

            if (getYeedBalanceOfSub(tokenId).compareTo(DEFAULT_SERVICE_FEE) < 0) {
                setErrorTxReceipt("Insufficient yeed stake of the token for service fee!");
                return txReceipt;
            }
            if (burnServiceFeeFromYeedStake(tokenId, DEFAULT_SERVICE_FEE) == false) {
                setErrorTxReceipt("Insufficient service fee in yeed stake!");
                return txReceipt;
            }

            saveExT2TRate(tokenId, targetTokenId, null);

            String msg = String.format(
                    "Token [%s] exchange close to target token %s completed successfully.",
                    tokenId, targetTokenId
            );
            setSuccessTxReceipt(msg);

            return txReceipt;
        }

        /**
         * Do exchange from a token to another token
         *
         * @issuer an account
         * @param params the params
         *               @tokenId
         *               @tokenExT2TTargetTokenId Exchange target token
         *               @amount Amount of tokens to exchange
         * @return the receipt
         */
        @InvokeTransaction
        public Receipt exchangeT2T(JsonObject params) {
            String[] requiredParamKeys = {TOKEN_ID, TOKEN_EX_T2T_TARGET_TOKEN_ID, AMOUNT};
            String checkParamsResult = checkParams(params, requiredParamKeys);
            if (checkParamsResult != null) {
                setErrorTxReceipt(
                        String.format("Param [%s] does not exist!", checkParamsResult));
                return txReceipt;
            }

            String tokenId = params.get(TOKEN_ID).getAsString().toLowerCase();

            JsonObject token = loadTokenObject(tokenId);

            if (token == null) {
                setErrorTxReceipt(
                        String.format("Token [%s] does not exist!", tokenId));
                return txReceipt;
            }

            if (isTokenRunning(tokenId) == false) {
                setErrorTxReceipt("Token is not running!");
                return txReceipt;
            }

            String targetTokenId = params.get(TOKEN_EX_T2T_TARGET_TOKEN_ID).getAsString().toLowerCase();
            BigDecimal exT2TRate = loadExT2TRate(tokenId, targetTokenId);
            if (exT2TRate == null) {
                setErrorTxReceipt("Token exchange is not opened to target token!");
                return txReceipt;
            }

            if (isTokenRunning(targetTokenId) == false) {
                setErrorTxReceipt("Target token is not running!");
                return txReceipt;
            }

            String issuer = txReceipt.getIssuer();
            BigInteger issuerTokenBalance = getBalance(tokenId, issuer);
            BigInteger tokenAmount = params.get(AMOUNT).getAsBigInteger();

            if (tokenAmount.compareTo(BigInteger.ZERO) <= 0) {
                setErrorTxReceipt("Exchange amount must be greater than ZERO!");
                return txReceipt;
            }

            if (issuerTokenBalance.compareTo(tokenAmount) < 0) {
                setErrorTxReceipt("Insufficient balance to exchange!");
                return txReceipt;
            }

            if (getYeedBalanceOfSub(tokenId).compareTo(DEFAULT_SERVICE_FEE) < 0) {
                setErrorTxReceipt("Insufficient yeed stake of the token for service fee!");
                return txReceipt;
            }
            if (burnServiceFeeFromYeedStake(tokenId, DEFAULT_SERVICE_FEE) == false) {
                setErrorTxReceipt("Insufficient service fee in yeed stake!");
                return txReceipt;
            }

            // targetTokenAmount = tokenAmount * exT2TRate
            BigDecimal tokenAmountDecimal = new BigDecimal(tokenAmount);
            BigInteger targetTokenAmount =
                    tokenAmountDecimal.multiply(exT2TRate)
                    .setScale(0, RoundingMode.HALF_EVEN).toBigInteger();

            // do exchange
            BigInteger curTokenBalance = getBalance(tokenId, issuer);
            putBalance(tokenId, issuer, curTokenBalance.subtract(tokenAmount));
            BigInteger curTotalSupplyOfToken = getBalance(tokenId, TOTAL_SUPPLY);
            putBalance(tokenId, TOTAL_SUPPLY, curTotalSupplyOfToken.subtract(tokenAmount));

            BigInteger curTargetTokenBalance = getBalance(targetTokenId, issuer);
            putBalance(targetTokenId, issuer, curTargetTokenBalance.add(targetTokenAmount));
            BigInteger curTargetTokenTotalSupply = getBalance(targetTokenId, TOTAL_SUPPLY);
            putBalance(targetTokenId, TOTAL_SUPPLY, curTargetTokenTotalSupply.add(targetTokenAmount));

            String msg = String.format(
                    "Token [%s] was exchanged to target token %s successfully. "
                    + "Token %s was exchanged to target token %s.",
                    tokenId, targetTokenId, tokenAmount, targetTokenAmount
            );
            setSuccessTxReceipt(msg);

            return txReceipt;
        }


        private String checkParams(JsonObject params, String[] requiredParamKeys) {
            for (String key:requiredParamKeys) {
                JsonElement e = params.get(key);
                if (e == null || e.isJsonNull()) {
                    return key;
                }
            }

            return null;
        }

        private void saveTokenPhase(String tokenId, String tokenPhase) {
            String key = TOKEN_PHASE_PREFIX.concat(tokenId);
            JsonObject storeValue = new JsonObject();
            storeValue.addProperty(TOKEN_PHASE, tokenPhase);
            store.put(key, storeValue);
        }

        private String loadTokenPhase(String tokenId) {
            String key = TOKEN_PHASE_PREFIX.concat(tokenId);
            JsonObject storeValue = store.get(key);
            return storeValue != null && storeValue.has(TOKEN_PHASE)
                    ? storeValue.get(TOKEN_PHASE).getAsString() : null;
        }

        private BigInteger getBalance(String tokenId, String address) {
            String targetAddress =
                    PrefixKeyEnum.ACCOUNT.toValue().concat(getTokenAddress(tokenId, address));
            JsonObject storeValue = store.get(targetAddress);
            return storeValue != null && storeValue.has(BALANCE)
                    ? storeValue.get(BALANCE).getAsBigInteger() : BigInteger.ZERO;
        }

        private void putBalance(String tokenId, String address, BigInteger value) {
            String targetAddress =
                    PrefixKeyEnum.ACCOUNT.toValue().concat(getTokenAddress(tokenId, address));
            JsonObject storeValue = new JsonObject();
            storeValue.addProperty(BALANCE, value);
            store.put(targetAddress, storeValue);
        }

        private String approveKey(String sender, String spender) {
            byte[] approveKeyByteArray = ByteUtil.merge(sender.getBytes(), spender.getBytes());
            byte[] approveKey = HashUtil.sha3(approveKeyByteArray);
            return PrefixKeyEnum.APPROVE.toValue().concat(HexUtil.toHexString(approveKey));
        }

        private void setErrorTxReceipt(String msg) {
            this.txReceipt.setStatus(ExecuteStatus.ERROR);
            this.txReceipt.addLog(msg);
        }

        private void setSuccessTxReceipt(String msg) {
            this.txReceipt.setStatus(ExecuteStatus.SUCCESS);
            this.txReceipt.addLog(msg);
        }

        private JsonObject loadTokenObject(String tokenId) {
            return store.get(TOKEN_PREFIX.concat(tokenId));
        }

        private String getTokenAddress(String tokenId, String address) {
            return tokenId.concat("-").concat(address);
        }

        private boolean depositYeedStakeSub(String from, BigInteger amount) {
            JsonObject param = new JsonObject();
            param.addProperty("from", from);
            param.addProperty("to", TOKEN_CONTRACT_NAME);
            param.addProperty("amount", amount);
            param.addProperty(SERVICE_FEE, DEFAULT_SERVICE_FEE);

            String yeedContractVersion = this.branchStateStore.getContractVersion("YEED");
            log.debug("YEED Contract {}", yeedContractVersion);

            String methodName = txReceipt.getIssuer().equals(from) ? "transferChannel" : "transferFromChannel";
            JsonObject result = this.channel.call(
                    yeedContractVersion, ContractMethodType.CHANNEL_METHOD, methodName, param);

            return result.get("result").getAsBoolean();
        }

        private boolean withdrawYeedStakeSub(String to, BigInteger amount) {
            JsonObject param = new JsonObject();
            param.addProperty("from", TOKEN_CONTRACT_NAME);
            param.addProperty("to", to);
            param.addProperty(AMOUNT, amount);
            param.addProperty(SERVICE_FEE, DEFAULT_SERVICE_FEE);

            String yeedContractVersion = this.branchStateStore.getContractVersion("YEED");
            log.debug("YEED Contract {}", yeedContractVersion);
            JsonObject result = this.channel.call(
                    yeedContractVersion, ContractMethodType.CHANNEL_METHOD, "transferChannel", param);

            return result.get("result").getAsBoolean();
        }

        private boolean burnServiceFeeFromYeedStake(String tokenId, BigInteger amount) {
            JsonObject param = new JsonObject();
            param.addProperty(SERVICE_FEE, amount);

            String yeedContractVersion = this.branchStateStore.getContractVersion("YEED");
            log.debug("YEED Contract {}", yeedContractVersion);
            JsonObject result = this.channel.call(
                    yeedContractVersion, ContractMethodType.CHANNEL_METHOD, "transferFeeChannel", param);

            if (result.get("result").getAsBoolean()) {
                BigInteger curYeedBalance = getYeedBalanceOfSub(tokenId);
                setYeedBalanceOfSub(tokenId, curYeedBalance.subtract(amount));
            }

            return result.get("result").getAsBoolean();
        }


        private boolean isTokenRunning(String tokenId) {
            return TOKEN_PHASE_RUN.equals(loadTokenPhase(tokenId));
        }


        private void saveExT2TRate(String tokenId, String targetTokenId, BigDecimal rate) {
            String key = TOKEN_EX_T2T_RATE_MAP_PREFIX.concat(tokenId);
            JsonObject exRateT2TMap = store.get(key);

            if (exRateT2TMap == null) {
                exRateT2TMap = new JsonObject();
            }

            String t2tName = tokenId.concat("-").concat(targetTokenId);

            if (rate == null) {
                exRateT2TMap.remove(t2tName);
            } else {
                exRateT2TMap.addProperty(t2tName, rate);
            }

            store.put(key, exRateT2TMap);
        }

        private BigDecimal loadExT2TRate(String tokenId, String targetTokenId) {
            String key = TOKEN_EX_T2T_RATE_MAP_PREFIX.concat(tokenId);
            JsonObject exRateT2TMap = store.get(key);

            if (exRateT2TMap == null) {
                return null;
            }

            String t2tName = tokenId.concat("-").concat(targetTokenId);

            JsonElement exRateT2TElement = exRateT2TMap.get(t2tName);
            if (exRateT2TElement == null) {
                return null;
            }

            return exRateT2TElement.getAsBigDecimal();
        }
    }
}
