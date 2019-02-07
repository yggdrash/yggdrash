package io.yggdrash.validator.data.pbft;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.yggdrash.proto.PbftProto;
import org.spongycastle.util.encoders.Hex;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

public class PbftMessageSet {

    private final PbftMessage prePrepare;
    private final Map<String, PbftMessage> prepareMap = new TreeMap<>();
    private final Map<String, PbftMessage> commitMap = new TreeMap<>();
    private final PbftMessage viewChange;

    public PbftMessageSet(PbftMessage prePrepare, Map<String, PbftMessage> prepareMap,
                          Map<String, PbftMessage> commitMap) {
        this(prePrepare, prepareMap, commitMap, null);
    }

    public PbftMessageSet(PbftMessage prePrepare, Map<String, PbftMessage> prepareMap,
                          Map<String, PbftMessage> commitMap, PbftMessage viewChange) {
        this.prePrepare = prePrepare;

        if (prepareMap != null) {
            this.prepareMap.putAll(prepareMap);
        }

        if (commitMap != null) {
            this.commitMap.putAll(commitMap);
        }

        this.viewChange = viewChange;
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

        this.viewChange = new PbftMessage(protoPbftMessageSet.getViewChange());
    }

    public PbftMessageSet(JsonObject jsonObject) {
        this.prePrepare = new PbftMessage(jsonObject.get("prePrepare").getAsJsonObject());
        for (JsonElement pbftMessageJsonElement : jsonObject.get("prepareList").getAsJsonArray()) {
            PbftMessage pbftMessage = new PbftMessage(pbftMessageJsonElement.getAsJsonObject());
            this.prepareMap.put(pbftMessage.getHashHex(), pbftMessage);
        }

        for (JsonElement pbftMessageJsonElement : jsonObject.get("commitList").getAsJsonArray()) {
            PbftMessage pbftMessage = new PbftMessage(pbftMessageJsonElement.getAsJsonObject());
            this.commitMap.put(pbftMessage.getHashHex(), pbftMessage);
        }
        this.viewChange = new PbftMessage(jsonObject.get("viewChange").getAsJsonObject());
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

    public PbftMessage getViewChange() {
        return viewChange;
    }

    public static boolean verify(PbftMessageSet pbftMessageSet) {
        PbftMessage prePrepare = pbftMessageSet.getPrePrepare();
        Map<String, PbftMessage> prepareMap = pbftMessageSet.getPrepareMap();
        Map<String, PbftMessage> commitMap = pbftMessageSet.getCommitMap();

        if (prePrepare == null || prePrepare.getSignature() == null
                || prepareMap == null
                || commitMap == null) {
            return false;
        }

        if (!PbftMessage.verify(prePrepare)) {
            return false;
        }

        for (String key : prepareMap.keySet()) {
            PbftMessage pbftMessage = prepareMap.get(key);
            if (!PbftMessage.verify(pbftMessage)) {
                return false;
            }
        }

        for (String key : commitMap.keySet()) {
            PbftMessage pbftMessage = commitMap.get(key);
            if (!PbftMessage.verify(pbftMessage)) {
                return false;
            }
        }

        //todo : check 2f + 1 message count

        return true;
    }

    public byte[] toBinary() {
        return this.toJsonObject().toString().getBytes(StandardCharsets.UTF_8);
    }

    public JsonObject toJsonObject() {
        JsonObject pbftMessageSetJsonObject = new JsonObject();

        JsonObject prePrepareJsonObject = this.prePrepare.toJsonObject();
        pbftMessageSetJsonObject.add("prePrepare", prePrepareJsonObject);

        JsonArray prepareJsonArray = new JsonArray();
        for (String key : this.getPrepareMap().keySet()) {
            PbftMessage pbftMessage = this.getPrepareMap().get(key);
            prepareJsonArray.add(pbftMessage.toJsonObject());
        }
        pbftMessageSetJsonObject.add("prepareList", prepareJsonArray);

        JsonArray commitJsonArray = new JsonArray();
        for (String key : this.getCommitMap().keySet()) {
            PbftMessage pbftMessage = this.getCommitMap().get(key);
            commitJsonArray.add(pbftMessage.toJsonObject());
        }
        pbftMessageSetJsonObject.add("commitList", commitJsonArray);

        JsonObject viewChangeJsonObject = this.viewChange.toJsonObject();
        pbftMessageSetJsonObject.add("viewChange", viewChangeJsonObject);

        return pbftMessageSetJsonObject;
    }


    public static PbftProto.PbftMessageSet toProto(PbftMessageSet pbftMessageSet) {

        if (pbftMessageSet == null) {
            return null;
        }

        PbftProto.PbftMessage protoPrePrepareMessage =
                PbftMessage.toProto(pbftMessageSet.getPrePrepare());
        PbftProto.PbftMessageList protoPrepareMessageList =
                PbftMessage.toProtoList(
                        new ArrayList<PbftMessage>(pbftMessageSet.getPrepareMap().values()));
        PbftProto.PbftMessageList protoCommitMessageList =
                PbftMessage.toProtoList(
                        new ArrayList<PbftMessage>(pbftMessageSet.getCommitMap().values()));
        PbftProto.PbftMessage protoViewChangeMessage =
                PbftMessage.toProto(pbftMessageSet.getViewChange());

        PbftProto.PbftMessageSet.Builder protoPbftMessageSetBuilder =
                PbftProto.PbftMessageSet.newBuilder();
        if (protoPrePrepareMessage != null) {
            protoPbftMessageSetBuilder.setPrePrepare(protoPrePrepareMessage);
        }
        if (protoPrepareMessageList != null) {
            protoPbftMessageSetBuilder.setPrepareList(protoPrepareMessageList);
        }
        if (protoCommitMessageList != null) {
            protoPbftMessageSetBuilder.setCommitList(protoCommitMessageList);
        }
        if (protoViewChangeMessage != null) {
            protoPbftMessageSetBuilder.setViewChange(protoViewChangeMessage);
        }

        return protoPbftMessageSetBuilder.build();
    }

}
