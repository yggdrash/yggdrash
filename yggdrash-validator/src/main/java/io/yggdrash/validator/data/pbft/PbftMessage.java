package io.yggdrash.validator.data.pbft;

import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import io.yggdrash.common.util.JsonUtil;
import io.yggdrash.proto.PbftProto;
import org.spongycastle.util.encoders.Hex;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class PbftMessage {
    private final String type;
    private final long viewNumber;
    private final long seqNumber;
    private final byte[] hash;
    private final byte[] result;
    private final byte[] signature;

    public PbftMessage(String type,
                       long viewNumber,
                       long seqNumber,
                       byte[] hash,
                       byte[] result,
                       byte[] signature) {
        this.type = type;
        this.viewNumber = viewNumber;
        this.seqNumber = seqNumber;
        this.hash = hash;
        this.result = result;
        this.signature = signature;
    }

    public PbftMessage(byte[] bytes) {
        JsonObject jsonObject = JsonUtil.parseJsonObject(new String(bytes, StandardCharsets.UTF_8));
        this.type = jsonObject.get("type").getAsString();
        this.viewNumber = jsonObject.get("viewNumber").getAsLong();
        this.seqNumber = jsonObject.get("seqNumber").getAsLong();
        this.hash = Hex.decode(jsonObject.get("hash").getAsString());
        this.result = Hex.decode(jsonObject.get("result").getAsString());
        this.signature = Hex.decode(jsonObject.get("signature").getAsString());
    }

    public PbftMessage(PbftProto.PbftMessage protoPbftMessage) {
        this.type = protoPbftMessage.getType();
        this.viewNumber = protoPbftMessage.getViewNumber();
        this.seqNumber = protoPbftMessage.getSeqNumber();
        this.hash = protoPbftMessage.getHash().toByteArray();
        this.result = protoPbftMessage.getResult().toByteArray();
        this.signature = protoPbftMessage.getSignature().toByteArray();
    }

    public String getType() {
        return type;
    }

    public long getViewNumber() {
        return viewNumber;
    }

    public long getSeqNumber() {
        return seqNumber;
    }

    public byte[] getHash() {
        return hash;
    }

    public byte[] getResult() {
        return result;
    }

    public byte[] getSignature() {
        return signature;
    }

    public byte[] toBinary() {
        return this.toJsonObject().toString().getBytes(StandardCharsets.UTF_8);
    }

    public JsonObject toJsonObject() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", this.type);
        jsonObject.addProperty("viewNumber", this.viewNumber);
        jsonObject.addProperty("seqNumber", this.seqNumber);
        jsonObject.addProperty("hash", Hex.toHexString(this.hash));
        jsonObject.addProperty("result", Hex.toHexString(this.result));
        jsonObject.addProperty("signature", Hex.toHexString(this.signature));
        return jsonObject;
    }

    public static List<PbftMessage> toPbftMessageList(
            PbftProto.PbftMessageList protoPbftMessageList) {
        List<PbftMessage> pbftMessagesList = new ArrayList<>();
        for (PbftProto.PbftMessage protoPbftMessage : protoPbftMessageList.getPbftMessageListList()) {
            pbftMessagesList.add(new PbftMessage(protoPbftMessage));
        }
        return pbftMessagesList;
    }

    public static PbftProto.PbftMessage toProto(PbftMessage pbftMessage) {
        PbftProto.PbftMessage.Builder protoPbftMessageBuilder = PbftProto.PbftMessage.newBuilder()
                .setType(pbftMessage.getType())
                .setViewNumber(pbftMessage.getViewNumber())
                .setSeqNumber(pbftMessage.getSeqNumber())
                .setHash(ByteString.copyFrom(pbftMessage.getHash()))
                .setResult(ByteString.copyFrom(pbftMessage.getResult()))
                .setSignature(ByteString.copyFrom(pbftMessage.getSignature()));
        return protoPbftMessageBuilder.build();
    }

    public static PbftProto.PbftMessageList toProtoList(
            List<PbftMessage> pbftMessageList) {
        PbftProto.PbftMessageList.Builder protoPbftMessageListBuilder =
                PbftProto.PbftMessageList.newBuilder();
        for (PbftMessage pbftMessage : pbftMessageList) {
            protoPbftMessageListBuilder.addPbftMessageList(PbftMessage.toProto(pbftMessage));
        }
        return protoPbftMessageListBuilder.build();
    }

}
