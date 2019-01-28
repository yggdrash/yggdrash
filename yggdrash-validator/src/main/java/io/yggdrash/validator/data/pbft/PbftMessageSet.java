package io.yggdrash.validator.data.pbft;

import io.yggdrash.proto.PbftProto;

import java.util.List;

public class PbftMessageSet {

    private PbftMessage prePrepare;
    private List<PbftMessage> prepareList;
    private List<PbftMessage> commitList;

    public PbftMessageSet(PbftMessage prePrepare, List<PbftMessage> prepareList, List<PbftMessage> commitList) {
        this.prePrepare = prePrepare;
        this.prepareList = prepareList;
        this.commitList = commitList;
    }

    public PbftMessageSet(PbftProto.PbftMessageSet protoPbftMessageSet) {
        this.prePrepare = new PbftMessage(protoPbftMessageSet.getPrePrepare());
        this.prepareList = PbftMessage.toPbftMessageList(protoPbftMessageSet.getPrepareList());
        this.commitList = PbftMessage.toPbftMessageList(protoPbftMessageSet.getCommitList());
    }

    public PbftMessage getPrePrepare() {
        return prePrepare;
    }

    public void setPrePrepare(PbftMessage prePrepare) {
        this.prePrepare = prePrepare;
    }

    public List<PbftMessage> getPrepareList() {
        return prepareList;
    }

    public void setPrepareList(List<PbftMessage> prepareList) {
        this.prepareList = prepareList;
    }

    public List<PbftMessage> getCommitList() {
        return commitList;
    }

    public void setCommitList(List<PbftMessage> commitList) {
        this.commitList = commitList;
    }

    public byte[] toBinary() {
        return null;
    }

    public static PbftProto.PbftMessageSet toProto(PbftMessageSet pbftMessageSet) {
        PbftProto.PbftMessageSet.Builder protoPbftMessageSetBuilder =
                PbftProto.PbftMessageSet.newBuilder()
                        .setPrePrepare(PbftMessage.toProto(pbftMessageSet.getPrePrepare()))
                        .setPrepareList(PbftMessage.toProtoList(pbftMessageSet.getPrepareList()))
                        .setCommitList(PbftMessage.toProtoList(pbftMessageSet.getCommitList()));
        return protoPbftMessageSetBuilder.build();
    }

}
