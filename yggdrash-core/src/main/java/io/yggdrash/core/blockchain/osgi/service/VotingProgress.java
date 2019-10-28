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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class VotingProgress implements Serializable {

    public int totalVotingCnt;
    public int agreeCnt;
    public int disagreeCnt;
    public Map<String, Vote> votingHistory;
    public VotingStatus votingStatus;

    public enum VotingStatus {
        VOTEABLE,
        AGREE,
        DISAGREE,
        EXPIRED,
        APPLYING
    }

    VotingProgress() {

    }

    VotingProgress(Set<String> validatorSet) {
        this.totalVotingCnt = validatorSet.size();
        this.agreeCnt = 0;
        this.disagreeCnt = 0;
        this.votingHistory = new HashMap<>();
        validatorSet.forEach(validator -> this.votingHistory.put(validator, new Vote()));
        this.votingStatus = VotingStatus.VOTEABLE;
    }

    void vote(String issuer, boolean agree) {
        if (agree) {
            agreeCnt++;
        } else {
            disagreeCnt++;
        }
        votingHistory.put(issuer, new Vote(agree));
        isAgreed();
    }

    boolean hashVoted(String issuer) {
        return votingHistory.get(issuer).isVoted;
    }


    boolean isAgreed() {
        int cnt = (int) (((double) totalVotingCnt * 0.66) + 1.0);

        boolean bAgreed = agreeCnt >= cnt;
        if (bAgreed) {
            votingStatus = VotingStatus.AGREE;
        }

        if (disagreeCnt > totalVotingCnt - cnt) {
            votingStatus = VotingStatus.DISAGREE;
        }

        return bAgreed;
    }

    VotingStatus getVotingStatus() {
        return votingStatus;
    }

    void setVotingStatus(VotingStatus votingStatus) {
        this.votingStatus = votingStatus;
    }

    static class Vote implements Serializable {
        public boolean isVoted;
        public boolean isAgree;

        Vote() {
            isVoted = false;
            isAgree = false;
        }

        Vote(boolean agree) {
            isVoted = true;
            isAgree = agree;
        }
    }

}
