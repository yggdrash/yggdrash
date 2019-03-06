package io.yggdrash.common.contract.vo.dpoa;

import io.yggdrash.common.contract.SerialEnum;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class ProposeValidatorSet implements Serializable {
    private static final long serialVersionUID = SerialEnum.PROPOSE_VALIDATOR_SET.toValue();

    private Map<String, Votable> validatorMap;

    public ProposeValidatorSet() {
        validatorMap = new HashMap<>();
    }

    public Map<String, Votable> getValidatorMap() {
        return validatorMap;
    }

    public void setValidatorMap(Map<String, Votable> validatorMap) {
        this.validatorMap = validatorMap;
    }

    public static class Votable implements Serializable {
        private String proposalValidatorAddr;

        private int totalVotableCnt;
        private int agreeCnt;
        private int disagreeCnt;
        private Map<String, Vote> votedMap;

        public Votable() {
        }

        public Votable(String proposalValidatorAddr, ValidatorSet validatorSet) {
            this.proposalValidatorAddr = proposalValidatorAddr;
            Map<String, Validator> validatorMap = validatorSet.getValidatorMap();
            this.totalVotableCnt = validatorMap.size();
            this.votedMap = new HashMap<>();
            validatorMap.forEach((k, v) -> this.votedMap.put(k, new Vote()));
        }

        public String getProposalValidatorAddr() {
            return proposalValidatorAddr;
        }

        public void setProposalValidatorAddr(String proposalValidatorAddr) {
            this.proposalValidatorAddr = proposalValidatorAddr;
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

        public Map<String, Vote> getVotedMap() {
            return votedMap;
        }

        public void setVotedMap(Map<String, Vote> votedMap) {
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

        public VoteStatus status() {
            int cnt = totalVotableCnt;
            if (totalVotableCnt != 2) {
                cnt = (totalVotableCnt / 3) * 2;
                cnt += totalVotableCnt % 3 > 0 ? 1 : 0;
            }

            if (agreeCnt >= cnt) {
                return VoteStatus.AGREE;
            }

            if (disagreeCnt >= cnt) {
                return VoteStatus.DISAGREE;
            }

            if (totalVotableCnt == (agreeCnt + disagreeCnt)) {
                return VoteStatus.DISAGREE;
            }

            return VoteStatus.NOT_YET;
        }
    }
}
