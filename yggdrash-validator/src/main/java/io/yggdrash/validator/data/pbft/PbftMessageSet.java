package io.yggdrash.validator.data.pbft;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.yggdrash.common.utils.JsonUtil;
import io.yggdrash.common.utils.SerializationUtil;
import io.yggdrash.core.consensus.ConsensusMessageSet;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.proto.PbftProto;
import org.spongycastle.util.encoders.Hex;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

public class PbftMessageSet implements ConsensusMessageSet<PbftMessage> {

    private final PbftMessage prePrepare;

    // Map<SignatureHex, PbftMessage>
    private final Map<String, PbftMessage> prepareMap = new TreeMap<>();
    private final Map<String, PbftMessage> commitMap = new TreeMap<>();
    private final Map<String, PbftMessage> viewChangeMap = new TreeMap<>();

    public PbftMessageSet(PbftMessage prePrepare, Map<String, PbftMessage> prepareMap,
                          Map<String, PbftMessage> commitMap, Map<String, PbftMessage> viewChangeMap) {
        if (prePrepare == null) {
            throw new NotValidateException("PrePrepare is not valid");
        }

        this.prePrepare = prePrepare;

        if (prepareMap != null) {
            this.prepareMap.putAll(prepareMap);
        }

        if (commitMap != null) {
            this.commitMap.putAll(commitMap);
        }
        if (viewChangeMap != null) {
            this.viewChangeMap.putAll(viewChangeMap);
        }
    }

    public PbftMessageSet(JsonObject jsonObject) {
        this.prePrepare = new PbftMessage(jsonObject.get("prePrepare").getAsJsonObject());

        if (jsonObject.get("prepareList") != null) {
            for (JsonElement pbftMessageJsonElement : jsonObject.get("prepareList").getAsJsonArray()) {
                PbftMessage pbftMessage = new PbftMessage(pbftMessageJsonElement.getAsJsonObject());
                this.prepareMap.put(pbftMessage.getSignatureHex(), pbftMessage);
            }
        }

        if (jsonObject.get("commitList") != null) {
            for (JsonElement pbftMessageJsonElement : jsonObject.get("commitList").getAsJsonArray()) {
                PbftMessage pbftMessage = new PbftMessage(pbftMessageJsonElement.getAsJsonObject());
                this.commitMap.put(pbftMessage.getSignatureHex(), pbftMessage);
            }
        }
        if (jsonObject.get("viewChangeList") != null) {
            for (JsonElement pbftMessageJsonElement : jsonObject.get("viewChangeList").getAsJsonArray()) {
                PbftMessage pbftMessage = new PbftMessage(pbftMessageJsonElement.getAsJsonObject());
                this.viewChangeMap.put(pbftMessage.getSignatureHex(), pbftMessage);
            }
        }
    }

    public PbftMessageSet(byte[] bytes) {
        this(JsonUtil.parseJsonObject(SerializationUtil.deserializeString(bytes)));
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

        for (PbftProto.PbftMessage pbftMessage : protoPbftMessageSet.getViewChangeList().getPbftMessageListList()) {
            if (!this.viewChangeMap.containsKey(Hex.toHexString(pbftMessage.getSignature().toByteArray()))) {
                this.viewChangeMap.putIfAbsent(Hex.toHexString(pbftMessage.getSignature().toByteArray()),
                        new PbftMessage(pbftMessage));
            }
        }
    }

    @Override
    public PbftMessage getPrePrepare() {
        return prePrepare;
    }

    @Override
    public Map<String, PbftMessage> getPrepareMap() {
        return prepareMap;
    }

    @Override
    public Map<String, PbftMessage> getCommitMap() {
        return commitMap;
    }

    @Override
    public Map<String, PbftMessage> getViewChangeMap() {
        return viewChangeMap;
    }

    @Override
    public byte[] toBinary() {
        return SerializationUtil.serializeJson(toJsonObject());
    }

    @Override
    public JsonObject toJsonObject() {
        if (this.prePrepare == null) {
            return null;
        }

        JsonObject pbftMessageSetJsonObject = new JsonObject();
        JsonObject prePrepareJsonObject = this.prePrepare.toJsonObject();
        if (prePrepareJsonObject.size() > 0) {
            pbftMessageSetJsonObject.add("prePrepare", prePrepareJsonObject);
        }

        JsonArray prepareJsonArray = new JsonArray();
        for (String key : this.getPrepareMap().keySet()) {
            PbftMessage pbftMessage = this.getPrepareMap().get(key);
            prepareJsonArray.add(pbftMessage.toJsonObject());
        }
        if (prepareJsonArray.size() > 0) {
            pbftMessageSetJsonObject.add("prepareList", prepareJsonArray);
        }

        JsonArray commitJsonArray = new JsonArray();
        for (String key : this.getCommitMap().keySet()) {
            PbftMessage pbftMessage = this.getCommitMap().get(key);
            commitJsonArray.add(pbftMessage.toJsonObject());
        }
        if (commitJsonArray.size() > 0) {
            pbftMessageSetJsonObject.add("commitList", commitJsonArray);
        }

        JsonArray viewChangeJsonArray = new JsonArray();
        for (String key : this.getViewChangeMap().keySet()) {
            PbftMessage pbftMessage = this.getViewChangeMap().get(key);
            viewChangeJsonArray.add(pbftMessage.toJsonObject());
        }
        if (viewChangeJsonArray.size() > 0) {
            pbftMessageSetJsonObject.add("viewChangeList", viewChangeJsonArray);
        }

        return pbftMessageSetJsonObject;
    }


    public static PbftProto.PbftMessageSet toProto(PbftMessageSet pbftMessageSet) {

        if (pbftMessageSet == null) {
            return null;
        }

        PbftProto.PbftMessage protoPrePrepareMessage =
                PbftMessage.toProto(pbftMessageSet.getPrePrepare());

        PbftProto.PbftMessageList protoPrepareMessageList =
                PbftMessage.toProtoList(pbftMessageSet.getPrepareMap().values());

        PbftProto.PbftMessageList protoCommitMessageList =
                PbftMessage.toProtoList(pbftMessageSet.getCommitMap().values());

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

        PbftProto.PbftMessageList protoViewChangeMessageList =
                PbftMessage.toProtoList(pbftMessageSet.getViewChangeMap().values());
        if (protoViewChangeMessageList != null) {
            protoPbftMessageSetBuilder.setViewChangeList(protoViewChangeMessageList);
        }

        return protoPbftMessageSetBuilder.build();
    }

    public static PbftMessageSet forGenesis() {
        PbftProto.PbftMessage empty = PbftProto.PbftMessage.newBuilder().build();
        return new PbftMessageSet(new PbftMessage(empty), null, null, null);
    }

    @Override
    public void clear() {
        this.prePrepare.clear();

        for (PbftMessage pbftMessage : this.prepareMap.values()) {
            pbftMessage.clear();
        }

        for (PbftMessage pbftMessage : this.commitMap.values()) {
            pbftMessage.clear();
        }

        for (PbftMessage pbftMessage : this.viewChangeMap.values()) {
            pbftMessage.clear();
        }
    }

    @Override
    public PbftMessageSet clone() {
        return new PbftMessageSet(this.toJsonObject());
    }

    public boolean equals(PbftMessageSet newPbftMessageSet) {
        if (newPbftMessageSet == null) {
            return false;
        }
        return Arrays.equals(this.toBinary(), newPbftMessageSet.toBinary());
    }
}
