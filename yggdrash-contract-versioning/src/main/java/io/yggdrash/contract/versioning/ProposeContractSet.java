package io.yggdrash.contract.versioning;

import io.yggdrash.common.contract.vo.dpoa.Validator;
import io.yggdrash.common.contract.vo.dpoa.ValidatorSet;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class ProposeContractSet {
    private Map<String, Votable> contractVote;

    public ProposeContractSet() {
        contractVote = new HashMap<>();
    }

    public Map<String, Votable> getContractVote() {
        return contractVote;
    }

    public void setContractVote(Map<String, Votable> contractVote) {
        this.contractVote = contractVote;
    }

    public static class Votable implements Serializable {
        private String issuer;
        private int totalVotableCnt;
        private int agreeCnt;
        private int disagreeCnt;
        private Map<String, Votable.Vote> votedMap;

        public Votable() {
        }

        public Votable(String issuer, ValidatorSet validatorSet) {
            this.issuer = issuer;
            Map<String, Validator> validatorMap = validatorSet.getValidatorMap();
            this.totalVotableCnt = validatorMap.size();
            this.votedMap = new HashMap<>();
            validatorMap.forEach((k, v) -> this.votedMap.put(k, new Votable.Vote()));
        }

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public int getTotalVotableCnt() {
            return totalVotableCnt;
        }

        public void setTotalVotableCnt(int totalVotableCnt) {
            this.totalVotableCnt = totalVotableCnt;
        }

        public int getAgreeCnt() {
            return agreeCnt;
        }

        public void setAgreeCnt(int agreeCnt) {
            this.agreeCnt = agreeCnt;
        }

        public int getDisagreeCnt() {
            return disagreeCnt;
        }

        public void setDisagreeCnt(int disagreeCnt) {
            this.disagreeCnt = disagreeCnt;
        }

        public Map<String, Votable.Vote> getVotedMap() {
            return votedMap;
        }

        public void setVotedMap(Map<String, Votable.Vote> votedMap) {
            this.votedMap = votedMap;
        }

        public static class Vote implements Serializable {
            boolean isVoted;
            boolean isAgree;

            public Vote() {

            }

            public Vote(boolean isVoted, boolean isAgree) {
                this.isVoted = isVoted;
                this.isAgree = isAgree;
            }

            public boolean isVoted() {
                return isVoted;
            }

            public void setVoted(boolean voted) {
                isVoted = voted;
            }

            public boolean isAgree() {
                return isAgree;
            }

            public void setAgree(boolean agree) {
                isAgree = agree;
            }
        }

        public enum VoteStatus {
            AGREE,
            DISAGREE,
            NOT_YET
        }

        public Votable.VoteStatus status() {
            int cnt = totalVotableCnt;
            if (totalVotableCnt != 2) {
                cnt = (totalVotableCnt / 3) * 2;
                cnt += totalVotableCnt % 3 > 0 ? 1 : 0;
            }

            if (agreeCnt >= cnt) {
                return Votable.VoteStatus.AGREE;
            }

            if (disagreeCnt >= cnt) {
                return Votable.VoteStatus.DISAGREE;
            }

            if (totalVotableCnt == (agreeCnt + disagreeCnt)) {
                return Votable.VoteStatus.DISAGREE;
            }

            return Votable.VoteStatus.NOT_YET;
        }
    }
}
