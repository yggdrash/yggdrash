/*
 * Copyright 2019 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.contract.yeed;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.yggdrash.common.contract.standard.CoinStandard;
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
import io.yggdrash.contract.core.annotation.ContractEndBlock;
import io.yggdrash.contract.core.annotation.ContractQuery;
import io.yggdrash.contract.core.annotation.ContractReceipt;
import io.yggdrash.contract.core.annotation.ContractStateStore;
import io.yggdrash.contract.core.annotation.Genesis;
import io.yggdrash.contract.core.annotation.InvokeTransaction;
import io.yggdrash.contract.core.annotation.ParamValidation;
import io.yggdrash.contract.core.channel.ContractChannel;
import io.yggdrash.contract.core.store.ReadWriterStore;
import io.yggdrash.contract.yeed.ehtereum.EthTokenTransaction;
import io.yggdrash.contract.yeed.ehtereum.EthTransaction;
import io.yggdrash.contract.yeed.intertransfer.TxConfirm;
import io.yggdrash.contract.yeed.intertransfer.TxConfirmStatus;
import io.yggdrash.contract.yeed.propose.ProcessTransaction;
import io.yggdrash.contract.yeed.propose.ProposeErrorCode;
import io.yggdrash.contract.yeed.propose.ProposeInterChain;
import io.yggdrash.contract.yeed.propose.ProposeStatus;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public class YeedContract implements BundleActivator, ServiceListener {

    // Param properties
    private static final String TOTAL_SUPPLY = "TOTAL_SUPPLY";
    private static final String ALLOC = "alloc";
    private static final String ADDRESS = "address";
    private static final String AMOUNT = "amount";
    private static final String ISSUER = "issuer";
    private static final String BALANCE = "balance";
    private static final String RAW_TX = "rawTransaction";
    private static final String TX_ID = "transactionId";
    private static final String TX_CONFIRM_ID = "txConfirmId";
    private static final String FEE = "fee";
    private static final String SERVICE_FEE = "serviceFee";
    private static final String STAKE_YEED = "stakeYeed";
    private static final String OWNER = "owner";
    private static final String SPENDER = "spender";
    private static final String STATUS = "status";
    private static final String TO = "to";
    private static final String FROM = "from";
    private static final String PROPOSE_ID = "proposeId";
    private static final String BLOCK_HEIGHT = "blockHeight";
    private static final String INDEX = "index";
    private static final String LAST_BLOCK_HEIGHT = "lastBlockHeight";
    private static final String CONTRACT_NAME = "contractName";

    // Log messages
    private static final String INSUFFICIENT_FUNDS = "Insufficient funds";
    private static final String INVALID_PARAMS = "Invalid parameters";
    private static final String LOW_TX_FEE = "Low transaction fee";
    private static final String INVALID_AMOUNT = "Invalid amount";
    private static final String INVALID_AMOUNT_OF_STAKE_YEED = "Invalid amount of stakeYeed";
    private static final String INIT_SUCCESS = "Initialization completed successfully. Total Supply is %s";
    private static final String INIT_FAIL = "Initialization failed";
    private static final String APPROVE_SUCCESS = "Approve %s to %s from %s";
    private static final String FEE_TRANSFER_FAIL = "Fee transfer failed";
    private static final String TRANSFER_SUCCESS = "Transfer %s from %s to %s fee %s";
    private static final String TRANSFER_FAIL = "Transfer failed";
    private static final String TRANSFER_FEE_SUCCESS = "Transfer fee %s from %s success";
    private static final String TRANSFER_FEE_FAIL = "Transfer fee %s from %s failed";
    private static final String BALANCE_EMPTY = "Balance empty";
    private static final String TRANSFER_FROM_SUCCESS = "TransferFrom %s from %s to %s fee %s by %s";
    private static final String TRANSFER_CHANNEL_FAILED = "Transfer channel failed";
    private static final String BURN_YEED = "Burn %s Yeed";
    private static final String PROPOSAL_NOT_EXPIRED = "The proposal has not expired";
    private static final String PROPOSAL_ALREADY_CLOSED = "The proposal already CLOSED";
    private static final String TX_ISSUER_IS_NOT_PROPOSAL_ISSUER = "Transaction issuer is not the proposal issuer";
    private static final String TX_CONFIRM_REQUIRE_VALIDATOR = "Transaction confirm is required for validator";
    private static final String TX_CONFIRM_FAIL = "%s is FAIL";
    private static final String TX_CONFIRM_DONE = "%s is DONE";
    private static final String TX_CONFIRM_EXIST = "Propose %s transaction %s exist";
    private static final String TX_CONFIRM_PROCESS_SUCCESS = "Propose %s check %s network %s transaction %s confirm ID %s";
    private static final String PROCESS_YEED_TO_ETH_TOKEN_SUCCESS = "Yeed to EthToken Proposal completed successfully";
    private static final String PROCESS_YEED_TO_ETH_SUCCESS = "Yeed to Eth Proposal completed successfully";
    private static final String PROCESS_PROPOSE_FAIL = "Propose cannot proceed (ProposeStatus=%s)";
    private static final String PROPOSE_NOT_EXISTS = "Propose not exists";
    private static final String PROPOSE_STATUS_NOT_EXISTS = "Propose status not exists";

    private static final Logger log = LoggerFactory.getLogger(YeedContract.class);

    @Override
    public void start(BundleContext context) {
        log.info("Start YEED contract");
        // Find for service in another bundle
        Hashtable<String, String> props = new Hashtable<>();
        props.put("YGGDRASH", "YEED");
        context.registerService(YeedService.class.getName(), new YeedService(), props);
        // GetBundle and wire this service
    }

    @Override
    public void stop(BundleContext context) {
        log.info("Stop Yeed contract");
    }

    @Override
    public void serviceChanged(ServiceEvent event) {
        log.info("serviceChanged called");
    }

    public static class YeedService implements CoinStandard {

        // TODO Base Fee is Network Fee - Governance Change this value
        // BASE FEE = 5*10^13
        private static BigInteger BASE_FEE = BigInteger.valueOf(50000000000000L);
        // 0.1 YEED = 100000000000000000
        private static BigInteger BASE_CURRENCY = BigInteger.TEN.pow(18);

        @ContractReceipt
        Receipt receipt;

        @ContractStateStore
        ReadWriterStore<String, JsonObject> store;

        @ContractBranchStateStore
        BranchStateStore branchStateStore;

        @ContractChannelField
        public ContractChannel channel;

        /**
         * @return Total amount of coin in existence
         */
        @ContractQuery
        @Override
        public BigInteger totalSupply() {
            return getBalance(TOTAL_SUPPLY);
        }

        /**
         * Gets the balance of the specified addressgit
         * params owner   The address to query the balance of
         *
         * @return A BigInteger representing the amount owned by the passed address
         */
        @ContractQuery
        @Override
        public BigInteger balanceOf(JsonObject params) {
            try {
                String address = params.get(ADDRESS).getAsString().toLowerCase();
                return getBalance(address);
            } catch (Exception e) {
                return BigInteger.ZERO;
            }
        }

        /**
         * Gets the balance of the contract by name of it
         * params contract  The name of contract
         *
         * @return A BigInteger representing the amount the contract is staking
         */
        @ContractQuery
        @ContractChannelMethod
        public BigInteger getContractBalanceOf(JsonObject params) {
            try {
                String contractName = params.get(CONTRACT_NAME).getAsString();
                return getBalance(contractAccountKey(contractName));
            } catch (Exception e) {
                return BigInteger.ZERO;
            }
        }

        /**
         * Function to check the amount of coin that an owner allowed to a spender
         * params owner    The address which owns the funds.
         * params spender  The address which will spend the funds
         *
         * @return A BigInteger specifying the amount of coin still available for the spender
         */
        @ContractQuery
        @Override
        public BigInteger allowance(JsonObject params) {
            try {
                String owner = params.get(OWNER).getAsString().toLowerCase();
                String spender = params.get(SPENDER).getAsString().toLowerCase();
                String approveKey = approveKey(owner, spender);
                return getBalance(approveKey);
            } catch (Exception e) {
                return BigInteger.ZERO;
            }
        }

        /**
         * Pre-allocate yeed to addresses
         * params frontier The Frontier is the first live release of the Yggdrash network
         * params balance  The balance of frontier
         *
         * @return Receipt
         */
        @Genesis
        @InvokeTransaction
        public Receipt init(JsonObject params) {
            // Set totalSupply to the sum of all balances of alloc
            BigInteger totalSupply = BigInteger.ZERO;
            try {
                JsonObject alloc = params.getAsJsonObject(ALLOC);
                for (Map.Entry<String, JsonElement> entry : alloc.entrySet()) {
                    String frontier;
                    BigInteger balance;
                    try {
                        frontier = entry.getKey();
                        JsonObject value = entry.getValue().getAsJsonObject();
                        balance = value.get(BALANCE).getAsBigInteger();
                    } catch (Exception e) {
                       setErrorTxReceipt(INVALID_PARAMS);
                       return receipt;
                    }

                    // Apply BASE_CURRENCY
                    balance = balance.multiply(BASE_CURRENCY);
                    totalSupply = totalSupply.add(balance);
                    addBalanceTo(frontier, balance);

                    JsonObject mintLog = new JsonObject();
                    mintLog.addProperty(TO, frontier);
                    mintLog.addProperty(BALANCE, balance.toString());
                    receipt.addLog(mintLog.toString());
                    log.debug("Address of Frontier : {}"
                            + "Balance of Frontier : {}", frontier, getBalance(frontier));
                }
                putBalance(TOTAL_SUPPLY, totalSupply);

                setSuccessTxReceipt(String.format(INIT_SUCCESS, totalSupply));
            } catch (Exception e) {
                setErrorTxReceipt(e.getMessage());
                log.debug("Init Exception : {}", e.getMessage());
            }

            return receipt;
        }

        /**
         * Approve the passed address to spend the specified amount of tokens on behalf of tx.sender
         * params spender  The address which will spend the funds
         * params amount   The amount of tokens to be spent
         *
         * @return Receipt
         */
        @InvokeTransaction
        @Override
        public Receipt approve(JsonObject params) {
            String sender = receipt.getIssuer();
            if (checkBalanceEmpty(sender)) {
                return receipt;
            }

            try {
                BigInteger amount = params.get(AMOUNT).getAsBigInteger();
                String spender = params.get(SPENDER).getAsString().toLowerCase();
                BigInteger fee = params.has(FEE) ? params.get(FEE).getAsBigInteger() : calculateFee();

                try {
                    if (checkNetworkFee(fee) && checkAmount(amount) && transferFee(sender, fee)) {
                        String approveKey = approveKey(sender, spender);
                        putBalance(approveKey, amount);
                        setSuccessTxReceipt(String.format(APPROVE_SUCCESS, spender, getBalance(approveKey), sender));

                        if (log.isDebugEnabled()) {
                            log.debug("[Approved] Approve {} to {} from {}. ApproveKey : {}",
                                    spender, getBalance(approveKey), sender, approveKey);
                        }
                    }
                } catch (Exception e) {
                    setErrorTxReceipt(e.getMessage());
                    log.debug("Approve Exception : {}", e.getMessage());
                }

            } catch (Exception e) {
                setFalseTxReceipt(INVALID_PARAMS);
                log.debug("{} : {}",INVALID_PARAMS, e.getMessage());
                return receipt;
            }

            return receipt;
        }

        /**
         * Transfer token for a specified address
         * params to      The address to transfer to
         * params amount  The amount to be transferred
         *
         * @return Receipt
         */
        @InvokeTransaction
        @ParamValidation
        @Override
        public Receipt transfer(JsonObject params) {
            String from = this.receipt.getIssuer();
            if (checkBalanceEmpty(from)) {
                return receipt;
            }

            try {
                String to = params.get(TO).getAsString().toLowerCase();
                BigInteger amount = params.get(AMOUNT).getAsBigInteger();
                // Fee is optional. Instead, the calculated networkFee is paid as a fee.
                BigInteger fee = params.has(FEE) ? params.get(FEE).getAsBigInteger() : calculateFee();

                try {
                    if (checkNetworkFee(fee) && checkAmount(amount)) {
                        transfer(from, to, amount, fee);
                        if (log.isDebugEnabled()) {
                            log.debug("[Transferred] Transfer {} from {} to {}", amount, from, to);
                            log.trace("Balance of From ({}) : {} To ({}) : {}", from, getBalance(from), to, getBalance(to));
                        }
                    }
                } catch (Exception e) {
                    setErrorTxReceipt(e.getMessage());
                    log.debug("Transfer Exception : {}", e.getMessage());
                }

            } catch (Exception e) {
                setFalseTxReceipt(INVALID_PARAMS);
                log.debug("{} : {}",INVALID_PARAMS, e.getMessage());
                return receipt;
            }

            return receipt;
        }

        /**
         * Transfer From Account to To Account, Amount, Network Fee
         * @param from Account (Wallet, Deposit Account, ... )
         * @param to Account (Wallet, Deposit Account, ... )
         * @param amount BigInteger Asset value
         * @param fee Network Fee Amount
         * @return Transfer is Success or not
         */
        protected boolean transfer(String from, String to, BigInteger amount, BigInteger fee) {
            BigInteger fromBalance = getBalance(from);
            BigInteger feeAmount = amount.add(fee);

            // Check from account balance
            if (isTransferable(fromBalance, feeAmount)) {
                subtractBalanceFrom(from, feeAmount);
                addBalanceTo(to, amount);
                // Stores fee for each branchId to reward to the validators {branchId : fee}
                addBalanceTo(receipt.getBranchId(), fee);
                setSuccessTxReceipt(String.format(TRANSFER_SUCCESS, amount, from, to, fee));
                return true;
            } else {
                setErrorTxReceipt(INSUFFICIENT_FUNDS);
                if (amount.equals(BigInteger.ZERO)) {
                    setErrorTxReceipt(FEE_TRANSFER_FAIL);
                } else {
                    setErrorTxReceipt(TRANSFER_FAIL);
                }
                return false;
            }
        }

        /***
         * Check amount is Positive
         * Amount is more than Zero
         * @param amount Check Value
         * @return Value is more than Zero
         */
        private boolean isPositive(BigInteger amount) {
            return amount.compareTo(BigInteger.ZERO) > 0; // amount > 0
        }

        /***
         * Check Account is exist and Asset is Zero
         * @param account Check Account
         * @return Account's Value is Zero (not exist or asset is Zero)
         */
        private boolean isAccountEmpty(String account) { // pre verification
            return getBalance(account).compareTo(BigInteger.ZERO) == 0;
        }

        private boolean isTransferable(BigInteger targetBalance, BigInteger amount) {
            // less is -1, same is  0, more is 1
            return targetBalance.subtract(amount).compareTo(BigInteger.ZERO) >= 0;
        }

        @ContractChannelMethod
        public boolean isTransferable(JsonObject params) { // Verify insufficient funds
            String address = params.get(ADDRESS).getAsString();
            BigInteger fee = params.get(AMOUNT).getAsBigInteger();
            return isTransferable(getBalance(address), fee);
        }

        /**
         * Transfer tokens from one address to another
         * params from    The address which you want to send tokens from
         * params to      The address which you want to transfer to
         * params amount  The amount of tokens to be transferred
         *
         * @return Receipt
         */
        @InvokeTransaction
        @Override
        public Receipt transferFrom(JsonObject params) {
            try {
                String to = params.get(TO).getAsString().toLowerCase();
                String from = params.get(FROM).getAsString().toLowerCase();
                BigInteger amount = params.get(AMOUNT).getAsBigInteger();
                // Fee is optional. Instead, the calculated networkFee is paid as a fee.
                BigInteger fee = params.has(FEE) ? params.get(FEE).getAsBigInteger() : calculateFee();

                try {
                    final String sender = receipt.getIssuer();
                    final String approveKey = approveKey(from, sender);

                    BigInteger amountFee = amount.add(fee);
                    if (!checkBalanceEmpty(approveKey) && checkNetworkFee(fee) && checkAmount(amount)
                            && checkBalance(approveKey, amountFee) && transfer(from, to, amount, fee)) {
                        subtractBalanceFrom(approveKey, amountFee);
                        setSuccessTxReceipt(String.format(TRANSFER_FROM_SUCCESS, amount, from, to, fee, sender));
                        //approveValue = approveValue.subtract(amountFee);
                        //putBalance(approveKey, approveValue);
                        if (log.isDebugEnabled()) {
                            log.debug("[Transferred] Transfer {} from {} to {}", amount, from, to);
                            log.debug("Allowed amount of Sender ({}) : {}", sender, getBalance(approveKey));
                            log.debug("Balance of From ({}) : {} Balance of To   ({}) : {}", from, getBalance(from),
                                    to, amount);
                        }
                    }
                } catch (Exception e) {
                    setErrorTxReceipt(e.getMessage());
                    log.debug("TransferFrom Exception : {}", e.getMessage());
                }

            } catch (Exception e) {
                setFalseTxReceipt(INVALID_PARAMS);
                log.debug("{} : {}",INVALID_PARAMS, e.getMessage());
                return receipt;
            }
            return receipt;
        }

        /***
         * Other Contract Call to Channel method, YEED Transfer to other contract or account
         * @param params {
         *                  from: from Account (Contract Name or Wallet Account) ,
         *                  to: to Account (Contract Name or Wallet Account),
         *                  amount: Transfer Amount Value(BigInteger)
         *                  serviceFee: Network Service Fee(BigInteger)
         *               }
         * @return Transfer is Success or not
         */
        @ContractChannelMethod
        public boolean transferChannel(JsonObject params) {
            // Contract name base
            String otherContract = this.receipt.getContractVersion();
            String contractName = this.branchStateStore.getContractName(otherContract);
            String contractAccount = String.format("%s%s", PrefixKeyEnum.CONTRACT_ACCOUNT, contractName);
            try {
                String issuer = this.receipt.getIssuer();
                String fromAccount = params.get(FROM).getAsString();
                String toAccount = params.get(TO).getAsString();
                BigInteger amount = params.get(AMOUNT).getAsBigInteger();
                BigInteger serviceFee = params.has(SERVICE_FEE)
                        ? params.get(SERVICE_FEE).getAsBigInteger() : BigInteger.ZERO;

                try {
                    if (!checkAmount(amount)) {
                        return false;
                    }

                    if (toAccount.equalsIgnoreCase(contractName)
                            && fromAccount.equalsIgnoreCase(issuer)) {
                        // deposit
                        return transfer(fromAccount, contractAccount, amount, serviceFee);
                    } else if (fromAccount.equalsIgnoreCase(contractName)) { // withdraw
                        // withdraw service fee is used contract Account
                        return transfer(contractAccount, toAccount, amount, serviceFee);
                    }

                    setFalseTxReceipt(TRANSFER_CHANNEL_FAILED);
                    return false; // If neither deposit nor withdraw
                } catch (Exception e) {
                    setErrorTxReceipt(e.getMessage());
                    log.debug("TransferChannel Exception : {}", e.getMessage());
                    return false;
                }

            } catch (Exception e) {
                setFalseTxReceipt(INVALID_PARAMS);
                log.debug("{} : {}", INVALID_PARAMS, e.getMessage());
                return false;
            }

        }

        @ContractChannelMethod
        public boolean transferFeeChannel(JsonObject params) {
            try {
                String otherContract = this.receipt.getContractVersion();
                String contractName = this.branchStateStore.getContractName(otherContract);
                String contractAccount = String.format("%s%s", PrefixKeyEnum.CONTRACT_ACCOUNT, contractName);

                BigInteger serviceFee = params.get(SERVICE_FEE).getAsBigInteger();
                try {
                    return transferFee(contractAccount, serviceFee);
                } catch (Exception e) {
                    setErrorTxReceipt(e.getMessage());
                    log.debug("TransferFeeChannel Exception : {}", e.getMessage());
                    return false;
                }
            } catch (Exception e) {
                setFalseTxReceipt(INVALID_PARAMS);
                log.debug("{} : {}", INVALID_PARAMS, e.getMessage());
                return false;
            }
        }

        private boolean transferFee(String from, BigInteger fee) {
            boolean result = checkAmount(fee) && transfer(from, from, BigInteger.ZERO, fee);
            if (result) {
                setSuccessTxReceipt(String.format(TRANSFER_FEE_SUCCESS, from, fee));
            } else {
                setFalseTxReceipt(String.format(TRANSFER_FEE_FAIL, from, fee));
            }
            return result;
        }

        private void addBalanceTo(String to, BigInteger amount) {
            BigInteger balance = getBalance(to);
            putBalance(to, balance.add(amount));
        }

        private void subtractBalanceFrom(String from, BigInteger amount) {
            BigInteger balance = getBalance(from);
            putBalance(from, balance.subtract(amount));
        }

        private void putBalance(String address, BigInteger value) {
            // TODO Store Base64 Encoding
            JsonObject storeValue = new JsonObject();
            storeValue.addProperty(BALANCE, value);
            address = PrefixKeyEnum.getAccountKey(address);
            store.put(address, storeValue);
        }

        public BigInteger getBalance(String address) {
            return getBalance(PrefixKeyEnum.ACCOUNT, address);
        }

        private BigInteger getBalance(PrefixKeyEnum type, String address) {
            String searchAddress = String.format("%s%s",type.toValue(),address);
            JsonObject storeValue = store.get(searchAddress);
            // TODO Store Base64 Encoding
            return storeValue != null && storeValue.has(BALANCE)
                    ? storeValue.get(BALANCE).getAsBigInteger() : BigInteger.ZERO;
        }

        /***
         * BASE FEE Model
         * Transaction Byte length multiplex BASE_FEE (0.0005 YEED)
         * @return Fee Value
         */
        private BigInteger calculateFee() {
            return BASE_FEE.multiply(BigInteger.valueOf(this.receipt.getTxSize()));
        }

        @InvokeTransaction
        public void issuePropose(JsonObject params) {
            try {
                String issuer = this.receipt.getIssuer();
                BigInteger stakeYeed = params.get(STAKE_YEED).getAsBigInteger();
                BigInteger fee = params.has(FEE) ? params.get(FEE).getAsBigInteger() : calculateFee();
                BigInteger stakeFee = stakeYeed.add(fee);

                //TODO Check whether status is false or error
                if (checkBalance(issuer, stakeFee) && checkNetworkFee(fee) && checkYeedStakeAmount(stakeYeed)) {
                    String txId = this.receipt.getTxId();
                    // Add Transaction Id and Issuer
                    params.addProperty(TX_ID, txId);
                    params.addProperty(ISSUER, issuer);

                    ProposeInterChain propose = new ProposeInterChain(params);
                    String proposeId = propose.getProposeId();

                    try {
                        if (transfer(issuer, proposeId, stakeFee, BigInteger.ZERO)) {
                            String proposeIdKey = proposeKey(proposeId);
                            log.debug("Propose Id Key : {} ", proposeIdKey);
                            // Save propose and set its status
                            this.store.put(proposeIdKey, propose.toJsonObject());
                            setProposeStatus(proposeId, ProposeStatus.ISSUED);
                            setSuccessTxReceipt(String.format("Propose %s ISSUED", proposeId));
                        } else {
                            setFalseTxReceipt(String.format("Propose %s ISSUE Fail", proposeId));
                        }
                    } catch (Exception e) {
                        setErrorTxReceipt(e.getMessage());
                        log.debug("IssuePropose Exception : {}", e.getMessage());
                    }
                }

            } catch (Exception e) {
                setFalseTxReceipt(INVALID_PARAMS);
                log.debug("{} : {}", INVALID_PARAMS, e.getMessage());
            }
        }

        @ContractQuery
        public JsonObject queryPropose(JsonObject param) {
            try {
                String proposeId = param.get(PROPOSE_ID).getAsString();  // TODO toLowerCase
                ProposeInterChain propose = getPropose(proposeId);
                ProposeStatus proposeStatus = getProposeStatus(proposeId);
                JsonObject proposeJson = propose.toJsonObject();
                proposeJson.addProperty(STATUS, proposeStatus.toString());
                return proposeJson;
            } catch (Exception e) {
                setErrorTxReceipt(e.getMessage());
                return new JsonObject();
            }
        }

        private boolean isProposeExist(String proposeId) {
            return this.store.contains(proposeKey(proposeId));
        }

        private boolean isProposeStatusExist(String proposeId) {
            return this.store.contains(proposeStatusKey(proposeId));
        }

        public ProposeInterChain getPropose(String proposeId) throws RuntimeException {
            if (!isProposeExist(proposeId)) {
                throw new RuntimeException(PROPOSE_NOT_EXISTS);
            }

            return new ProposeInterChain(this.store.get(proposeKey(proposeId)));
        }

        private ProposeStatus getProposeStatus(String proposeId) throws RuntimeException {
            if (!isProposeStatusExist(proposeId)) {
                throw new RuntimeException(PROPOSE_STATUS_NOT_EXISTS);
            }

            return ProposeStatus.fromValue(this.store.get(proposeStatusKey(proposeId)).get(STATUS).getAsInt());
        }

        private void setProposeStatus(String proposeId, ProposeStatus status) {
            JsonObject statusValue = new JsonObject();
            statusValue.addProperty(STATUS, status.toValue());
            String proposeKey = proposeStatusKey(proposeId);
            this.store.put(proposeKey, statusValue);
            log.debug("ProposeId : {} (ProposeKey={}) is {}", proposeId, proposeKey, status);
        }

        @InvokeTransaction
        public void processPropose(JsonObject param) { // Issue interTransfer Eth(or Token) To YEED
            /*
            // TODO Check issuer (Can any other address process propose?)
            if (!propose.getIssuer().equals(this.receipt.getIssuer())) {
                setErrorTxReceipt("The issuer is not the proposer");
                return;
            }
            */
            try {
                String issuer = this.receipt.getIssuer();
                String rawTransaction = param.get(RAW_TX).getAsString();
                BigInteger fee = param.has(FEE) ? param.get(FEE).getAsBigInteger() : calculateFee();
                String proposeIdParam = param.get(PROPOSE_ID).getAsString();

                try {
                    ProposeInterChain propose = getPropose(proposeIdParam);
                    ProposeStatus proposeStatus = getProposeStatus(propose.getProposeId());

                    if (ProposeStatus.ISSUED != proposeStatus && ProposeStatus.PROCESSING != proposeStatus) {
                        transferFee(this.receipt.getIssuer(), fee);
                        setFalseTxReceipt(String.format(PROCESS_PROPOSE_FAIL, proposeStatus));
                        return;
                    }

                    if (checkNetworkFee(fee) && checkBalance(issuer, fee)) {
                        switch (propose.getProposeType()) {
                            case YEED_TO_ETHER:
                                processYeedToEth(propose, rawTransaction, fee);
                                break;
                            case YEED_TO_ETHER_TOKEN:
                                processYeedToEthToken(propose, rawTransaction, fee);
                                break;
                            default:
                                throw new RuntimeException("Not Supported Yet");
                        }
                    }
                } catch (Exception e) {
                    setErrorTxReceipt(e.getMessage());
                    log.debug("ProcessPropose Exception : {}", e.getMessage());
                }

            } catch (Exception e) {
                setFalseTxReceipt(INVALID_PARAMS);
                log.debug("{} : {}", INVALID_PARAMS, e.getMessage());
            }

        }

        public void processYeedToEth(ProposeInterChain propose, String rawTransaction, BigInteger fee) {
            byte[] etheSendEncode = HexUtil.hexStringToBytes(rawTransaction);
            EthTransaction ethTransaction = new EthTransaction(etheSendEncode);

            String senderAddress = HexUtil.toHexString(ethTransaction.getSendAddress());
            String receiverAddress = HexUtil.toHexString(ethTransaction.getReceiverAddress());

            ProcessTransaction pt = new ProcessTransaction();
            pt.setSenderAddress(senderAddress);
            pt.setReceiverAddress(receiverAddress);
            pt.setChainId(ethTransaction.getChainId());
            pt.setAsset(ethTransaction.getValue());
            pt.setTransactionHash(HexUtil.toHexString(ethTransaction.getTxHash()));

            // Ethereum Transaction and Proposal verification
            transferFee(this.receipt.getIssuer(), fee);
            int checkPropose = propose.verificationProposeProcess(pt);
            if (checkPropose == 0) {
                processProposeTransaction(propose, pt);
                setSuccessTxReceipt(PROCESS_YEED_TO_ETH_SUCCESS);
            } else {
                log.error("[Yeed -> Eth] Error Code", checkPropose);
                List<String> errors = ProposeErrorCode.errorLogs(checkPropose);
                setFalseTxReceipt(errors);
            }
        }

        public void processYeedToEthToken(ProposeInterChain propose, String rawTransaction, BigInteger fee) {
            byte[] etheSendEncode = HexUtil.hexStringToBytes(rawTransaction);
            EthTokenTransaction tokenTransaction = new EthTokenTransaction(etheSendEncode);

            String senderAddress = HexUtil.toHexString(tokenTransaction.getSendAddress());
            // TODO Token Swap Need to Method
            // input data param[0] == method, param[1] == ReceiveAddress, param[2] == asset
            // Check Method - Token a9059cbb
            String method = HexUtil.toHexString(tokenTransaction.getMethod());
            String receiveAddress = HexUtil.toHexString(tokenTransaction.getParam()[0]);
            BigInteger sendAsset = new BigInteger(tokenTransaction.getParam()[1]);
            String targetAddress = HexUtil.toHexString(tokenTransaction.getReceiverAddress());

            ProcessTransaction pt = new ProcessTransaction();
            pt.setSenderAddress(senderAddress);
            pt.setReceiverAddress(receiveAddress.substring(24));
            pt.setChainId(tokenTransaction.getChainId());
            pt.setTargetAddress(targetAddress);
            pt.setAsset(sendAsset);
            pt.setMethod(method);
            pt.setTransactionHash(HexUtil.toHexString(tokenTransaction.getTxHash()));
            // Transfer Fee
            transferFee(this.receipt.getIssuer(), fee);

            // Check Propose
            int checkPropose = propose.verificationProposeProcess(pt);
            if (checkPropose == 0) {
                processProposeTransaction(propose, pt);
                setSuccessTxReceipt(PROCESS_YEED_TO_ETH_TOKEN_SUCCESS);
            } else {
                log.error("[Yeed -> EthToken] Error Code {}", checkPropose);
                List<String> errors = ProposeErrorCode.errorLogs(checkPropose);
                setFalseTxReceipt(errors);
            }
        }

        private void processProposeTransaction(ProposeInterChain propose, ProcessTransaction pt) {
            final boolean isProposeSender = propose.proposeSender(pt.getSenderAddress());

            BigDecimal receiveValue = new BigDecimal(pt.getAsset());
            BigDecimal stakeYeedDecimal = new BigDecimal(propose.getStakeYeed());
            // Calculate ratio
            BigDecimal ratio = stakeYeedDecimal.divide(new BigDecimal(propose.getReceiveAsset()));

            //BigInteger ratio = propose.getReceiveAsset().divide(propose.getStakeYeed());
            log.debug("Ratio : {}", ratio);
            BigInteger transferYeed = ratio.multiply(receiveValue).toBigInteger();
            log.debug("transferYeed : {}", transferYeed);
            BigInteger stakeYeed = getBalance(propose.getProposeId());
            stakeYeed = stakeYeed.subtract(propose.getFee());

            // Set transferYeed to stakeYeed if transferYeed is greater than stakeYeed
            if (stakeYeed.compareTo(transferYeed) < 0) {
                transferYeed = stakeYeed;
            }

            // Transaction ID , propose ID, SendAddress, transfer YEED
            TxConfirm confirm = new TxConfirm(
                    propose.getProposeId(), pt.getTransactionHash(), pt.getSenderAddress(), transferYeed);
            // confirm duplicate
            log.debug("Confirm Id : {}", confirm.getTxConfirmId());
            if (!processConfirmTx(propose, confirm)) {
                throw new RuntimeException("Propose Confirm Duplicate");
            }
            // Check the transaction issuer is same as the proposal issuer.
            boolean isProposerIssuer = propose.getIssuer().equals(this.receipt.getIssuer());
            if (isProposerIssuer && isProposeSender) {
                // 1. Proposal issuer and this transaction issuer are same
                // 2. Proposal set Sender Address
                // 3. Proposal Sender Address sends transaction to ReceiverAddress
                transfer(propose.getProposeId(), pt.getSenderAddress(), transferYeed, BigInteger.ZERO);
                stakeYeed = stakeYeed.subtract(transferYeed);
                // All stake YEED is transfer to senderAddress
                if (stakeYeed.compareTo(BigInteger.ZERO) <= 0) {
                    // 1. Proposal issuer and process issuer are same
                    // 2. Receive Asset Value is more than propose receiveAsset or equal
                    // 3. Proposal set Sender Address
                    if (pt.getAsset().compareTo(propose.getReceiveAsset()) >= 0) {
                        BigInteger proposeFee = propose.getFee();
                        BigInteger returnFee = proposeFee.divide(BigInteger.valueOf(2L));
                        proposeProcessDone(propose, ProposeStatus.DONE, returnFee);
                    } else {
                        // Done propose so send Fee to branch
                        proposeProcessDone(propose, ProposeStatus.DONE, BigInteger.ZERO);
                    }
                }
            } else {
                setProposeStatus(propose.getProposeId(), ProposeStatus.PROCESSING);
                this.receipt.addLog(
                        String.format("Propose %s %s", propose.getProposeId(), ProposeStatus.PROCESSING));
            }
        }

        private void proposeProcessDone(ProposeInterChain propose, ProposeStatus status, BigInteger refundFee) {
            BigInteger stakeBalance = getBalance(propose.getProposeId());
            BigInteger proposeFee = propose.getFee();
            proposeFee = proposeFee.subtract(refundFee);
            setProposeStatus(propose.getProposeId(), status);
            BigInteger returnStakeBalance = stakeBalance.subtract(proposeFee);
            transfer(propose.getProposeId(), propose.getIssuer(), returnStakeBalance, proposeFee);
            setSuccessTxReceipt(String.format("Proposal %s %s", propose.getProposeId(), status));
        }

        private boolean processConfirmTx(ProposeInterChain propose, TxConfirm confirm) {
            // Check confirmTx already exists
            if (isTxConfirmExist(confirm.getTxConfirmId())) {
                setFalseTxReceipt(String.format(TX_CONFIRM_EXIST, propose.getProposeId(), confirm.getTxConfirmId()));
                return false;
            } else {
                // Save TxConfirm
                setTxConfirm(confirm);
                setSuccessTxReceipt(String.format(TX_CONFIRM_PROCESS_SUCCESS,
                        propose.getProposeId(), propose.getProposeType(), propose.getReceiveChainId(),
                        confirm.getTxId(), confirm.getTxConfirmId()));
                return true;
            }
        }

        @InvokeTransaction
        public void closePropose(JsonObject param) { // Close the proposal if it is issued by the proposal issuer
            try {
                String proposeIdParam = param.get(PROPOSE_ID).getAsString();

                try {
                    ProposeInterChain propose = getPropose(proposeIdParam);
                    ProposeStatus proposeStatus = getProposeStatus(proposeIdParam);
                    // Check block height
                    if (this.receipt.getBlockHeight() < propose.getBlockHeight()) {
                        throw new RuntimeException(PROPOSAL_NOT_EXPIRED);
                    }
                    // Check propose status
                    if (proposeStatus == ProposeStatus.CLOSED) {
                        throw new RuntimeException(PROPOSAL_ALREADY_CLOSED);
                    }
                    // Check issuer or validator
                    if (!propose.getIssuer().equals(this.receipt.getIssuer())) {
                        throw new RuntimeException(TX_ISSUER_IS_NOT_PROPOSAL_ISSUER);
                    }
                    proposeProcessDone(propose, ProposeStatus.CLOSED, BigInteger.ZERO);
                } catch (Exception e) {
                    setErrorTxReceipt(e.getMessage());
                    log.debug("ClosePropose Exception : {}", e.getMessage());
                }
            } catch (Exception e) {
                setFalseTxReceipt(INVALID_PARAMS);
                log.debug("{} : {}", INVALID_PARAMS, e.getMessage());
            }
        }

        @InvokeTransaction
        public void transactionConfirm(JsonObject param) { // Validator can validate other network transaction
            // TODO validator or truth node can validate transaction confirm
            // Validator is required for transaction confirmation
            if (!this.branchStateStore.isValidator(this.receipt.getIssuer())) {
                throw new RuntimeException(TX_CONFIRM_REQUIRE_VALIDATOR);
            }

            try {
                // Get Transaction Confirm by id
                String txConfirmId = param.get(TX_CONFIRM_ID).getAsString();
                TxConfirmStatus status = TxConfirmStatus.fromValue(param.get(STATUS).getAsInt());
                long blockHeight = param.get(BLOCK_HEIGHT).getAsLong();
                int index = param.get(INDEX).getAsInt();
                long lastBlockHeight = param.get(LAST_BLOCK_HEIGHT).getAsLong();

                try {
                    TxConfirm txConfirm = getTxConfirm(txConfirmId);
                    TxConfirmStatus curStatus = txConfirm.getStatus();
                    log.debug("TxConfirm ProposeId :  {}", txConfirm.getProposeId());
                    log.debug("TxConfirm Status : {}", txConfirm.getStatus());

                    // Get confirm
                    if (curStatus == TxConfirmStatus.VALIDATE_REQUIRE || curStatus == TxConfirmStatus.NOT_EXIST) {

                        txConfirm.setBlockHeight(blockHeight);
                        txConfirm.setIndex(index);
                        txConfirm.setLastBlockHeight(lastBlockHeight);

                        ProposeInterChain pi = getPropose(txConfirm.getProposeId());
                        ProposeStatus pis = getProposeStatus(txConfirm.getProposeId());
                        if (pis == ProposeStatus.PROCESSING) {
                            //  processing propose
                            if (status == TxConfirmStatus.DONE) {
                                txConfirm.setStatus(TxConfirmStatus.DONE);
                                txConfirm.setIndex(index);
                                txConfirm.setBlockHeight(blockHeight);

                                // Check block height
                                // Propose Issuer set network block height
                                log.debug("Propose require block height : {} ", pi.getNetworkBlockHeight());
                                log.debug("TxConfirm block height : {} ", txConfirm.getBlockHeight());
                                boolean checkBlockHeight = false;
                                // TODO Fix Network Block Height Check
                                if (pi.getNetworkBlockHeight() <= txConfirm.getBlockHeight()) {
                                    checkBlockHeight = true;
                                }

                                log.debug("CheckBlockHeight : {}", checkBlockHeight);
                                if (checkBlockHeight) {
                                    BigInteger fee = pi.getFee();
                                    BigInteger stakeYeed = getBalance(pi.getProposeId());
                                    stakeYeed = stakeYeed.subtract(fee);
                                    // find last
                                    BigInteger transferYeed = txConfirm.getTransferYeed();
                                    // If stake Yeed is
                                    if (stakeYeed.compareTo(transferYeed) <= 0) {
                                        transferYeed = stakeYeed;
                                    }
                                    log.debug("Stake YEED {}", stakeYeed);
                                    log.debug("TransferYeed YEED {}", transferYeed);
                                    log.debug("PI Fee {}", fee);
                                    // Send transaction confirm
                                    boolean transfer = transfer(pi.getProposeId(), txConfirm.getSenderAddress(),
                                            transferYeed, BigInteger.ZERO);
                                    if (transfer) {
                                        setSuccessTxReceipt(String.format(TX_CONFIRM_DONE, txConfirmId));
                                        txConfirm.setStatus(TxConfirmStatus.DONE);
                                        setTxConfirm(txConfirm);

                                        // Propose is done
                                        stakeYeed = stakeYeed.subtract(transferYeed);
                                        if (stakeYeed.compareTo(BigInteger.ZERO) == 0) {
                                            proposeProcessDone(pi, ProposeStatus.DONE, BigInteger.ZERO);
                                        }
                                    } else {
                                        setFalseTxReceipt(String.format(TX_CONFIRM_FAIL, txConfirmId));
                                    }

                                }
                            }
                        } else {
                            // Propose Status is not processing
                            setFalseTxReceipt(String.format("%s is %s", pi.getProposeId(), pis.toString()));
                        }
                    } else {
                        setFalseTxReceipt(String.format("%s is %s", txConfirmId, txConfirm.getStatus().toString()));
                    }

                } catch (Exception e) {
                    setErrorTxReceipt(e.getMessage());
                    log.debug("TransactionConfirm Exception : {}", e.getMessage());
                }

            } catch (Exception e) {
                setFalseTxReceipt(INVALID_PARAMS);
                log.debug("{} : {}", INVALID_PARAMS, e.getMessage());
            }
        }

        @ContractQuery
        public JsonObject queryTransactionConfirm(JsonObject param) {
            try {
                String txConfirmId = param.get(TX_CONFIRM_ID).getAsString().toLowerCase();
                TxConfirm confirm = getTxConfirm(txConfirmId);
                return confirm.toJsonObject();
            } catch (Exception e) {
                return new JsonObject();
            }
        }

        public boolean isTxConfirmExist(String txConfirmId) {
            return this.store.contains(txConfirmKey(txConfirmId));
        }

        public TxConfirm getTxConfirm(String txConfirmId) throws RuntimeException {
            if (!isTxConfirmExist(txConfirmId)) {
                throw new RuntimeException("Transaction Confirm not exists");
            }

            JsonObject txConfirmObj = this.store.get(txConfirmKey(txConfirmId));
            return new TxConfirm(txConfirmObj);
        }

        private void setTxConfirm(TxConfirm txConfirm) {
            this.store.put(txConfirmKey(txConfirm.getTxConfirmId()), txConfirm.toJsonObject());
        }

        @InvokeTransaction
        public void faucet(JsonObject param) { // THIS IS FAUCET IN TEST NET!!
            String issuer = this.receipt.getIssuer();
            String faucetKey = String.format("%s%s", "faucet", issuer);
            BigInteger balance = this.getBalance(issuer);
            // Can be charged Yeed once per account
            if (!this.store.contains(faucetKey) && balance.compareTo(BigInteger.ZERO) == 0) {
                balance = balance.add(BASE_CURRENCY.multiply(BigInteger.valueOf(1000L))); // Add 1000 YEED

                // Update TOTAL SUPPLY
                BigInteger totalSupply = this.totalSupply();
                totalSupply = totalSupply.add(balance);
                // Save total supply and faucet balance
                putBalance(TOTAL_SUPPLY, totalSupply);
                putBalance(issuer, balance);

                setSuccessTxReceipt(Arrays.asList("The faucet function can only be called from a test net",
                        String.format("%s has received %s", issuer, balance.toString())));

            } else {
                setErrorTxReceipt(String.format("%s has already received or has the balance", issuer));
            }
        }

        @ContractEndBlock
        public void endBlock() {
            try {
                // Network Fee Burn
                BigInteger networkFee = getBalance(receipt.getBranchId());
                if (networkFee.compareTo(BigInteger.ZERO) > 0) {
                    BigInteger totalSupply = totalSupply();

                    // subtract networkFee
                    totalSupply = totalSupply.subtract(networkFee);

                    // Set Total Supply
                    putBalance(TOTAL_SUPPLY, totalSupply);
                    this.receipt.addLog(String.format(BURN_YEED, networkFee));

                    // Network Fee is Zero
                    putBalance(receipt.getBranchId(), BigInteger.ZERO);
                }
                receipt.setStatus(ExecuteStatus.SUCCESS);
            } catch (Exception e) {
                this.receipt.addLog(e.getMessage());
                log.debug("EndBlock Exception : {}", e.getMessage());
            }
        }

        private boolean checkYeedStakeAmount(BigInteger amount) {
            if (!isPositive(amount)) { // Amount must be greater than zero
                setFalseTxReceipt(INVALID_AMOUNT_OF_STAKE_YEED);
                log.debug(INVALID_AMOUNT_OF_STAKE_YEED);
                return false;
            }
            return true;
        }

        private boolean checkNetworkFee(BigInteger fee) {
            BigInteger networkFee = calculateFee();
            if (networkFee.compareTo(fee) > 0) { // Fee must be greater or equal to network fee
                setFalseTxReceipt(LOW_TX_FEE);
                log.debug(LOW_TX_FEE);
                return false;
            }
            return true;
        }

        private boolean checkBalanceEmpty(String account) {
            if (isAccountEmpty(account)) { // Balance must not be empty
                setFalseTxReceipt(BALANCE_EMPTY);
                log.debug(BALANCE_EMPTY);
                return true;
            }
            return false;
        }

        private boolean checkBalance(String account, BigInteger amount) {
            if (!isTransferable(getBalance(account), amount)) {
                setFalseTxReceipt(INSUFFICIENT_FUNDS);
                log.debug(INSUFFICIENT_FUNDS);
                return false;
            }
            return true;
        }

        private boolean checkAmount(BigInteger amount) {
            if (!isPositive(amount)) { // Amount must be greater than zero
                setFalseTxReceipt(INVALID_AMOUNT);
                log.debug(INVALID_AMOUNT);
                return false;
            }
            return true;
        }

        private String approveKey(String sender, String spender) {
            byte[] approveKeyByteArray = ByteUtil.merge(sender.getBytes(), spender.getBytes());
            byte[] approveKey = HashUtil.sha3(approveKeyByteArray);
            return String.format("%s%s", PrefixKeyEnum.APPROVE.toValue(), HexUtil.toHexString(approveKey));
        }

        private String contractAccountKey(String contractName) {
            return  String.format("%s%s", PrefixKeyEnum.CONTRACT_ACCOUNT, contractName);
        }

        private String txConfirmKey(String txConfirmId) {
            return String.format("%s%s", PrefixKeyEnum.TRANSACTION_CONFIRM.toValue(), txConfirmId);
        }

        private String proposeKey(String proposeId) {
            return String.format("%s%s", PrefixKeyEnum.PROPOSE_INTER_CHAIN.toValue(), proposeId);
        }

        private String proposeStatusKey(String proposeId) {
            return String.format("%s%s", PrefixKeyEnum.PROPOSE_INTER_CHAIN_STATUS.toValue(), proposeId);
        }

        private void setErrorTxReceipt(String msg) {
            this.receipt.setStatus(ExecuteStatus.ERROR);
            this.receipt.addLog(msg);
        }

        private void setFalseTxReceipt(String msg) {
            this.receipt.setStatus(ExecuteStatus.FALSE);
            this.receipt.addLog(msg);
        }

        private void setFalseTxReceipt(List<String> msgs) {
            this.receipt.setStatus(ExecuteStatus.FALSE);
            msgs.forEach(l -> this.receipt.addLog(l));
        }

        private void setSuccessTxReceipt(String msg) {
            this.receipt.setStatus(ExecuteStatus.SUCCESS);
            this.receipt.addLog(msg);
        }

        private void setSuccessTxReceipt(List<String> msgs) {
            this.receipt.setStatus(ExecuteStatus.SUCCESS);
            msgs.forEach(l -> this.receipt.addLog(l));
        }

    }
}
