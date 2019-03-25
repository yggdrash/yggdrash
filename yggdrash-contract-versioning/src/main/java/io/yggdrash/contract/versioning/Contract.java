package io.yggdrash.contract.versioning;

import io.yggdrash.common.contract.vo.dpoa.Validator;
import io.yggdrash.common.contract.vo.dpoa.ValidatorSet;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Contract implements Serializable, Comparable<Contract> {

    private String targetVersion;
    private Votable votedHistory;
    private boolean isUpgrade;
    private long targetBlockHeight;
    private byte[] updateContract;
    private String txId;

    public Contract() {
    }

    public Contract(String txId) {
        this.txId = txId;
        this.isUpgrade = false;
    }
    public String getTxId() {
        return txId;
    }

    public void setTxId(String txId) {
        this.txId = txId;
    }

    public String getTargetVersion() {
        return targetVersion;
    }

    public void setTargetVersion(String targetVersion) {
        this.targetVersion = targetVersion;
    }

    public Votable getVotedHistory() {
        return votedHistory;
    }

    public void setVotedHistory(Votable votedHistory) {
        this.votedHistory = votedHistory;
    }

    public boolean isUpgrade() {
        return isUpgrade;
    }

    public void setUpgrade(boolean upgrade) {
        isUpgrade = upgrade;
    }

    public long gettargetBlockHeight() {
        return targetBlockHeight;
    }

    public void setTargetBlockHeight(long targetBlockHeight) {
        this.targetBlockHeight = targetBlockHeight;
    }

    public byte[] getUpdateContract() {
        return updateContract;
    }

    public void setUpdateContract(byte[] updateContract) {
        this.updateContract = updateContract;
    }

    @Override
    public int compareTo(Contract o) {
        return targetVersion.compareTo(o.targetVersion);

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
