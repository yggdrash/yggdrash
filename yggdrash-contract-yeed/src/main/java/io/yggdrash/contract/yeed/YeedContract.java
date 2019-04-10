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
import io.yggdrash.common.utils.ByteUtil;
import io.yggdrash.contract.core.ExecuteStatus;
import io.yggdrash.contract.core.TransactionReceipt;
import io.yggdrash.contract.core.annotation.ContractQuery;
import io.yggdrash.contract.core.annotation.ContractStateStore;
import io.yggdrash.contract.core.annotation.ContractTransactionReceipt;
import io.yggdrash.contract.core.annotation.Genesis;
import io.yggdrash.contract.core.annotation.InvokeTransaction;
import io.yggdrash.contract.core.annotation.ParamValidation;
import io.yggdrash.contract.core.store.ReadWriterStore;
import io.yggdrash.contract.yeed.ehtereum.EthTransaction;
import io.yggdrash.contract.yeed.propose.ProposeInterChain;
import io.yggdrash.contract.yeed.propose.ProposeStatus;
import io.yggdrash.contract.yeed.propose.ProposeType;
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
            log.debug("\ntransfer :: params => {}", params);

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
                txReceipt.addLog(String.format("{} Amount not enought", from));
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
                txReceipt.addLog(String.format("{} transfer Error", from));
                return false;
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
            log.debug("\napprove :: params => {}", params);

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
            log.debug("\ntransferFrom :: params => {}", params);

            String from = params.get("from").getAsString().toLowerCase();
            String to = params.get("to").getAsString().toLowerCase();

            String sender = txReceipt.getIssuer();
            String approveKey = approveKey(from, sender);
            log.debug("approve Key : {}", approveKey);
            if (getBalance(approveKey).compareTo(BigInteger.ZERO) == 0) {
                txReceipt.addLog(String.format("{} has no balance", from));
                txReceipt.setStatus(ExecuteStatus.ERROR);
                return txReceipt;
            }
            // check from amount
            BigInteger fromValue = getBalance(from);
            BigInteger approveValue = getBalance(approveKey);
            BigInteger amount = params.get(AMOUNT).getAsBigInteger();
            BigInteger fee = params.has(FEE) ? params.get(FEE).getAsBigInteger() : BigInteger.ZERO;
            BigInteger amountFee = amount.add(fee);

            // TODO Check Fee
            if (isTransferable(fromValue, amountFee) && isTransferable(approveValue, amountFee)) {
                // Transfer Coin
                boolean isTransfer = transfer(from, to, amount, fee);
                if (isTransfer) {
                    approveValue = approveValue.subtract(amountFee);
                    putBalance(approveKey, approveValue);
                }

                txReceipt.setStatus(isTransfer ? ExecuteStatus.SUCCESS : ExecuteStatus.ERROR);
                log.debug("\n[Transferred] Transfer {} from {} to {}", amount, from, to);
                log.debug("\nAllowed amount of Sender ({}) : {}", sender, approveValue);
                log.debug("\nBalance of From ({}) : {}"
                        + "\nBalance of To   ({}) : {}", from, fromValue, to, getBalance(to));
            } else {
                txReceipt.addLog(String.format("%s has not enough balance", from));
                txReceipt.setStatus(ExecuteStatus.FALSE);
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
                log.debug("\nAddress of Frontier : {}"
                        + "\nBalance of Frontier : {}", frontier, getBalance(frontier));
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

        public BigInteger getBalance(String address) {
            address = PrefixKeyEnum.getAccountKey(address);
            JsonObject storeValue = store.get(address);
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
            return String.format("%s%s", PrefixKeyEnum.APPROVE.toValue(),Hex.toHexString(approveKey));
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
            // TODO Check Issuer FEE and network FEE

            // check Issuer has YEED
            BigInteger stakeFee = stakeYeed.add(fee);
            BigInteger balance = getBalance(issuer);
            if (balance.compareTo(stakeFee) < 0) {
                this.txReceipt.setStatus(ExecuteStatus.ERROR);
                throw new RuntimeException(String.format("%s has not enough balance", issuer));
            }


            String txId = this.txReceipt.getTxId();
            String receiveAddress = params.get("receiveAddress").getAsString();
            BigInteger receiveEth = params.get("receiveEth").getAsBigInteger();
            Integer receiveChainId = params.get("receiveChainId").getAsInt();
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
            ProposeInterChain propose = new ProposeInterChain(txId, receiveAddress,
                    receiveEth, receiveChainId, proposeType, senderAddress, inputData, stakeYeed,
                    target, fee, issuer);

            String proposeIdKey = String.format("%s%s", PrefixKeyEnum.PROPOSE_INTER_CHAIN.toValue(),
                    propose.getProposeId());

            // Fee is send to Later
            transfer(issuer, propose.getProposeId(), stakeFee, BigInteger.ZERO);
            this.txReceipt.setStatus(ExecuteStatus.SUCCESS);

            log.debug(propose.toJsonObject().toString());
            // Save propose
            this.store.put(proposeIdKey, propose.toJsonObject());
            //String proposeStatusKey =
            // Save propose Status
            setProposeStatus(propose.getProposeId(), ProposeStatus.ISSUED);

            this.txReceipt.addLog(String.format("Propose %s ISSUED", propose.getProposeId()));
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

        private ProposeInterChain getPropose(String proposeId) {
            String proposeIdKey = String.format("%s%s", PrefixKeyEnum.PROPOSE_INTER_CHAIN.toValue(),
                    proposeId);
            JsonObject proposal = this.store.get(proposeIdKey);
            return new ProposeInterChain(proposal);
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

            if (!propose.getIssuer().equals(this.txReceipt.getIssuer())) {
                // TODO check fee
                if (fee.compareTo(BigInteger.ZERO) < 0) {
                    throw new RuntimeException("fee required");
                }
            }
            log.debug("issuer Check");
            // issuer Check
            if (propose.getProposeType() == ProposeType.YEED_TO_ETHER) {
                byte[] etheSendEncode = HexUtil.hexStringToBytes(rawTransaction);
                EthTransaction ethTransaction = new EthTransaction(etheSendEncode);
                // check propose
                boolean checkPropose = true;
                String senderAddress = HexUtil.toHexString(ethTransaction.getSendAddress());
                String receiveAddress = HexUtil.toHexString(ethTransaction.getReceiveAddress());
                checkPropose &= propose.getReceiveAddress().equals(receiveAddress);
                checkPropose &= propose.getReceiveChainId() == ethTransaction.getChainId();
                // TODO sender is option
                checkPropose &= propose.getSenderAddress().equals(senderAddress);

                log.debug("check Propose : {}", checkPropose);

                if (checkPropose) {
                    BigInteger receiveEth = ethTransaction.getValue();
                    // calculate ratio
                    BigInteger ratio = propose.getReceiveEth().divide(propose.getStakeYeed());

                    BigInteger transferYeed = ratio.multiply(receiveEth);
                    BigInteger stakeBalance = getBalance(propose.getProposeId());
                    stakeBalance = stakeBalance.subtract(propose.getFee());
                    // balance check if transferYeed over stakeBalance, set stakeBalance
                    if (stakeBalance.compareTo(transferYeed) < 0) {
                        transferYeed = stakeBalance;
                    }

                    // issuer
                    if (propose.getIssuer().equals(this.txReceipt.getIssuer())) {
                        transfer(propose.getProposeId(), senderAddress, transferYeed, fee);
                        stakeBalance = stakeBalance.subtract(transferYeed);
                        // All stake YEED is transfer to sendAddress
                        if (stakeBalance.compareTo(BigInteger.ZERO) <= 0) {
                            setProposeStatus(propose.getProposeId(), ProposeStatus.DONE);
                            this.txReceipt.addLog(String.format("propose %s %s", propose.getProposeId(), ProposeStatus.DONE));
                            BigInteger proposeFee = propose.getFee();
                            // send network fee to branch
                            if(receiveEth.compareTo(propose.getReceiveEth()) == 0) {
                                BigInteger returnFee = proposeFee.divide(BigInteger.valueOf(2L));
                                proposeFee = proposeFee.subtract(returnFee);
                                transfer(propose.getProposeId(), propose.getIssuer(), returnFee, BigInteger.ZERO);
                                transfer(propose.getProposeId(), this.txReceipt.getBranchId(), proposeFee, BigInteger.ZERO);
                            }
                        } else {
                            setProposeStatus(propose.getProposeId(), ProposeStatus.PROCESSING);
                            this.txReceipt.addLog(String.format("propose %s %s",propose.getProposeId(), ProposeStatus.PROCESSING));
                        }
                    } else {
                        // check validator
                        // TODO Save Transaction confirm
                        // Transaction ID , propose ID, SendAddress, transfer YEED
                        // Save transaction
                        this.txReceipt.addLog(String.format("propose %s check %s network %s transaction %s",
                                propose.getProposeId(), propose.getProposeType(), ethTransaction.getChainId(),
                                HexUtil.toHexString(ethTransaction.getTxHash())));
                        setProposeStatus(propose.getProposeId(), ProposeStatus.PROCESSING);
                        this.txReceipt.addLog(String.format("propose %s %s",propose.getProposeId(), ProposeStatus.PROCESSING));
                    }


                    this.txReceipt.setStatus(ExecuteStatus.SUCCESS);

                } else {
                    this.txReceipt.setStatus(ExecuteStatus.FALSE);
                }


            } else {
                throw new RuntimeException("");
            }
            // or any other address can process propose
            // param get
        }

        // propose close
        @InvokeTransaction
        public void closePropose(JsonObject param) {
            String proposeIdParam = param.get("proposeId").getAsString();
            ProposeInterChain propose = getPropose(proposeIdParam);
            ProposeStatus proposeStatus = getProposeStatus(proposeIdParam);
            // block height check
            if (this.txReceipt.getBlockHeight() < propose.getBlockHeight()) {
                throw new RuntimeException("propose is not expired");
            }
            // propose status check
            if (proposeStatus != ProposeStatus.ISSUED) {
                throw new RuntimeException("propose is not issued");
            }
            // issuer check
            if (!propose.getIssuer().equals(this.txReceipt.getIssuer())) {
                throw new RuntimeException("issuer is not propose issuer");
            }
            // fee is not return
            // TODO pay fee to branch
            BigInteger proposeStakeYeed = getBalance(propose.getProposeId()).subtract(propose.getFee());
            // return issuer
            transfer(propose.getProposeId(), this.txReceipt.getIssuer(), proposeStakeYeed, propose.getFee());
            setProposeStatus(propose.getProposeId(), ProposeStatus.CLOSED);
            txReceipt.setStatus(ExecuteStatus.SUCCESS);
            txReceipt.addLog(String.format("propose %s closed", propose.getProposeId()));
        }

        // TODO interTransfer TOKEN to YEED

        // TODO validator or truth node can validate propose

    }
}
