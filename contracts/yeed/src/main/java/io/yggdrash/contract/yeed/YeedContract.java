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
import io.yggdrash.common.utils.JsonUtil;
import io.yggdrash.contract.core.ExecuteStatus;
import io.yggdrash.contract.core.TransactionReceipt;
import io.yggdrash.contract.core.annotation.ContractBranchStateStore;
import io.yggdrash.contract.core.annotation.ContractChannelField;
import io.yggdrash.contract.core.annotation.ContractChannelMethod;
import io.yggdrash.contract.core.annotation.ContractQuery;
import io.yggdrash.contract.core.annotation.ContractStateStore;
import io.yggdrash.contract.core.annotation.ContractTransactionReceipt;
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
import io.yggdrash.contract.yeed.propose.ProposeType;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public class YeedContract implements BundleActivator, ServiceListener {
    private static final Logger log = LoggerFactory.getLogger(YeedContract.class);
    private static final String AMOUNT = "amount";
    private static final String BALANCE = "balance";
    private static final String FEE = "fee";

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
        // 1 YEED = 1000000000000000000
        private static BigInteger BASE_CURRENCY = BigInteger.TEN.pow(18);

        private static final String TOTAL_SUPPLY = "TOTAL_SUPPLY";

        @ContractTransactionReceipt
        TransactionReceipt txReceipt;

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
            String address = params.get("address").getAsString().toLowerCase();
            return getBalance(address);
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
            String contractAccount
                    = String.format("%s%s", PrefixKeyEnum.CONTRACT_ACCOUNT, params.get("contractName").getAsString());
            return getBalance(contractAccount);
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
            String owner = params.get("owner").getAsString().toLowerCase();
            String spender = params.get("spender").getAsString().toLowerCase();
            String approveKey = approveKey(owner, spender);

            return getBalance(approveKey);
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
            // Set totalSupply to the sum of all balances of alloc
            BigInteger totalSupply = BigInteger.ZERO;

            JsonObject alloc = params.getAsJsonObject("alloc");
            for (Map.Entry<String, JsonElement> entry : alloc.entrySet()) {
                String frontier = entry.getKey();
                JsonObject value = entry.getValue().getAsJsonObject();
                BigInteger balance = value.get(BALANCE).getAsBigInteger();
                // apply BASE_CURRENCY
                balance = balance.multiply(BASE_CURRENCY);

                totalSupply = totalSupply.add(balance);
                addBalanceTo(frontier, balance);
                //putBalance(frontier, balance);

                JsonObject mintLog = new JsonObject();
                mintLog.addProperty("to", frontier);
                mintLog.addProperty(BALANCE, balance.toString());
                txReceipt.addLog(mintLog.toString());
                log.debug("Address of Frontier : {}"
                        + "Balance of Frontier : {}", frontier, getBalance(frontier));
            }
            putBalance(TOTAL_SUPPLY, totalSupply);

            // TODO Validator will call by contract channel
            // boolean isSuccess = saveInitValidator(params.getAsJsonArray("validator"));
            boolean isSuccess = true;
            if (isSuccess) {
                setSuccessTxReceipt(
                        String.format("Initialization completed successfully. Total Supply is %s", totalSupply));
            } else {
                setErrorTxReceipt("Initialization failed");
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
        @Override
        public TransactionReceipt approve(JsonObject params) {
            String sender = txReceipt.getIssuer();
            BigInteger amount = params.get(AMOUNT).getAsBigInteger();
            BigInteger senderBalance = getBalance(sender);

            BigInteger fee = params.has(FEE) ? params.get(FEE).getAsBigInteger() : BigInteger.ZERO;
            BigInteger networkFee = calculateFee();
            // Check Fee
            if (networkFee.compareTo(fee) > 0) {
                setErrorTxReceipt("Low transaction fee");
                return txReceipt;
            }

            if (!isTransferable(senderBalance, amount)) {
                setErrorTxReceipt("Insufficient funds");
                return txReceipt;
            }


            boolean transferFee = transferFee(sender, fee);
            if (!transferFee) {
                setErrorTxReceipt("Invalid fee");
                return txReceipt;
            }

            String spender = params.get("spender").getAsString().toLowerCase();
            String approveKey = approveKey(sender, spender);
            putBalance(approveKey, amount);

            setSuccessTxReceipt(
                    String.format("[Approved] Approve %s to %s from %s", spender, getBalance(approveKey), sender));
            log.debug("[Approved] Approve {} to {} from {}. ApproveKey : {}",
                    spender, getBalance(approveKey), sender, approveKey);

            return txReceipt;
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
            String from = this.txReceipt.getIssuer();
            if (isAccountEmpty(from)) {
                setErrorTxReceipt("Insufficient funds");
                return txReceipt;
            }

            String to = params.get("to").getAsString().toLowerCase();
            BigInteger amount = params.get(AMOUNT).getAsBigInteger();

            // Check amount and fee. The amount and fee must be greater than zero. (No fee is allowed currently)
            BigInteger fee = params.has(FEE) ? params.get(FEE).getAsBigInteger() : BigInteger.ZERO;

            BigInteger networkFee = calculateFee();
            // Check Fee
            if (networkFee.compareTo(fee) > 0) {
                setErrorTxReceipt("Low transaction fee");
                return txReceipt;
            }

            if (!isPositive(amount)) { // amount > 0
                setErrorTxReceipt("Invalid amount");
                return txReceipt;
            }

            boolean isTransfer = transfer(from, to, amount, fee);
            // Set Transfer Result
            setTransferExecute(isTransfer, from, to, amount, fee);

            if (log.isDebugEnabled() && isTransfer) {
                log.debug("[Transferred] Transfer {} from {} to {}", amount, from, to);
                log.trace("Balance of From ({}) : {} To ({}) : {}", from, getBalance(from), to, getBalance(to));
            }
            return txReceipt;
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
                addBalanceTo(to, amount);
                fromBalance = getBalance(from).subtract(feeAmount);
                putBalance(from, fromBalance);
                // Stores fee for each branchId to reward to the validators <branchId : fee>
                addBalanceTo(txReceipt.getBranchId(), fee);
                return true;
            } else {
                log.debug("is Not transferable");
                return false;
            }
        }

        private boolean isNegative(BigInteger amount) {
            return amount.compareTo(BigInteger.ZERO) < 0; // -1
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
            String address = params.get("address").getAsString();
            BigInteger fee = params.get("amount").getAsBigInteger();
            return isTransferable(getBalance(address), fee);
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
        @Override
        public TransactionReceipt transferFrom(JsonObject params) {
            String to = params.get("to").getAsString().toLowerCase();
            String from = params.get("from").getAsString().toLowerCase();
            String sender = txReceipt.getIssuer();
            String approveKey = approveKey(from, sender);

            // Check approved amount empty
            if (isAccountEmpty(approveKey)) {
                setErrorTxReceipt("Insufficient funds");
                return txReceipt;
            }

            BigInteger fee = params.has(FEE) ? params.get(FEE).getAsBigInteger() : BigInteger.ZERO;
            BigInteger networkFee = calculateFee();
            // Check Fee
            if (networkFee.compareTo(fee) > 0) {
                setErrorTxReceipt("Low transaction fee");
                return txReceipt;
            }

            BigInteger approveValue = getBalance(approveKey);
            BigInteger amount = params.get(AMOUNT).getAsBigInteger();
            BigInteger amountFee = amount.add(fee);

            // The fee is transferred from the approved amount. Sender can transfer yeed without any fee.
            if (!isTransferable(approveValue, amountFee)) {
                setErrorTxReceipt("Insufficient funds");
                return txReceipt;
            }

            boolean isTransfer = transfer(from, to, amount, fee);
            setTransferExecute(isTransfer, from, to, amount, fee);

            if (isTransfer) {
                txReceipt.addLog(
                        String.format("transferFrom %s from %s to %s fee %s by %s", amount, from, to, fee, sender));
            } else {
                // Transfer Error
                return txReceipt;
            }

            approveValue = approveValue.subtract(amountFee);
            putBalance(approveKey, approveValue);

            if (log.isDebugEnabled()) {
                log.debug("[Transferred] Transfer {} from {} to {}", amount, from, to);
                log.debug("Allowed amount of Sender ({}) : {}", sender, approveValue);
                log.debug("Balance of From ({}) : {} Balance of To   ({}) : {}", from, getBalance(from),
                        to, amount);
            }

            return txReceipt;
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
            String otherContract = this.txReceipt.getContractVersion();
            String contractName = this.branchStateStore.getContractName(otherContract);
            String contractAccount = String.format("%s%s", PrefixKeyEnum.CONTRACT_ACCOUNT, contractName);

            String issuer = this.txReceipt.getIssuer();
            String fromAccount = params.get("from").getAsString();
            String toAccount = params.get("to").getAsString();
            BigInteger amount = params.get("amount").getAsBigInteger();
            BigInteger serviceFee = params.has("serviceFee")
                    ? params.get("serviceFee").getAsBigInteger() : BigInteger.ZERO;

            if (!isPositive(amount)) {
                return false;
            }

            if (toAccount.equalsIgnoreCase(contractName) &&
                    fromAccount.equalsIgnoreCase(issuer)) {
                // deposit
                return transfer(fromAccount, contractAccount, amount, serviceFee);
            } else if (fromAccount.equalsIgnoreCase(contractName)) { // withdraw
                // withdraw service fee is used contract Account
                return transfer(contractAccount, toAccount, amount, serviceFee);

            }
            this.txReceipt.addLog("Transfer channel fail");
            return false; // If neither deposit nor withdraw
        }


        protected boolean transferFee(String from, BigInteger fee) {
            if (fee.compareTo(BigInteger.ZERO) > 0) {
                return transfer(from, from, BigInteger.ZERO, fee);
            } else {
                return fee.compareTo(BigInteger.ZERO) >= 0; // Return false if fee is less than zero (fee < 0)
            }
        }

        private void addBalanceTo(String to, BigInteger amount) {
            if (amount.compareTo(BigInteger.ZERO) > 0) {
                BigInteger balance = getBalance(to);
                putBalance(to, balance.add(amount));
            }
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

        private void putBalance(String address, BigInteger value) {
            // TODO Store Base64 Encoding
            JsonObject storeValue = new JsonObject();
            storeValue.addProperty(BALANCE, value);
            address = PrefixKeyEnum.getAccountKey(address);
            store.put(address, storeValue);
        }

        private String approveKey(String sender, String spender) {
            byte[] approveKeyByteArray = ByteUtil.merge(sender.getBytes(), spender.getBytes());
            byte[] approveKey = HashUtil.sha3(approveKeyByteArray);
            return String.format("%s%s", PrefixKeyEnum.APPROVE.toValue(), HexUtil.toHexString(approveKey));
        }


        /***
         * BASE FEE Model
         * Transaction Byte length multiplex BASE_FEE (0.0005 YEED)
         * @return Fee Value
         */
        private BigInteger calculateFee() {
            return BASE_FEE.multiply(BigInteger.valueOf(this.txReceipt.getTxSize()));
        }


        private void setTransferExecute(boolean isTransfer, String from, String to, BigInteger amount, BigInteger fee) {
            if (isTransfer) {
                setSuccessTxReceipt(
                        String.format("Transfer %s from %s to %s fee %s", amount, from, to, fee));
            } else {
                setErrorTxReceipt("Transfer error");
            }
        }


        @InvokeTransaction
        public void issuePropose(JsonObject params) {
            BigInteger stakeYeed = params.get("stakeYeed").getAsBigInteger();
            BigInteger fee = params.get(FEE).getAsBigInteger();
            String issuer = this.txReceipt.getIssuer();

            //TODO Check whether status is false or error

            // Check Issuer FEE and network FEE
            if (!isPositive(stakeYeed)) {
                setFalseTxReceipt("Invalid amount of stakeYeed");
                return;
            }

            BigInteger networkFee = calculateFee();
            // Check Fee
            if (networkFee.compareTo(fee) > 0) {
                setErrorTxReceipt("Low transaction fee");
                return;
            }

            // Check issuer balance
            BigInteger stakeFee = stakeYeed.add(fee);
            BigInteger balance = getBalance(issuer);
            if (!isTransferable(balance, stakeFee)) {
                setErrorTxReceipt("Insufficient funds");
                return;
            }

            // TokenAddress is YEED TO TOKEN
            String tokenAddress = JsonUtil.parseString(params, "tokenAddress", "");
            String receiveAddress = params.get("receiverAddress").getAsString();
            BigInteger receiveAsset = params.get("receiveAsset").getAsBigInteger();
            Integer receiveChainId = params.get("receiveChainId").getAsInt();
            long networkBlockHeight = params.get("networkBlockHeight").getAsLong();
            long target = params.get("blockHeight").getAsLong();
            ProposeType proposeType = ProposeType.fromValue(params.get("proposeType").getAsInt());
            String senderAddress = null;
            String inputData = null;
            if (ProposeType.YEED_TO_ETHER.equals(proposeType)) {
                senderAddress = params.get("senderAddress").getAsString();
                if (!params.get("inputData").isJsonNull()) {
                    inputData = params.get("inputData").getAsString();
                }
            }

            // Issue Proposal
            String txId = this.txReceipt.getTxId();
            ProposeInterChain propose = new ProposeInterChain(txId, tokenAddress, receiveAddress,
                    receiveAsset, receiveChainId, networkBlockHeight, proposeType, senderAddress, inputData,
                    stakeYeed, target, fee, issuer);

            String proposeIdKey = String.format("%s%s", PrefixKeyEnum.PROPOSE_INTER_CHAIN.toValue(),
                    propose.getProposeId());

            // The fee is transferred at the end (Issue closed or done)
            boolean isTransfer = transfer(issuer, propose.getProposeId(), stakeFee, BigInteger.ZERO);

            setTransferExecute(isTransfer, issuer, propose.getProposeId(), stakeFee, BigInteger.ZERO);
            if (!isTransfer) {
                txReceipt.addLog(String.format("Propose %s ISSUE Fail", propose.getProposeId()));
            }

            // Save propose and set its status
            this.store.put(proposeIdKey, propose.toJsonObject());
            setProposeStatus(propose.getProposeId(), ProposeStatus.ISSUED);
            setSuccessTxReceipt(String.format("Propose %s ISSUED", propose.getProposeId()));
        }

        @ContractQuery
        public JsonObject queryPropose(JsonObject param) {
            String proposeIdParam = param.get("proposeId").getAsString();
            ProposeInterChain propose = getPropose(proposeIdParam);
            JsonObject proposeJson = propose.toJsonObject();
            proposeJson.addProperty("status", getProposeStatus(proposeIdParam).toString());
            return proposeJson;
        }

        public ProposeInterChain getPropose(String proposeId) {
            String proposeIdKey = String.format("%s%s", PrefixKeyEnum.PROPOSE_INTER_CHAIN.toValue(), proposeId);
            JsonObject proposal = this.store.get(proposeIdKey);
            return new ProposeInterChain(proposal);
        }

        private ProposeStatus getProposeStatus(String proposeId) {
            String proposeKey = String.format("%s%s", PrefixKeyEnum.PROPOSE_INTER_CHAIN_STATUS.toValue(), proposeId);
            JsonObject proposeStatus = this.store.get(proposeKey);
            return ProposeStatus.fromValue(proposeStatus.get("status").getAsInt());
        }

        private void setProposeStatus(String proposeId, ProposeStatus status) {
            String proposeKey = String.format("%s%s",PrefixKeyEnum.PROPOSE_INTER_CHAIN_STATUS.toValue(), proposeId);
            JsonObject statusValue = new JsonObject();
            statusValue.addProperty("status", status.toValue());
            log.debug("ProposeId : {} (ProposeKey={}) is {}", proposeId, proposeKey, status);
            this.store.put(proposeKey, statusValue);
        }

        @InvokeTransaction
        public void processPropose(JsonObject param) { // Issue interTransfer Eth(or Token) To YEED
            String issuer = this.txReceipt.getIssuer();
            String rawTransaction = param.get("rawTransaction").getAsString();
            BigInteger fee = param.get("fee").getAsBigInteger();
            String proposeIdParam = param.get("proposeId").getAsString();

            ProposeInterChain propose = getPropose(proposeIdParam);

            /*
            // TODO Check issuer (Can any other address process propose?)
            if (!propose.getIssuer().equals(this.txReceipt.getIssuer())) {
                setErrorTxReceipt("The issuer is not the proposer");
                return;
            }
            */

            BigInteger networkFee = calculateFee();
            // Check Fee
            if (networkFee.compareTo(fee) > 0) {
                setErrorTxReceipt("Low transaction fee");
                return;
            }

            // Check issuer balance
            if (!isTransferable(getBalance(issuer), fee)) {
                setErrorTxReceipt("Insufficient funds");
                return;
            }

            ProposeStatus proposeStatus = getProposeStatus(propose.getProposeId());
            // Check propose status
            if (ProposeStatus.ISSUED != proposeStatus && ProposeStatus.PROCESSING != proposeStatus) {
                setFalseTxReceipt(String.format("Propose cannot proceed (ProposeStatus=%s)", proposeStatus));
                transferFee(this.txReceipt.getIssuer(), fee);
                return;
            }

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
            int checkPropose = propose.verificationProposeProcess(pt);
            if (checkPropose == 0) {
                processProposeTransaction(propose, pt);
                transferFee(this.txReceipt.getIssuer(), fee);
                setSuccessTxReceipt("Yeed to Eth Proposal completed successfully");
            } else {
                transferFee(this.txReceipt.getIssuer(), fee);
                log.error("[Yeed -> Eth] Error Code", checkPropose);
                List<String> errors = ProposeErrorCode.errorLogs(checkPropose);
                setFalseTxReceipt(errors);
            }
        }

        public void processYeedToEthToken(ProposeInterChain propose, String rawTransaction, BigInteger fee) {
            byte[] etheSendEncode = HexUtil.hexStringToBytes(rawTransaction);
            EthTokenTransaction tokenTransaction = new EthTokenTransaction(etheSendEncode);

            String senderAddress = HexUtil.toHexString(tokenTransaction.getSendAddress());
            // input data param[0] == method, param[1] == ReceiveAddress, param[2] == asset
            String receiveAddress = HexUtil.toHexString(tokenTransaction.getParam()[1]);
            BigInteger sendAsset = new BigInteger(tokenTransaction.getParam()[2]);
            String targetAddress = HexUtil.toHexString(tokenTransaction.getReceiverAddress());

            ProcessTransaction pt = new ProcessTransaction();
            pt.setSenderAddress(senderAddress);
            pt.setReceiverAddress(receiveAddress);
            pt.setChainId(tokenTransaction.getChainId());
            pt.setTargetAddress(targetAddress);
            pt.setAsset(sendAsset);

            int checkPropose = propose.verificationProposeProcess(pt);
            if (checkPropose == 0) {
                processProposeTransaction(propose, pt);
                transferFee(this.txReceipt.getIssuer(), fee);
                setSuccessTxReceipt("Yeed to EthToken Proposal completed successfully");
            } else {
                transferFee(this.txReceipt.getIssuer(), fee);
                log.error("[Yeed -> EthToken] Error Code", checkPropose);
                List<String> errors = ProposeErrorCode.errorLogs(checkPropose);
                setFalseTxReceipt(errors);
            }
        }

        private void processProposeTransaction(ProposeInterChain propose, ProcessTransaction pt) {
            boolean isProposeSender = propose.proposeSender(pt.getSenderAddress());

            BigInteger receiveValue = pt.getAsset();
            // Calculate ratio
            BigInteger ratio = propose.getReceiveAsset().divide(propose.getStakeYeed());
            BigInteger transferYeed = ratio.multiply(receiveValue);
            BigInteger stakeYeed = getBalance(propose.getProposeId());
            stakeYeed = stakeYeed.subtract(propose.getFee());

            // Set transferYeed to stakeYeed if transferYeed is greater than stakeYeed
            if (stakeYeed.compareTo(transferYeed) < 0) {
                transferYeed = stakeYeed;
            }

            // Check the transaction issuer is same as the proposal issuer.
            boolean isProposerIssuer = propose.getIssuer().equals(this.txReceipt.getIssuer());
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
                    if (receiveValue.compareTo(propose.getReceiveAsset()) >= 0) {
                        BigInteger proposeFee = propose.getFee();
                        BigInteger returnFee = proposeFee.divide(BigInteger.valueOf(2L));
                        proposeProcessDone(propose, ProposeStatus.DONE, returnFee);
                    } else {
                        // Done propose so send Fee to branch
                        proposeProcessDone(propose, ProposeStatus.DONE, BigInteger.ZERO);
                    }
                }
            } else {
                // Save Transaction confirm
                // Transaction ID , propose ID, SendAddress, transfer YEED
                TxConfirm confirm = new TxConfirm(
                        propose.getProposeId(), pt.getTransactionHash(), pt.getSenderAddress(), transferYeed);

                // Check confirmed txId exists
                boolean isConfirmTxIdExists = processConfirmTx(propose, confirm);
                if (isConfirmTxIdExists) {
                    setProposeStatus(propose.getProposeId(), ProposeStatus.PROCESSING);
                    this.txReceipt.addLog(
                            String.format("Propose %s %s", propose.getProposeId(), ProposeStatus.PROCESSING));
                }
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
            if (isExistTxConfirm(confirm.getTxConfirmId())) {
                setFalseTxReceipt(String.format("Propose %s transaction %s exist",
                        propose.getProposeId(), confirm.getTxConfirmId()));
                return false;
            } else {
                // Save TxConfirm
                setTxConfirm(confirm);
                setSuccessTxReceipt(String.format("Propose %s check %s network %s transaction %s confirm ID %s",
                        propose.getProposeId(), propose.getProposeType(), propose.getReceiveChainId(),
                        confirm.getTxId(), confirm.getTxConfirmId()));
                return true;
            }
        }

        @InvokeTransaction
        public void closePropose(JsonObject param) { // Close the proposal if it is issued by the proposal issuer
            String proposeIdParam = param.get("proposeId").getAsString();
            ProposeInterChain propose = getPropose(proposeIdParam);
            ProposeStatus proposeStatus = getProposeStatus(proposeIdParam);
            // Check block height
            if (this.txReceipt.getBlockHeight() < propose.getBlockHeight()) {
                throw new RuntimeException("The proposal has not expired");
            }
            // Check propose status
            if (proposeStatus == ProposeStatus.CLOSED) {
                throw new RuntimeException("The proposal already CLOSED");
            }
            // Check issuer or validator
            if (!propose.getIssuer().equals(this.txReceipt.getIssuer())) {
                throw new RuntimeException("Transaction issuer is not the proposal issuer");
            }
            proposeProcessDone(propose, ProposeStatus.CLOSED, BigInteger.ZERO);
        }

        @InvokeTransaction
        public void transactionConfirm(JsonObject param) { // Validator can validate other network transaction
            // TODO validator or truth node can validate transaction confirm

            // Validator is required for transaction confirmation
            if (!this.branchStateStore.isValidator(this.txReceipt.getIssuer())) {
                throw new RuntimeException("Transaction Confirm is require Validator");
            }

            // Get Transaction Confirm by id
            String txConfirmId = param.get("txConfirmId").getAsString();
            TxConfirmStatus status = TxConfirmStatus.fromValue(param.get("status").getAsInt());

            long blockHeight = param.get("blockHeight").getAsLong();
            int index = param.get("index").getAsInt();
            long lastBlockHeight = param.get("lastBlockHeight").getAsLong();

            TxConfirm txConfirm = getTxConfirm(txConfirmId);

            log.debug("TxConfirm ProposeId :  {}", txConfirm.getProposeId());
            log.debug("TxConfirm Status : {}", txConfirm.getStatus());

            // Get confirm
            if (txConfirm.getStatus() == TxConfirmStatus.VALIDATE_REQUIRE
                    || txConfirm.getStatus() == TxConfirmStatus.NOT_EXIST) {

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
                        if (pi.getNetworkBlockHeight() <= txConfirm.getBlockHeight()) {
                            checkBlockHeight = true;
                        } else if (pi.getNetworkBlockHeight() >= txConfirm.getLastBlockHeight()) {
                            checkBlockHeight = true;
                        }

                        log.debug("checkBlockHeight : {}", checkBlockHeight);
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
                            log.debug("stake YEED {}", stakeYeed);
                            log.debug("TransferYeed YEED {}", txConfirm.getTransferYeed());
                            log.debug("PI Fee {}", fee);
                            // Send transaction confirm
                            boolean transfer = transfer(pi.getProposeId(), txConfirm.getSenderAddress(),
                                    transferYeed, BigInteger.ZERO);
                            if (transfer) {
                                this.txReceipt.addLog(String.format("%s is DONE",txConfirm.getTxConfirmId()));
                                // check propose
                                if (fee.compareTo(BigInteger.ZERO) > 0) { // propose is done
                                    setProposeStatus(pi.getProposeId(), ProposeStatus.DONE);
                                }
                                this.txReceipt.setStatus(ExecuteStatus.SUCCESS);
                                // Save Tx Confirm
                                txConfirm.setStatus(TxConfirmStatus.DONE);
                                // Save TxConfirm
                                setTxConfirm(txConfirm);
                                // propose is done
                                if (stakeYeed.compareTo(transferYeed) == 0) {
                                    proposeProcessDone(pi, ProposeStatus.DONE, BigInteger.ZERO);
                                }
                            } else {
                                this.txReceipt.addLog(String.format("%s is FAIL",txConfirm.getTxConfirmId()));
                                this.txReceipt.setStatus(ExecuteStatus.FALSE);
                            }

                        }
                    }
                } else {
                    // Propose Status is not processing
                    setFalseTxReceipt(String.format("%s is %s", pi.getProposeId(), pis.toString()));
                }
            } else {
                setFalseTxReceipt(String.format("%s is %s", txConfirm.getTxConfirmId(),
                        txConfirm.getStatus().toString()));
            }
        }

        @ContractQuery
        public JsonObject queryTransactionConfirm(JsonObject param) {
            String transactionConfirmId = param.get("txConfirmId").getAsString();
            TxConfirm confirm = getTxConfirm(transactionConfirmId);
            return confirm.toJsonObject();
        }

        public TxConfirm getTxConfirm(String txConfirmId) {
            String txConfirmKey = String.format("%s%s", PrefixKeyEnum.TRANSACTION_CONFIRM.toValue(),
                    txConfirmId
            );
            JsonObject txConfirm = this.store.get(txConfirmKey);
            return new TxConfirm(txConfirm);
        }

        public boolean isExistTxConfirm(String txConfirmId) {
            String txConfirmKey = String.format("%s%s", PrefixKeyEnum.TRANSACTION_CONFIRM.toValue(),
                    txConfirmId
            );
            return this.store.contains(txConfirmKey);
        }

        private void setTxConfirm(TxConfirm txConfirm) {
            String txConfirmKey = String.format("%s%s", PrefixKeyEnum.TRANSACTION_CONFIRM.toValue(),
                    txConfirm.getTxConfirmId()
            );
            this.store.put(txConfirmKey, txConfirm.toJsonObject());
        }

        @InvokeTransaction
        public void faucet(JsonObject param) { // THIS IS FAUCET IN TEST NET!!
            String issuer = this.txReceipt.getIssuer();
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

        private void setErrorTxReceipt(String msg) {
            this.txReceipt.setStatus(ExecuteStatus.ERROR);
            this.txReceipt.addLog(msg);
        }

        private void setFalseTxReceipt(String msg) {
            this.txReceipt.setStatus(ExecuteStatus.FALSE);
            this.txReceipt.addLog(msg);
        }

        private void setFalseTxReceipt(List<String> msgs) {
            this.txReceipt.setStatus(ExecuteStatus.FALSE);
            msgs.forEach(l -> this.txReceipt.addLog(l));
        }

        private void setSuccessTxReceipt(String msg) {
            this.txReceipt.setStatus(ExecuteStatus.SUCCESS);
            this.txReceipt.addLog(msg);
        }

        private void setSuccessTxReceipt(List<String> msgs) {
            this.txReceipt.setStatus(ExecuteStatus.SUCCESS);
            msgs.forEach(l -> this.txReceipt.addLog(l));
        }

    }
}
