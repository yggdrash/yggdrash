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
import io.yggdrash.common.contract.ContractVersion;
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
        log.info("Start Yeed contract");
        //Find for service in another bundle
        Hashtable<String, String> props = new Hashtable<>();
        props.put("YGGDRASH", "Yeed");
        context.registerService(YeedService.class.getName(), new YeedService(), props);
        // getBundle and wire this service
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
        @ParamValidation
        @Override
        public BigInteger totalSupply() {
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
            String address = params.get("address").getAsString();
            return getBalance(address);
        }

        @ContractQuery
        public BigInteger balanceOfContract(JsonObject params) {
            String contract = params.get("contract").getAsString();
            return getBalance(PrefixKeyEnum.CONTRACT_ACCOUNT, contract);
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
            log.debug("allowance : params => {}", params);
            String owner = params.get("owner").getAsString();
            String spender = params.get("spender").getAsString();
            String approveKey = approveKey(owner, spender);

            return getBalance(approveKey);
        }

        // check transfer fee
        public BigInteger checkFee(JsonObject params) {
            // TODO FeeModel
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
            log.debug("transfer :: params => {}", params);

            String to = params.get("to").getAsString().toLowerCase();
            BigInteger amount = params.get(AMOUNT).getAsBigInteger();

            String from = this.txReceipt.getIssuer();

            /*
            if (getBalance(sender).compareTo(BigInteger.ZERO) == 0) {
                txReceipt.addLog(sender + " has no balance");
                return txReceipt;
            }
            */

            // check fee but now is allow 0
            BigInteger fee = params.has(FEE) ? params.get(FEE).getAsBigInteger() : BigInteger.ZERO;
            // amount, fee check
            if (amount.compareTo(BigInteger.ZERO) <= 0 || fee.compareTo(BigInteger.ZERO) < 0) {
                txReceipt.addLog(String.format("{} Amount not enough", from));
                return txReceipt;
            }
            log.debug("transfer {} {} {} {}",from, to, amount, fee);
            boolean isTransfer = transfer(from, to, amount, fee);

            txReceipt.setStatus(isTransfer ? ExecuteStatus.SUCCESS : ExecuteStatus.FALSE);

            if (log.isDebugEnabled() && isTransfer) {
                log.debug("[Transferred] Transfer {} from {} to {}", amount, from, to);
                log.debug("Balance of From ({}) : {} To ({}) : ", from, getBalance(from), to, getBalance(to));
            }
            return txReceipt;
        }


        @ContractChannelMethod
        public boolean transferChannel(JsonObject params) {
            // call other contract to transfer

            // contract Name base
            String otherContract = this.txReceipt.getContractVersion();
            String contractName = this.branchStateStore.getContractName(otherContract);

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

        // Transfer A to B include fee
        protected boolean transfer(String from, String to, BigInteger amount, BigInteger fee) {
            BigInteger fromBalance = getBalance(from);
            BigInteger feeAmount = amount.add(fee);

            // check from account balance
            if (isTransferable(fromBalance, feeAmount)) {
                fromBalance = fromBalance.subtract(feeAmount);
                addBalanceTo(to, amount);
                putBalance(from, fromBalance);
                // fee account
                addBalanceTo(txReceipt.getBranchId(), fee);
                txReceipt.addLog(String.format("Transfer from %s to %s value %s fee %s ",
                        from, to, amount, fee));
                return true;
            } else {
                txReceipt.addLog(String.format("%s transfer Error", from));
                return false;
            }
        }

        protected boolean transferFee(String from, BigInteger fee) {
            if (fee.compareTo(BigInteger.ZERO) > 0) {
                return transfer(from, from, BigInteger.ZERO, fee);
            } else {
                return true;
            }

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
            log.debug("approve :: params => {}", params);

            String spender = params.get("spender").getAsString().toLowerCase();
            BigInteger amount = params.get(AMOUNT).getAsBigInteger();
            BigInteger fee = params.has(FEE) ? params.get(FEE).getAsBigInteger() : BigInteger.ZERO;

            String sender = txReceipt.getIssuer();

            if (getBalance(sender).compareTo(BigInteger.ZERO) <= 0) {
                txReceipt.addLog(String.format("%s has no balance.", sender));
                txReceipt.setStatus(ExecuteStatus.ERROR);
                return txReceipt;
            }
            // Check Fee
            // spend fee
            if (fee.compareTo(BigInteger.ZERO) != 0) {
                transfer(sender, sender, BigInteger.ZERO, fee);
            }

            BigInteger senderBalance = getBalance(sender);

            // Approve not check balance
            if (isTransferable(senderBalance, amount)) {
                String approveKey = approveKey(sender, spender);
                putBalance(approveKey, amount);
                log.debug("approve Key : {}", approveKey);

                txReceipt.setStatus(ExecuteStatus.SUCCESS);
                log.debug("[Approved] Approve {} to {} from {}", spender, getBalance(approveKey), sender);
            } else {
                log.debug("[ERR] {} has no enough balance!", sender);
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
            log.debug("transferFrom :: params => {}", params);

            String from = params.get("from").getAsString().toLowerCase();
            String to = params.get("to").getAsString().toLowerCase();

            String sender = txReceipt.getIssuer();
            String approveKey = approveKey(from, sender);
            log.debug("approve Key : {}", approveKey);
            BigInteger senderBalance = getBalance(sender);
            if (getBalance(approveKey).compareTo(BigInteger.ZERO) == 0) {
                txReceipt.addLog(String.format("%s has no balance", from));
                txReceipt.setStatus(ExecuteStatus.ERROR);
                return txReceipt;
            }
            // check from amount
            BigInteger fromValue = getBalance(from);
            BigInteger approveValue = getBalance(approveKey);
            BigInteger amount = params.get(AMOUNT).getAsBigInteger();
            BigInteger fee = params.has(FEE) ? params.get(FEE).getAsBigInteger() : BigInteger.ZERO;
            BigInteger amountFee = amount.add(fee);

            // Check Fee
            if (senderBalance.compareTo(fee) < 0) {
                txReceipt.addLog(String.format("%s has no balance", from));
                txReceipt.setStatus(ExecuteStatus.ERROR);
                return txReceipt;
            }

            if (isTransferable(fromValue, amountFee) && isTransferable(approveValue, amountFee)) {
                // Transfer Coin
                boolean isTransfer = transfer(from, to, amount, fee);
                if (isTransfer) {
                    approveValue = approveValue.subtract(amountFee);
                    putBalance(approveKey, approveValue);
                }

                txReceipt.setStatus(isTransfer ? ExecuteStatus.SUCCESS : ExecuteStatus.FALSE);
                log.debug("[Transferred] Transfer {} from {} to {}", amount, from, to);
                log.debug("Allowed amount of Sender ({}) : {}", sender, approveValue);
                log.debug("Balance of From ({}) : {} Balance of To   ({}) : {}", from, fromValue,
                        to, amount);
            } else {
                txReceipt.addLog(String.format("%s has not enough balance", from));
                txReceipt.setStatus(ExecuteStatus.ERROR);
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
            log.debug("genesis :: params => {}", params);

            //totalSupply 는 alloc 의 balance 를 모두 더한 값으로 세팅
            BigInteger totalSupply = BigInteger.ZERO;
            JsonObject alloc = params.getAsJsonObject("alloc");
            for (Map.Entry<String, JsonElement> entry : alloc.entrySet()) {
                String frontier = entry.getKey();
                JsonObject value = entry.getValue().getAsJsonObject();
                BigInteger balance = value.get(BALANCE).getAsBigInteger();
                totalSupply = totalSupply.add(balance);
                addBalanceTo(frontier, balance);

                putBalance(frontier, balance);

                JsonObject mintLog = new JsonObject();
                mintLog.addProperty("to", frontier);
                mintLog.addProperty(BALANCE, balance.toString());
                txReceipt.addLog(mintLog.toString());
                log.debug("Address of Frontier : {}"
                        + "Balance of Frontier : {}", frontier, getBalance(frontier));
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
            if (storeValue != null && storeValue.has(BALANCE)) {
                return storeValue.get(BALANCE).getAsBigInteger();
            } else {
                return BigInteger.ZERO;
            }
        }


        private void putBalance(String address, BigInteger value) {
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

        private boolean isTransferable(BigInteger targetBalance, BigInteger amount) {
            // same is  0, more is 1
            return targetBalance.subtract(amount).compareTo(BigInteger.ZERO) >= 0;
        }

        // TODO withdraw fee amount by branchId
        // Issue InterTransfer Eth(or Token) To YEED
        @InvokeTransaction
        public void issuePropose(JsonObject params) {
            BigInteger stakeYeed = params.get("stakeYeed").getAsBigInteger();
            BigInteger fee = params.get(FEE).getAsBigInteger();
            String issuer = this.txReceipt.getIssuer();
            // Check Issuer FEE and network FEE

            if (stakeYeed.compareTo(BigInteger.ZERO) <= 0 || fee.compareTo(BigInteger.ZERO) <= 0) {
                this.txReceipt.setStatus(ExecuteStatus.FALSE);
                this.txReceipt.addLog("stakeYeed and fee is positive number");
                return;
            }

            // check Issuer has YEED
            BigInteger stakeFee = stakeYeed.add(fee);
            BigInteger balance = getBalance(issuer);
            if (balance.compareTo(stakeFee) < 0) {
                this.txReceipt.setStatus(ExecuteStatus.FALSE);
                this.txReceipt.addLog(String.format("%s has not enough balance", issuer));
                return;
            }


            String txId = this.txReceipt.getTxId();

            // TokenAddress is YEED TO TOKEN
            String tokenAddress = JsonUtil.parseString(params, "tokenAddress", "");
            String receiveAddress = params.get("receiveAddress").getAsString();
            BigInteger receiveAsset = params.get("receiveAsset").getAsBigInteger();
            Integer receiveChainId = params.get("receiveChainId").getAsInt();
            long networkBlockHeight = params.get("networkBlockHeight").getAsLong();
            ProposeType proposeType = ProposeType.fromValue(params.get("proposeType").getAsInt());

            String senderAddress = null;
            String inputData = null;
            if (ProposeType.YEED_TO_ETHER.equals(proposeType)) {
                senderAddress = params.get("senderAddress").getAsString();
                if (!params.get("inputData").isJsonNull()) {
                    inputData = params.get("inputData").getAsString();
                }
            }
            long target = params.get("blockHeight").getAsLong();

            // Issue Propose
            ProposeInterChain propose = new ProposeInterChain(txId, tokenAddress, receiveAddress,
                    receiveAsset, receiveChainId, networkBlockHeight, proposeType, senderAddress, inputData,
                    stakeYeed, target, fee, issuer);

            String proposeIdKey = String.format("%s%s", PrefixKeyEnum.PROPOSE_INTER_CHAIN.toValue(),
                    propose.getProposeId());

            // Fee is send Later (Issue close or done)
            boolean transfer = transfer(issuer, propose.getProposeId(), stakeFee, BigInteger.ZERO);
            if (transfer) {
                this.txReceipt.setStatus(ExecuteStatus.SUCCESS);

                // Save propose
                this.store.put(proposeIdKey, propose.toJsonObject());
                //String proposeStatusKey =
                // Save propose Status
                setProposeStatus(propose.getProposeId(), ProposeStatus.ISSUED);

                this.txReceipt.addLog(String.format("Propose %s ISSUED", propose.getProposeId()));
            } else {
                this.txReceipt.setStatus(ExecuteStatus.FALSE);
                this.txReceipt.addLog(String.format("Propose %s ISSUE Fail", propose.getProposeId()));
            }
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
            log.debug("{}({}) is {}", proposeId, proposeKey, status);
            this.store.put(proposeKey, statusValue);
        }

        public ProposeInterChain getPropose(String proposeId) {
            String proposeIdKey = String.format("%s%s", PrefixKeyEnum.PROPOSE_INTER_CHAIN.toValue(),
                    proposeId);
            JsonObject proposal = this.store.get(proposeIdKey);
            return new ProposeInterChain(proposal);
        }

        public TxConfirm getTxConfirm(String txConfirmId) {
            String txConfirmKey = String.format("%s%s",PrefixKeyEnum.TRANSACTION_CONFIRM.toValue(),
                    txConfirmId
                    );
            JsonObject txConfirm = this.store.get(txConfirmKey);
            return new TxConfirm(txConfirm);
        }

        public boolean isExistTxConfirm(String txConfirmId) {
            String txConfirmKey = String.format("%s%s",PrefixKeyEnum.TRANSACTION_CONFIRM.toValue(),
                    txConfirmId
            );
            return this.store.contains(txConfirmKey);
        }


        private void setTxConfirm(TxConfirm txConfirm) {

            String txConfirmKey = String.format("%s%s",PrefixKeyEnum.TRANSACTION_CONFIRM.toValue(),
                    txConfirm.getTxConfirmId()
            );
            this.store.put(txConfirmKey, txConfirm.toJsonObject());
        }

        @ContractQuery
        public JsonObject queryTransactionConfirm(JsonObject param) {
            String transactionConfirmId = param.get("txConfirmId").getAsString();
            TxConfirm confirm = getTxConfirm(transactionConfirmId);
            return confirm.toJsonObject();
        }


        // get propse
        @ContractQuery
        public JsonObject queryPropose(JsonObject param) {
            String proposeIdParam = param.get("proposeId").getAsString();
            ProposeInterChain propose = getPropose(proposeIdParam);
            JsonObject proposeJson = propose.toJsonObject();
            proposeJson.addProperty("status", getProposeStatus(proposeIdParam).toString());
            return proposeJson;
        }


        // interTransfer ETH to YEED
        @InvokeTransaction
        public void processPropose(JsonObject param) {

            String proposeIdParam = param.get("proposeId").getAsString();
            String rawTransaction = param.get("rawTransaction").getAsString();
            BigInteger fee = param.get("fee").getAsBigInteger();

            ProposeInterChain propose = getPropose(proposeIdParam);

            // TODO process fee check
            if (!propose.getIssuer().equals(this.txReceipt.getIssuer())) {
                // TODO check fee
                if (fee.compareTo(BigInteger.ZERO) < 0) {
                    throw new RuntimeException("fee required");
                }
            }

            // check propose status
            ProposeStatus proposeStatus = getProposeStatus(propose.getProposeId());
            if (ProposeStatus.ISSUED != proposeStatus && ProposeStatus.PROCESSING != proposeStatus) {
                this.txReceipt.setStatus(ExecuteStatus.FALSE);
                this.txReceipt.addLog("propose can not process");
                transferFee(this.txReceipt.getIssuer(), fee);
                return;
            }

            // check propose Status
            log.debug("issuer Check");
            // issuer Check
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
            // or any other address can process propose
            // param get
        }



        public void processYeedToEth(ProposeInterChain propose, String rawTransaction, BigInteger fee) {
            byte[] etheSendEncode = HexUtil.hexStringToBytes(rawTransaction);
            EthTransaction ethTransaction = new EthTransaction(etheSendEncode);


            String senderAddress = HexUtil.toHexString(ethTransaction.getSendAddress());
            String receiveAddress = HexUtil.toHexString(ethTransaction.getReceiveAddress());

            ProcessTransaction pt = new ProcessTransaction();
            pt.setSendAddress(senderAddress);
            pt.setReceiveAddress(receiveAddress);
            pt.setChainId(ethTransaction.getChainId());
            pt.setAsset(ethTransaction.getValue());
            pt.setTransactionHash(HexUtil.toHexString(ethTransaction.getTxHash()));

            // Ethereum Transaction and Propose verification
            // check propose
            int checkPropose = propose.verificationProposeProcess(pt);

            log.debug("check Propose : {}", checkPropose);
            if (checkPropose == 0) {
                processProposeTransaction(propose, pt);
                // send fee
                transferFee(this.txReceipt.getIssuer(), fee);
                this.txReceipt.setStatus(ExecuteStatus.SUCCESS);
            } else {
                log.error("{} error Code", checkPropose);
                List<String> errors = ProposeErrorCode.errorLogs(checkPropose);
                // add Error log to txReceipt
                errors.stream().forEach(l -> this.txReceipt.addLog(l));
                this.txReceipt.setStatus(ExecuteStatus.FALSE);
            }
        }


        public void processYeedToEthToken(ProposeInterChain propose, String rawTransaction, BigInteger fee) {
            // Check Token
            byte[] etheSendEncode = HexUtil.hexStringToBytes(rawTransaction);
            EthTokenTransaction tokenTransaction = new EthTokenTransaction(etheSendEncode);

            String senderAddress = HexUtil.toHexString(tokenTransaction.getSendAddress());
            // input data param[0] == method, param[1] == ReceiveAddress, param[2] == asset
            String receiveAddress = HexUtil.toHexString(tokenTransaction.getParam()[1]);
            BigInteger sendAsset = new BigInteger(tokenTransaction.getParam()[2]);
            String targetAddress = HexUtil.toHexString(tokenTransaction.getReceiveAddress());

            ProcessTransaction pt = new ProcessTransaction();
            pt.setSendAddress(senderAddress);
            pt.setReceiveAddress(receiveAddress);
            pt.setChainId(tokenTransaction.getChainId());
            pt.setTargetAddress(targetAddress);
            pt.setAsset(sendAsset);

            int checkPropose = propose.verificationProposeProcess(pt);

            // TODO Do process
            if (checkPropose == 0) {

                processProposeTransaction(propose, pt);
                // send fee
                transferFee(this.txReceipt.getIssuer(), fee);
                this.txReceipt.setStatus(ExecuteStatus.SUCCESS);
            } else {
                log.error("{} error Code", checkPropose);
                List<String> errors = ProposeErrorCode.errorLogs(checkPropose);
                // add Error log to txReceipt
                errors.stream().forEach(l -> this.txReceipt.addLog(l));
                // send fee
                transferFee(this.txReceipt.getIssuer(), fee);
                this.txReceipt.setStatus(ExecuteStatus.FALSE);
            }
        }

        private void processProposeTransaction(ProposeInterChain propose, ProcessTransaction pt) {
            boolean proposeSender = propose.proposeSender(pt.getSendAddress());

            BigInteger receiveValue = pt.getAsset();
            // calculate ratio
            BigInteger ratio = propose.getReceiveAsset().divide(propose.getStakeYeed());

            BigInteger transferYeed = ratio.multiply(receiveValue);
            BigInteger stakeBalance = getBalance(propose.getProposeId());
            stakeBalance = stakeBalance.subtract(propose.getFee());
            // balance check if transferYeed over stakeBalance, set stakeBalance
            if (stakeBalance.compareTo(transferYeed) < 0) {
                transferYeed = stakeBalance;
            }

            // issuer
            boolean isProposerAreIssuer = propose.getIssuer().equals(this.txReceipt.getIssuer());

            if (isProposerAreIssuer && proposeSender) {
                // 1. propose issuer and this transaction issuer are same
                // 2. Propose set Sender Address
                // 3. Propose Send Address send transaction to receive Address
                transfer(propose.getProposeId(), pt.getSendAddress(), transferYeed, BigInteger.ZERO);
                stakeBalance = stakeBalance.subtract(transferYeed);
                // All stake YEED is transfer to sendAddress
                if (stakeBalance.compareTo(BigInteger.ZERO) <= 0) {
                    // 1. propose issuer and process issuer are same
                    // 2. receive Asset Value is more than propose receiveAsset or equal
                    // 3. Propose set Sender Address
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
                TxConfirm confirm = new TxConfirm(propose.getProposeId(), pt.getTransactionHash(),
                        pt.getSendAddress(), transferYeed);
                // check exist confirm TxId
                processConfirmTx(propose, confirm);
                if (this.txReceipt.getStatus() == ExecuteStatus.SUCCESS) {
                    setProposeStatus(propose.getProposeId(), ProposeStatus.PROCESSING);
                    this.txReceipt.addLog(String.format("propose %s %s", propose.getProposeId(),
                            ProposeStatus.PROCESSING));
                }
            }
        }


        private void processConfirmTx(ProposeInterChain propose, TxConfirm confirm) {
            // check exist confirm TxId
            if (isExistTxConfirm(confirm.getTxConfirmId())) { // confirmTx is Exist
                this.txReceipt.setStatus(ExecuteStatus.FALSE);
                this.txReceipt.addLog(String.format("Propose %s transaction %s exist",
                        propose.getProposeId(), confirm.getTxConfirmId()));
            } else {
                // Save TxConfirm
                setTxConfirm(confirm);
                this.txReceipt.setStatus(ExecuteStatus.SUCCESS);
                this.txReceipt.addLog(String.format("propose %s check %s network %s transaction %s confirm ID %s",
                        propose.getProposeId(), propose.getProposeType(), propose.getReceiveChainId(),
                        confirm.getTxId(), confirm.getTxConfirmId()
                ));
            }
        }

        private void proposeProcessDone(ProposeInterChain propose, ProposeStatus status, BigInteger refundFee) {
            BigInteger stakeBalanace = getBalance(propose.getProposeId());
            BigInteger proposeFee = propose.getFee();
            proposeFee = proposeFee.subtract(refundFee);
            setProposeStatus(propose.getProposeId(), status);
            BigInteger returnStakeBalance = stakeBalanace.subtract(proposeFee);
            transfer(propose.getProposeId(), propose.getIssuer(), returnStakeBalance, proposeFee);
            this.txReceipt.addLog(String.format("propose %s %s", propose.getProposeId(),
                    status));
            this.txReceipt.setStatus(ExecuteStatus.SUCCESS);

        }

        @InvokeTransaction
        public void transactionConfirm(JsonObject param) {
            // validator can validate other network transaction
            // check validate
            // TODO validator or truth node can validate transaction confirm
            if (!this.branchStateStore.isValidator(this.txReceipt.getIssuer())) {
                throw new RuntimeException("Transaction Confirm is require Validator");
            }

            // Get Transaction Confirm
            String txConfirmId = param.get("txConfirmId").getAsString();
            // Transaction Status
            TxConfirmStatus status = TxConfirmStatus.fromValue(param.get("status").getAsInt());

            long blockHeight = param.get("blockHeight").getAsLong();
            int index = param.get("index").getAsInt();
            long lastBlockHeight = param.get("lastBlockHeight").getAsLong();


            TxConfirm txConfirm = getTxConfirm(txConfirmId);

            log.debug("process {}",txConfirm.getProposeId());
            log.debug("status {}",txConfirm.getStatus());

            // Get confirm
            if (txConfirm.getStatus() == TxConfirmStatus.VALIDATE_REQUIRE
                    || txConfirm.getStatus() == TxConfirmStatus.NOT_EXIST) {

                txConfirm.setBlockHeight(blockHeight);
                txConfirm.setIndex(index);
                txConfirm.setLastBlockHeight(lastBlockHeight);


                // get Propose
                ProposeInterChain pi = getPropose(txConfirm.getProposeId());
                ProposeStatus pis = getProposeStatus(txConfirm.getProposeId());
                if (pis == ProposeStatus.PROCESSING) {
                    //  processing propose
                    if (status == TxConfirmStatus.DONE) {
                        txConfirm.setStatus(TxConfirmStatus.DONE);
                        txConfirm.setIndex(index);
                        txConfirm.setBlockHeight(blockHeight);

                        // check block height
                        // Propose Issuer set network block height
                        //
                        log.debug("Propose require block height : {} ", pi.getNetworkBlockHeight());
                        log.debug("txConfirm block height : {} ", txConfirm.getBlockHeight());
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
                            boolean transfer = transfer(pi.getProposeId(), txConfirm.getSendAddress(),
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
                    // Propose Status is not Processing
                    this.txReceipt.setStatus(ExecuteStatus.FALSE);
                    this.txReceipt.addLog(String.format("%s is %s",pi.getProposeId(), pis.toString()));
                }
            } else {
                // Transaction Confirm Is Done
                this.txReceipt.addLog(String.format("%s is %s",txConfirm.getTxConfirmId(),
                        txConfirm.getStatus().toString()));
                this.txReceipt.setStatus(ExecuteStatus.FALSE);
            }
        }

        // propose close
        @InvokeTransaction
        public void closePropose(JsonObject param) {
            // close Propose is issued by propose issuer
            String proposeIdParam = param.get("proposeId").getAsString();
            ProposeInterChain propose = getPropose(proposeIdParam);
            ProposeStatus proposeStatus = getProposeStatus(proposeIdParam);
            // block height check
            if (this.txReceipt.getBlockHeight() < propose.getBlockHeight()) {
                throw new RuntimeException("propose is not expired");
            }
            // propose status check
            if (proposeStatus == ProposeStatus.CLOSED) {
                throw new RuntimeException("propose is CLOSED");
            }
            // issuer or validator check
            if (!propose.getIssuer().equals(this.txReceipt.getIssuer())) {
                throw new RuntimeException("issuer is not propose issuer");
            }
            proposeProcessDone(propose, ProposeStatus.CLOSED, BigInteger.ZERO);
            txReceipt.setStatus(ExecuteStatus.SUCCESS);
        }
    }
}
