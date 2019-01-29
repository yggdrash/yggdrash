package io.yggdrash.validator.data.pbft;

import io.yggdrash.core.blockchain.Block;
import io.yggdrash.proto.PbftProto;
import org.spongycastle.util.encoders.Hex;

import java.util.Map;
import java.util.TreeMap;

public class PbftMessageSet {

    private final PbftMessage prePrepare;
    private final Map<String, PbftMessage> prepareMap = new TreeMap<>();
    private final Map<String, PbftMessage> commitMap = new TreeMap<>();

    public PbftMessageSet(PbftMessage prePrepare, TreeMap<String, PbftMessage> prepareMap, TreeMap<String, PbftMessage> commitMap) {
        this.prePrepare = prePrepare;
        if (prepareMap != null) {
            this.prepareMap.putAll(prepareMap);
        }
        if (commitMap != null) {
            this.commitMap.putAll(commitMap);
        }
    }

    public PbftMessageSet(PbftProto.PbftMessageSet protoPbftMessageSet) {
        this.prePrepare = new PbftMessage(protoPbftMessageSet.getPrePrepare());

        for (PbftProto.PbftMessage pbftMessage : protoPbftMessageSet.getPrepareList().getPbftMessageListList()) {
            if (!this.prepareMap.containsKey(Hex.toHexString(pbftMessage.getSignature().toByteArray()))) {
                this.prepareMap.putIfAbsent(Hex.toHexString(pbftMessage.getSignature().toByteArray()),
                        new PbftMessage(pbftMessage));
            }
        }

        for (PbftProto.PbftMessage pbftMessage : protoPbftMessageSet.getCommitList().getPbftMessageListList()) {
            if (!this.commitMap.containsKey(Hex.toHexString(pbftMessage.getSignature().toByteArray()))) {
                this.commitMap.putIfAbsent(Hex.toHexString(pbftMessage.getSignature().toByteArray()),
                        new PbftMessage(pbftMessage));
            }
        }
    }

    public PbftMessage getPrePrepare() {
        return prePrepare;
    }

    public Map<String, PbftMessage> getPrepareMap() {
        return prepareMap;
    }

    public Map<String, PbftMessage> getCommitMap() {
        return commitMap;
    }

    public static boolean verify(PbftMessageSet pbftMessageSet, Block block) {
//        PbftMessage prePrepare = pbftMessageSet.getPrePrepare();
//        List<PbftMessage> prepareMap = pbftMessageSet.getPrepareMap();
//        List<PbftMessage> commitMap = pbftMessageSet.getCommitMap();
//
//        if (prePrepare == null
//                || prepareMap == null
//                || commitMap == null) {
//            return false;
//        }
//
//        if (!PbftMessage.verify(prePrepare, block)) {
//            return false;
//        }
//
//        for (PbftMessage pbftMessage : ((Map) prepareMap).keySet()) {
//            if (!PbftMessage.verify(pbftMessage)) {
//                return false;
//            }
//        }
//
//        for (PbftMessage pbftMessage : commitList) {
//            if (!PbftMessage.verify(pbftMessage)) {
//                return false;
//            }
//        }

        return true;
    }

    public byte[] toBinary() {
        return null;
    }

    public static PbftProto.PbftMessageSet toProto(PbftMessageSet pbftMessageSet) {

        if (pbftMessageSet == null) {
            return null;
        }

//        PbftProto.PbftMessage protoPrePrepareMessage =
//                PbftMessage.toProto(pbftMessageSet.getPrePrepare());
//        PbftProto.PbftMessageList protoPrepareMessageList =
//                PbftMessage.toProtoList(pbftMessageSet.getPrepareMap());
//        PbftProto.PbftMessageList protoCommitMessageList =
//                PbftMessage.toProtoList(pbftMessageSet.getCommitMap());
//
        PbftProto.PbftMessageSet.Builder protoPbftMessageSetBuilder =
                PbftProto.PbftMessageSet.newBuilder();
//        if (protoPrePrepareMessage != null) {
//            protoPbftMessageSetBuilder.setPrePrepare(protoPrePrepareMessage);
//        }
//        if (protoPrepareMessageList != null) {
//            protoPbftMessageSetBuilder.setPrepareList(protoPrepareMessageList);
//        }
//        if (protoCommitMessageList != null) {
//            protoPbftMessageSetBuilder.setCommitList(protoCommitMessageList);
//        }

        return protoPbftMessageSetBuilder.build();
    }

}
