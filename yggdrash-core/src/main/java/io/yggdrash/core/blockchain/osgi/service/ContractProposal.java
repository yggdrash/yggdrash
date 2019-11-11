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

import java.io.Serializable;
import java.util.Set;

public class ContractProposal implements Serializable, Comparable<ContractProposal> {

    public String txId;
    public String proposer;

    public String proposalVersion;
    public String sourceUrl;
    public String buildVersion;

    public long targetBlockHeight;
    public long applyBlockHeight;

    public VotingProgress votingProgress;
    public ProposalType proposalType;


    ContractProposal() {

    }

    public ContractProposal(String txId, String proposer, String proposalVersion, String sourceUrl,
                            String buildVersion, long blockHeight, long votePeriod, long applyPeriod,
                            Set<String> validatorSet, String proposalType) {
        this.txId = txId;
        this.proposer = proposer;
        this.proposalVersion = proposalVersion;
        this.sourceUrl = sourceUrl;
        this.buildVersion = buildVersion;
        this.targetBlockHeight = blockHeight + votePeriod;
        this.applyBlockHeight = targetBlockHeight + applyPeriod;
        this.votingProgress = new VotingProgress(validatorSet);
        this.proposalType = ProposalType.valueOf(proposalType);
    }

    boolean isExpired(long blockHeight) {
        return votingProgress.votingStatus.equals(VotingProgress.VotingStatus.EXPIRED)
                | blockHeight > targetBlockHeight;
    }

    boolean hasAlreadyVoted(String validator) {
        return votingProgress.hashVoted(validator);
    }

    void vote(String issuer, boolean agree) {
        votingProgress.vote(issuer, agree);
    }

    boolean isAgreed() {
        return votingProgress.isAgreed();
    }

    void setVotingStatus(VotingProgress.VotingStatus status) {
        this.votingProgress.setVotingStatus(status);
    }

    String getTxId() {
        return txId;
    }

    public String getProposalVersion() {
        return proposalVersion;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public String getBuildVersion() {
        return buildVersion;
    }

    public long getTargetBlockHeight() {
        return targetBlockHeight;
    }

    public long getApplyBlockHeight() {
        return applyBlockHeight;
    }

    public VotingProgress getVotingProgress() {
        return votingProgress;
    }

    public ProposalType getProposalType() {
        return this.proposalType;
    }

    @Override
    public int compareTo(ContractProposal proposal) {
        return proposalVersion.compareTo(proposal.proposalVersion);
    }

}
