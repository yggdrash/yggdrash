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

    VotingProgress() {

    }

    VotingProgress(Set<String> validatorSet) {
        this.totalVotingCnt = validatorSet.size();
        this.agreeCnt = 0;
        this.disagreeCnt = 0;
        this.votingHistory = new HashMap<>();
        validatorSet.forEach(validator -> this.votingHistory.put(validator, new Vote()));
    }

    void vote(String issuer, boolean agree) {
        if (agree) {
            agreeCnt++;
        } else {
            disagreeCnt++;
        }
        votingHistory.put(issuer, new Vote(agree));
    }

    boolean isVotingFinished() {
        int cnt = (int) Math.ceil((double) (totalVotingCnt / 3) * 2);
        return agreeCnt >= cnt;
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