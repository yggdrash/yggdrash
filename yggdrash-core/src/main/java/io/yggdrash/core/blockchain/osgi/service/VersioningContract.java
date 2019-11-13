/*
 * Copyright 2019 Akashic Foundation
 * Licensed under the Apache License, Version 2.0 (the “License”);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an “AS IS” BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package io.yggdrash.core.blockchain.osgi.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.common.store.BranchStateStore;
import io.yggdrash.common.utils.JsonUtil;
import io.yggdrash.contract.core.ContractEvent;
import io.yggdrash.contract.core.ExecuteStatus;
import io.yggdrash.contract.core.Receipt;
import io.yggdrash.contract.core.annotation.ContractBranchStateStore;
import io.yggdrash.contract.core.annotation.ContractEndBlock;
import io.yggdrash.contract.core.annotation.ContractQuery;
import io.yggdrash.contract.core.annotation.ContractReceipt;
import io.yggdrash.contract.core.annotation.ContractStateStore;
import io.yggdrash.contract.core.annotation.Genesis;
import io.yggdrash.contract.core.annotation.InvokeTransaction;
import io.yggdrash.contract.core.channel.ContractEventType;
import io.yggdrash.contract.core.store.ReadWriterStore;
import io.yggdrash.core.blockchain.osgi.ContractConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;


public class VersioningContract {

    private static final Logger log = LoggerFactory.getLogger(VersioningContract.class);

    private static final String LOG_PREFIX = "[Versioning Contract]";
    private static final String TX_ID = "txId";
    private static final String PROPOSAL_VERSION = "proposalVersion";
    private static final String SOURCE_URL = "sourceUrl";
    private static final String BUILD_VERSION = "buildVersion";
    private static final String PROPOSAL_TYPE = "proposalType";
    private static final String VOTE_PERIOD = "votePeriod";
    private static final String APPLY_PERIOD = "applyPeriod";

    @ContractStateStore
    ReadWriterStore<String, JsonObject> state;

    @ContractBranchStateStore
    BranchStateStore branchStore;

    @ContractReceipt
    Receipt receipt;

    @ContractQuery
    public ContractProposal proposalStatus(JsonObject params) {
        return getProposal(params.get(TX_ID).getAsString());
    }

    @Genesis
    @InvokeTransaction
    public Receipt init(JsonObject params) {
        log.info("Init VersioningContract");
        return receipt;
    }

    @InvokeTransaction
    public Receipt propose(JsonObject params) {
        // Verify that the issuer is a validator
        if (!isValidator()) {
            log.warn("{} Validator verification failed in TX {}.", LOG_PREFIX, receipt.getTxId());
            setFalseTxReceipt("Validator verification failed");
            return receipt;
        }

        // Create a contract proposal with parameters.
        String txId = receipt.getTxId();
        String proposer = receipt.getIssuer();
        long blockHeight = receipt.getBlockHeight();
        String proposalVersion = params.get(PROPOSAL_VERSION).getAsString();
        String sourceUrl = params.get(SOURCE_URL).getAsString();
        String buildVersion = params.get(BUILD_VERSION).getAsString();
        long votePeriod = params.get(VOTE_PERIOD).getAsLong();
        long applyPeriod = params.get(APPLY_PERIOD).getAsLong();
        Set<String> validatorSet = new HashSet<>(branchStore.getValidators().getValidatorMap().keySet());
        String proposalType = params.get(PROPOSAL_TYPE).getAsString().toUpperCase();

        if (!isSha1Hash(proposalVersion)) {
            log.warn("{} Proposal Version is not validate in TX {}.", LOG_PREFIX, txId);
            setFalseTxReceipt("Proposal Version is not validate.");
            return receipt;
        }

        if (ProposalType.findBy(proposalType) == null) {
            log.warn("{} Proposal Type is not validate in TX {}.", LOG_PREFIX, txId);
            setFalseTxReceipt("Proposal Type is not validate.");
            return receipt;
        }

        // blockHeight => targetBlockHeight
        ContractProposal proposal = new ContractProposal(
                txId, proposer, proposalVersion, sourceUrl, buildVersion,
                blockHeight, votePeriod, applyPeriod, validatorSet, proposalType);

        // The proposer automatically votes to agree
        proposal.vote(proposer, true);

        JsonObject proposalObj = JsonUtil.parseJsonObject(proposal);

        setProposalToTargetBlockHeight(txId, proposal);

        setSuccessTxReceipt("Contract proposal has been issued");
        log.info("Contract Proposal : txId = {}, proposal = {}", txId, proposalObj);

        return receipt;
    }

    @InvokeTransaction
    public Receipt vote(JsonObject params) {
        // Verify that the issuer is a validator
        if (!isValidator()) {
            setFalseTxReceipt("Validator verification failed");
            return receipt;
        }

        String txId = params.get("txId").getAsString();
        ContractProposal proposal = getProposal(txId);
        // Verify the proposal exists
        if (proposal == null) {
            setFalseTxReceipt("Contract proposal not found");
            return receipt;
        }

        long curBlockHeight = receipt.getBlockHeight();
        // Verify the proposal is expired
        if (proposal.isExpired(curBlockHeight)) {
            setFalseTxReceipt("Contract proposal has already expired");
            proposal.setVotingStatus(VotingProgress.VotingStatus.EXPIRED);
            return receipt;
        }

        String issuer = receipt.getIssuer();
        // Verify if the validator has already voted
        if (proposal.hasAlreadyVoted(issuer)) {
            setFalseTxReceipt("Validator has already voted");
            return receipt;
        }

        boolean agree = params.get("agree").getAsBoolean();
        proposal.vote(issuer, agree);

        setProposalToTargetBlockHeight(txId, proposal);

        setSuccessTxReceipt("Update proposal voting is in progress");

        return receipt;
    }

    @ContractEndBlock
    public Receipt endBlock() {
        // Check if an event of the current block height exists in the StateStore.
        String currentBlockHeight = String.valueOf(receipt.getBlockHeight());
        if (state.contains(currentBlockHeight)) {
            JsonObject txIdArrObj = state.get(currentBlockHeight);
            log.debug("EndBlock : Proposal TxHashes -> {} " , txIdArrObj.toString());
            JsonArray txIdArr = txIdArrObj.get(TX_ID).getAsJsonArray();

            for (int i = 0; i < txIdArr.size(); i++) {
                String txId = txIdArr.get(i).getAsString();
                JsonObject proposalJson = state.get(txId);
                ContractProposal proposal = JsonUtil.generateJsonToClass(proposalJson.toString(), ContractProposal.class);
                setContractEvent(txId, proposal);
            }
        }
        return receipt;
    }

    private void setContractEvent(String txId, ContractProposal proposal) {
        VotingProgress.VotingStatus votingStatus = proposal.getVotingProgress().getVotingStatus();

        switch (votingStatus) {
            case VOTEABLE:
                log.debug("the vote not end. It will be expired. tx id : {}", proposal.getTxId());
                proposal.setVotingStatus(VotingProgress.VotingStatus.EXPIRED);
                state.put(proposal.getTxId(), JsonUtil.parseJsonObject(proposal));
                setSuccessTxReceipt(
                        String.format("%s Expire proposal tx id : %s", LOG_PREFIX, proposal.getTxId()));
                break;
            case AGREE:
                log.debug("The vote ends with agreed. It will be updated.");
                ContractEvent agreeEvent =
                        new ContractEvent(
                                ContractEventType.AGREE, proposal,
                                ContractConstants.VERSIONING_CONTRACT.toString());

                proposal.setVotingStatus(VotingProgress.VotingStatus.APPLYING);

                setProposalToApplyBlockHeight(proposal.getTxId(), proposal);

                setSuccessTxReceipt(
                        String.format("%s The vote ends up with agreed. tx id : %s", LOG_PREFIX, txId)
                );

                receipt.addEvent(agreeEvent);
                break;
            case DISAGREE:
                log.debug("The vote end with disagreed.");
                setSuccessTxReceipt(
                        String.format("%s The vote ends up with disagreed. tx id : %s", LOG_PREFIX, txId)
                );
                break;
            case APPLYING:
                log.debug("The proposal is applying.");
                ContractEvent applyEvent = new ContractEvent(
                        ContractEventType.APPLY, proposal, ContractConstants.VERSIONING_CONTRACT.toString());

                setSuccessTxReceipt(
                        String.format("%s The proposal is applying. tx id : %s", LOG_PREFIX, txId)
                );

                receipt.addEvent(applyEvent);
                break;
            default:
                setFalseTxReceipt("The vote is in unknown status.");
                log.warn("The vote is in unknown status.");
                break;
        }
    }

    private ContractProposal getProposal(String txId) {
        JsonObject proposalObj = state.get(txId);
        return proposalObj != null
                ? JsonUtil.generateJsonToClass(proposalObj.toString(), ContractProposal.class) : null;
    }

    private boolean isValidator() {
        return branchStore.getValidators() != null && branchStore.getValidators().getValidatorMap().keySet().stream()
                .anyMatch(key -> !key.isEmpty() && key.equals(receipt.getIssuer()));
    }

    private void setProposalToTargetBlockHeight(String txId, ContractProposal proposal) {
        // Store the proposal in stateStore
        saveProposal(txId, proposal);
        // Store the txId in endblock store
        addTxIdToBlockHeight(String.valueOf(proposal.getTargetBlockHeight()), txId);
    }

    private void setProposalToApplyBlockHeight(String txId, ContractProposal proposal) {
        // Store the proposal in stateStore
        saveProposal(txId, proposal);
        // Store the txId in endblock store
        addTxIdToBlockHeight(String.valueOf(proposal.getApplyBlockHeight()), txId);
    }

    private void saveProposal(String txId, ContractProposal proposal) {
        state.put(txId, JsonUtil.parseJsonObject(proposal));
    }

    private void addTxIdToBlockHeight(String blockHeight, String txId) {
        state.put(blockHeight, createTxIdArrObj(blockHeight, txId));
    }

    private JsonObject createTxIdArrObj(String blockHeight, String txId) {
        JsonObject txIdArrObj = new JsonObject();
        JsonArray txIdArr = new JsonArray();
        if (state.contains(blockHeight)) {
            txIdArr = state.get(blockHeight).get(TX_ID).getAsJsonArray();
        }
        if (!txIdArr.toString().contains(txId)) {
            txIdArr.add(txId);
        }
        txIdArrObj.add(TX_ID, txIdArr);
        return txIdArrObj;
    }

    private boolean isSha1Hash(String proposalVersion) {
        // Mark: add match fail reason. @lucas. @190917
        Pattern pattern = Pattern.compile("\\b[0-9a-f]{5,40}\\b");
        return pattern.matcher(proposalVersion).matches();
    }

    private void setFalseTxReceipt(String msg) {
        this.receipt.setStatus(ExecuteStatus.FALSE);
        this.receipt.addLog(msg);
    }

    private void setSuccessTxReceipt(String msg) {
        this.receipt.setStatus(ExecuteStatus.SUCCESS);
        this.receipt.addLog(msg);
    }

}
