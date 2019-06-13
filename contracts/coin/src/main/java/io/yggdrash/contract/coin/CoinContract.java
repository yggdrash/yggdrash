package io.yggdrash.contract.coin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.yggdrash.common.contract.standard.CoinStandard;
import io.yggdrash.common.contract.vo.PrefixKeyEnum;
import io.yggdrash.common.contract.vo.dpoa.Validator;
import io.yggdrash.common.contract.vo.dpoa.ValidatorSet;
import io.yggdrash.common.crypto.HashUtil;
import io.yggdrash.common.utils.ByteUtil;
import io.yggdrash.common.utils.JsonUtil;
import io.yggdrash.contract.core.ExecuteStatus;
import io.yggdrash.contract.core.TransactionReceipt;
import io.yggdrash.contract.core.annotation.ContractQuery;
import io.yggdrash.contract.core.annotation.ContractStateStore;
import io.yggdrash.contract.core.annotation.ContractTransactionReceipt;
import io.yggdrash.contract.core.annotation.Genesis;
import io.yggdrash.contract.core.annotation.InvokeTransaction;
import io.yggdrash.contract.core.annotation.ParamValidation;
import io.yggdrash.contract.core.exception.ContractException;
import io.yggdrash.contract.core.store.ReadWriterStore;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.Hashtable;
import java.util.Map;

public class CoinContract implements BundleActivator, ServiceListener {
    private static final Logger log = LoggerFactory.getLogger(CoinContract.class);
    private static final String AMOUNT = "amount";
    private static final String BALANCE = "balance";

    @Override
    public void start(BundleContext context) {
        log.info("⚪ Start coin contract");
        //Find for service in another bundle
        Hashtable<String, String> props = new Hashtable<>();
        props.put("YGGDRASH", "Coin");
        context.registerService(CoinService.class.getName(), new CoinService(), props);
    }

    @Override
    public void stop(BundleContext context) {
        log.info("⚫ Stop coin contract");
    }

    @Override
    public void serviceChanged(ServiceEvent event) {
        log.info("serviceChanged called");
    }

    public static class CoinService implements CoinStandard {
        private static final String TOTAL_SUPPLY = "TOTAL_SUPPLY";

        @ContractTransactionReceipt
        TransactionReceipt txReceipt;

        @ContractStateStore
        ReadWriterStore<String, JsonObject> store;

        /**
         * @return Total amount of coin in existence
         */
        @ContractQuery
        @ParamValidation
        @Override
        public BigInteger totalSupply() {
            log.debug("\ntotalSupply :: param => ");
            return getBalance(TOTAL_SUPPLY);
        }

        /**
         * Gets the balance of the specified address
         * params owner   The address to query the balance of
         *
         * @return A BigInteger representing the amount owned by the passed address
         */
        @ContractQuery
        @ParamValidation
        @Override
        public BigInteger balanceOf(JsonObject params) {
            log.debug("\nbalanceOf :: params => {}", params);

            String address = params.get("address").getAsString().toLowerCase();
            if (store.get(address) != null) {
                return getBalance(address);
            }
            return BigInteger.ZERO;
        }

        /**
         * Function to check the amount of coin that an owner allowed to a spender
         * params owner    The address which owns the funds.
         * params spender  The address which will spend the funds
         *
         * @return A BigInteger specifying the amount of coin still available for the spender
         */
        @ContractQuery
        @ParamValidation
        @Override
        public BigInteger allowance(JsonObject params) {
            log.debug("\nallowance :: params => {}", params);

            String owner = params.get("owner").getAsString().toLowerCase();
            String spender = params.get("spender").getAsString().toLowerCase();
            String approveKey = approveKey(owner, spender);

            if (store.get(owner) != null && store.get(approveKey) != null) {
                return getBalance(approveKey);
            }
            return BigInteger.ZERO;
        }

        /**
         * Transfer token for a specified address
         * params to      The address to transfer to
         * params amount  The amount to be transferred
         *
         * @return TransactionReceipt
         */
        @InvokeTransaction
        @ParamValidation
        @Override
        public TransactionReceipt transfer(JsonObject params) {
            log.debug("\ntransfer :: params => {}", params);

            String to = getAsString(params, "to").toLowerCase();
            BigInteger amount = getAsBigInteger(params, AMOUNT);

            String sender = this.txReceipt.getIssuer();
            BigInteger senderBalance = getBalance(sender);
            log.debug("sender : {}", senderBalance);

            if (require(senderBalance, amount)) {
                senderBalance = senderBalance.subtract(amount);
                addBalanceTo(to, amount);
                putBalance(sender, senderBalance);
                txReceipt.setStatus(ExecuteStatus.SUCCESS);
                txReceipt.addLog(String.format("[Transfer] Transferred %d to %s from %s", amount, to, sender));
                Transfer(amount, sender, to);
            }

            return txReceipt;
        }

        /**
         * Approve the passed address to spend the specified amount of tokens on behalf of tx.sender
         * params spender  The address which will spend the funds
         * params amount   The amount of tokens to be spent
         *
         * @return TransactionReceipt
         */
        @InvokeTransaction
        @ParamValidation
        @Override
        public TransactionReceipt approve(JsonObject params) {
            log.debug("\napprove :: params => {}", params);

            String spender = getAsString(params, "spender").toLowerCase();
            BigInteger amount = getAsBigInteger(params, AMOUNT);

            String sender = txReceipt.getIssuer();
            BigInteger senderBalance = getBalance(sender);
            log.debug("sender : {}", senderBalance);

            if (require(senderBalance, amount)) {
                String approveKey = approveKey(sender, spender);
                putBalance(approveKey, amount);
                log.debug("approve Key : {}", approveKey);
                txReceipt.setStatus(ExecuteStatus.SUCCESS);
                txReceipt.addLog(
                        String.format("[Approved] Approve %d to %s from %s", getBalance(approveKey), spender, sender));
                Approve(approveKey, spender, sender);
            }

            return txReceipt;
        }

        /**
         * Transfer tokens from one address to another
         * params from    The address which you want to send tokens from
         * params to      The address which you want to transfer to
         * params amount  The amount of tokens to be transferred
         *
         * @return TransactionReceipt
         */
        @InvokeTransaction
        @ParamValidation
        @Override
        public TransactionReceipt transferFrom(JsonObject params) {
            log.debug("\ntransferFrom :: params => {}", params);

            String from = getAsString(params, "from").toLowerCase();
            String to = getAsString(params, "to").toLowerCase();
            BigInteger amount = getAsBigInteger(params, AMOUNT);

            String sender = txReceipt.getIssuer();
            String approveKey = approveKey(from, sender);
            log.debug("approve Key : {}", approveKey);

            BigInteger approveBalance = getBalance(approveKey);
            BigInteger fromBalance = getBalance(from);

            if (require(fromBalance, amount) && require(approveBalance, amount)) {
                fromBalance = fromBalance.subtract(amount);
                approveBalance = approveBalance.subtract(amount);
                addBalanceTo(to, amount);
                putBalance(from, fromBalance);
                putBalance(approveKey, approveBalance);
                txReceipt.setStatus(ExecuteStatus.SUCCESS);
                txReceipt.addLog(String.format("[Transferred] Transfer %d from %s to %s", amount, from, to));
                TransferFrom(amount, from, to, sender, approveBalance);
            }

            return txReceipt;
        }

        /**
         * Pre-allocate yeed to addresses
         * params frontier The Frontier is the first live release of the Yggdrash network
         * params balance  The balance of frontier
         *
         * @return TransactionReceipt
         */
        @Genesis
        @InvokeTransaction
        public TransactionReceipt init(JsonObject params) {
            log.debug("\ngenesis :: params => {}", params);

            //totalSupply 는 alloc 의 balance 를 모두 더한 값으로 세팅
            BigInteger totalSupply = BigInteger.ZERO;
            JsonObject alloc = getAsJsonObject(params, "alloc");
            for (Map.Entry<String, JsonElement> entry : alloc.entrySet()) {
                String frontier = entry.getKey();
                JsonObject value = entry.getValue().getAsJsonObject();
                BigInteger balance = getAsBigInteger(value, BALANCE);

                totalSupply = totalSupply.add(balance);
                addBalanceTo(frontier, balance);
                putBalance(frontier, balance);

                JsonObject mintLog = new JsonObject();
                mintLog.addProperty("to", frontier);
                mintLog.addProperty(BALANCE, balance.toString());
                txReceipt.addLog(mintLog.toString());
                Init(frontier);
            }
            // TODO Validator will call by contract channel
            // boolean isSuccess = saveInitValidator(params.getAsJsonArray("validator"));
            boolean isSuccess = true;

            // FIXME convert to Json or something
            try {
                putBalance(TOTAL_SUPPLY, totalSupply);
            } catch (Exception e) {
                isSuccess = false;
                log.warn(e.getMessage());
            }
            txReceipt.setStatus(isSuccess ? ExecuteStatus.SUCCESS : ExecuteStatus.FALSE);
            JsonObject totalSupplyLog = new JsonObject();
            totalSupplyLog.addProperty("totalSupply", totalSupply.toString());
            txReceipt.addLog(totalSupplyLog.toString());

            return txReceipt;
        }

        private void addBalanceTo(String to, BigInteger amount) {
            BigInteger balance = getBalance(to);
            putBalance(to, balance.add(amount));
        }

        public void saveInitValidator(JsonArray validators) {
            ValidatorSet validatorSet = JsonUtil.generateJsonToClass(
                    store.get(PrefixKeyEnum.VALIDATORS.toValue()).toString(), ValidatorSet.class);
            if (validatorSet != null || validators == null) {
                return;
            }

            validatorSet = new ValidatorSet();
            for (JsonElement validator : validators) {
                validatorSet.getValidatorMap().put(validator.getAsString(), new Validator(validator.getAsString()));
            }
            JsonObject jsonObject = JsonUtil.parseJsonObject(JsonUtil.convertObjToString(validatorSet));
            store.put(PrefixKeyEnum.VALIDATORS.toValue(), jsonObject);
            return;
        }

        private BigInteger getBalance(String key) {
            JsonObject storeValue = store.get(key);
            if (storeValue != null && storeValue.has(BALANCE)) {
                return storeValue.get(BALANCE).getAsBigInteger();
            } else {
                return BigInteger.ZERO;
            }
        }

        private void putBalance(String key, BigInteger value) {
            JsonObject storeValue = new JsonObject();
            storeValue.addProperty(BALANCE, value);
            store.put(key, storeValue);
        }

        private String approveKey(String sender, String spender) {
            byte[] approveKeyByteArray = ByteUtil.merge(sender.getBytes(), spender.getBytes());
            byte[] approveKey = HashUtil.sha3(approveKeyByteArray);
            return Hex.toHexString(approveKey);
        }

        private boolean require(BigInteger targetBalance, BigInteger amount) throws ContractException {
            if (targetBalance.subtract(amount).compareTo(BigInteger.ZERO) >= 0) { // same is  0, more is 1
                return true;
            } else {
                txReceipt.setStatus(ExecuteStatus.ERROR);
                throw new ContractException(Err.INSUFFICIENT_FUNDS.toValue(), Err.INSUFFICIENT_FUNDS.toString());
            }
        }

        private String getAsString(JsonObject params, String prop) throws ContractException {
            if (params.has(prop)) {
                return params.get(prop).getAsString();
            } else {
                txReceipt.setStatus(ExecuteStatus.ERROR);
                throw new ContractException(Err.INVALID_PARAMS.toValue(), Err.INVALID_PARAMS.toString());
            }
        }

        private BigInteger getAsBigInteger(JsonObject params, String prop) throws ContractException {
            if (params.has(prop)) {
                return params.get(prop).getAsBigInteger();
            } else {
                txReceipt.setStatus(ExecuteStatus.ERROR);
                throw new ContractException(Err.INVALID_PARAMS.toValue(), Err.INVALID_PARAMS.toString());
            }
        }

        private JsonObject getAsJsonObject(JsonObject params, String prop) throws ContractException {
            if (params.has(prop)) {
                return params.getAsJsonObject(prop);
            } else {
                txReceipt.setStatus(ExecuteStatus.ERROR);
                throw new ContractException(Err.INVALID_PARAMS.toValue(), Err.INVALID_PARAMS.toString());
            }
        }

        private void Transfer(BigInteger amount, String sender, String to) {
            log.debug("\n[Transferred] Transfer {} from {} to {}", amount, sender, to);
            log.debug("\nBalance of From ({}) : {}"
                    + "\nBalance of To   ({}) : {}", sender, getBalance(sender), to, getBalance(to));
        }

        private void Approve(String approveKey, String spender, String sender) {
            log.debug("\n[Approved] Approve {} to {} from {}", getBalance(approveKey), spender, sender);
        }

        private void TransferFrom(BigInteger amount, String from, String to, String sender, BigInteger approveBalance) {
            log.debug("\n[Transferred] Transfer {} from {} to {}", amount, from, to);
            log.debug("\nAllowed amount of Sender ({}) : {}", sender, approveBalance);
            log.debug("\nBalance of From ({}) : {}"
                    + "\nBalance of To   ({}) : {}", from, getBalance(from), to, getBalance(to));
        }

        private void Init(String frontier) {
            log.debug("\nAddress of Frontier : {}"
                    + "\nBalance of Frontier : {}", frontier, getBalance(frontier));
        }

        private enum Err {
            //The errors are appended to the transactionReceipt.
            INVALID_PARAMS(34001),          //1000010011010001
            INSUFFICIENT_FUNDS(34002),      //1000010011010010
            EXECUTION_FAILED(34004)         //1000010011010100
            ;

            private int code;

            Err(int code) {
                this.code = code;
            }

            public int toValue() {
                return code;
            }

            public String toString() {
                if (code == INVALID_PARAMS.code) {
                    return "Params not allowed";
                }
                if (code == INSUFFICIENT_FUNDS.code) {
                    return "Insufficient funds";
                }
                if (code == EXECUTION_FAILED.code) {
                    return "Execution failed";
                }
                return "";
            }
        }
    }
}
