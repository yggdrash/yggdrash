package io.yggdrash.validator.data.pbft;

import com.google.gson.JsonObject;
import io.yggdrash.common.util.JsonUtil;
import io.yggdrash.proto.PbftProto;
import org.spongycastle.util.encoders.Hex;

import java.nio.charset.StandardCharsets;

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
        this.hash = protoPbftMessage.getBlockHash().toByteArray();
        this.result = protoPbftMessage.getResult().toByteArray();
        this.signature = protoPbftMessage.getSignature().toByteArray();
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
}
