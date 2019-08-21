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

import com.google.gson.JsonObject;
import io.yggdrash.common.contract.ContractVersion;
import io.yggdrash.common.store.BranchStateStore;
import io.yggdrash.common.utils.JsonUtil;
import io.yggdrash.contract.core.ExecuteStatus;
import io.yggdrash.contract.core.TransactionReceipt;
import io.yggdrash.contract.core.annotation.ContractBranchStateStore;
import io.yggdrash.contract.core.annotation.ContractQuery;
import io.yggdrash.contract.core.annotation.ContractStateStore;
import io.yggdrash.contract.core.annotation.ContractTransactionReceipt;
import io.yggdrash.contract.core.annotation.InvokeTransaction;
import io.yggdrash.contract.core.store.ReadWriterStore;
import io.yggdrash.core.blockchain.osgi.Downloader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;


public class VersioningContract {

    private static final Logger log = LoggerFactory.getLogger(VersioningContract.class);
    private static final String SUFFIX_TEMP_CONTRACT = "/update-temp-contracts";
    private static final String SUFFIX_CONTRACT = "/.yggdrash/contract";

    @ContractStateStore
    ReadWriterStore<String, JsonObject> state;

    @ContractBranchStateStore
    BranchStateStore branchStore;

    @ContractTransactionReceipt
    TransactionReceipt txReceipt;

    @ContractQuery
    public ContractProposal proposalStatus(JsonObject params) {
        return getProposal(params.get("txId").getAsString());
    }

    @InvokeTransaction
    public TransactionReceipt propose(JsonObject params) {
        // Verify that the issuer is a validator
        if (!isValidator()) {
            setFalseTxReceipt("Validator verification failed");
            return txReceipt;
        }

        // Create a contract proposal with parameters.
        String txId = txReceipt.getTxId();
        long blockHeight = txReceipt.getBlockHeight();
        String contractVersion = params.get("contractVersion").getAsString();
        String sourceUrl = params.get("sourceUrl").getAsString();
        String buildVersion = params.get("buildVersion").getAsString();
        Set<String> validatorSet = new HashSet<>(branchStore.getValidators().getValidatorMap().keySet());

        ContractProposal proposal = new ContractProposal(
                txId, contractVersion, sourceUrl, buildVersion, blockHeight, validatorSet);

        JsonObject proposalObj = JsonUtil.parseJsonObject(proposal);

        // Store the proposal in stateStore
        state.put(txId, proposalObj);

        setSuccessTxReceipt("Contract proposal has been issued");
        log.info("Contract Proposal : txId = {}, proposal = {}", txId, proposalObj);

        return txReceipt;
    }

    @InvokeTransaction
    public TransactionReceipt vote(JsonObject params) {
        // Verify that the issuer is a validator
        if (!isValidator()) {
            setFalseTxReceipt("Validator verification failed");
            return txReceipt;
        }

        String txId = params.get("txId").getAsString();
        ContractProposal proposal = getProposal(txId);
        // Verify the proposal exists
        if (proposal == null) {
            setFalseTxReceipt("Contract proposal not found");
            return txReceipt;
        }

        long curBlockHeight = txReceipt.getBlockHeight();
        // Verify the proposal is expired
        if (proposal.isExpired(curBlockHeight)) {
            setFalseTxReceipt("Contract proposal has already expired");
            return txReceipt;
        }

        String issuer = txReceipt.getIssuer();
        boolean agree = params.get("agree").getAsBoolean();

        proposal.vote(issuer, agree);
        state.put(txId, JsonUtil.parseJsonObject(proposal));

        setSuccessTxReceipt("Update proposal voting is in progress");

        String contractVersion = proposal.getContractVersion();
        // Verify the voting is finished
        if (proposal.isVotingFinished()) {
            try {
                downloadContractFile(contractVersion);
                setSuccessTxReceipt("Contract file has been downloaded");

                moveTmpContract(contractVersion);
                setSuccessTxReceipt("Update proposal voting was completed successfully");
            } catch (IOException e) {
                setFalseTxReceipt("Contract file download failed or cannot be located");
                return txReceipt;
            }
        }

        return txReceipt;
    }

    private void moveTmpContract(String contractVersion) throws IOException {
        Path tmp = Paths.get(String.format("%s/%s", tmpContractPath(), contractVersion + ".jar"));
        Path origin = Paths.get(contractPath());
        Files.move(tmp, origin.resolve(tmp.getFileName()), StandardCopyOption.REPLACE_EXISTING);
    }

    private File downloadContractFile(String contractVersion) throws IOException {
        return Downloader.downloadContract(tmpContractPath(), ContractVersion.of(contractVersion));
    }

    private String tmpContractPath() {
        return String.format("%s%s", contractPath(), SUFFIX_TEMP_CONTRACT);
    }

    private String contractPath() {
        Path path = Paths.get(System.getProperty("user.dir"));
        return String.format("%s%s", path.getParent(), SUFFIX_CONTRACT);
    }

    private ContractProposal getProposal(String txId) {
        JsonObject proposalObj = state.get(txId);
        return proposalObj != null
                ? JsonUtil.generateJsonToClass(proposalObj.toString(), ContractProposal.class) : null;
    }

    private boolean isValidator() {
        return branchStore.getValidators() != null && branchStore.getValidators().getValidatorMap().keySet().stream()
                .anyMatch(key -> !key.isEmpty() && key.equals(txReceipt.getIssuer()));
    }

    private void setFalseTxReceipt(String msg) {
        this.txReceipt.setStatus(ExecuteStatus.FALSE);
        this.txReceipt.addLog(msg);
    }

    private void setSuccessTxReceipt(String msg) {
        this.txReceipt.setStatus(ExecuteStatus.SUCCESS);
        this.txReceipt.addLog(msg);
    }

}