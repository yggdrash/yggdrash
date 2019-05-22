package io.yggdrash.validator.data.pbft;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.crypto.HashUtil;
import io.yggdrash.common.utils.ByteUtil;
import io.yggdrash.common.utils.JsonUtil;
import io.yggdrash.common.utils.SerializationUtil;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.blockchain.BlockImpl;
import io.yggdrash.core.consensus.ConsensusMessage;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.proto.PbftProto;
import org.spongycastle.util.encoders.Hex;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class PbftMessage implements ConsensusMessage<PbftMessage> {
    private final String type;
    private final long viewNumber;
    private final long seqNumber;
    private final byte[] hash;
    private final byte[] result;
    private final byte[] signature;
    private final Block block;

    public PbftMessage(String type,
                       long viewNumber,
                       long seqNumber,
                       Sha3Hash hash,
                       byte[] result,
                       byte[] signature,
                       Block block) {
        this.type = type;
        this.viewNumber = viewNumber;
        this.seqNumber = seqNumber;
        this.hash = hash.getBytes();
        this.result = result;
        this.signature = signature;
        this.block = block; // todo: whether check PrePrepare message
    }

    public PbftMessage(String type,
                       long viewNumber,
                       long seqNumber,
                       Sha3Hash hash,
                       byte[] result,
                       Wallet wallet,
                       Block block) {
        this.type = type;
        this.viewNumber = viewNumber;
        this.seqNumber = seqNumber;
        this.hash = hash.getBytes();
        this.result = result;
        this.signature = this.sign(wallet);
        this.block = block; // todo: whether check PrePrepare message
    }

    public PbftMessage(byte[] bytes) {
        this(JsonUtil.parseJsonObject(SerializationUtil.deserializeString(bytes)));
    }

    public PbftMessage(PbftProto.PbftMessage protoPbftMessage) {
        this(protoPbftMessage.getType(),
                protoPbftMessage.getViewNumber(),
                protoPbftMessage.getSeqNumber(),
                Sha3Hash.createByHashed(protoPbftMessage.getHash().toByteArray()),
                protoPbftMessage.getResult().toByteArray().length == 0
                        ? null : protoPbftMessage.getResult().toByteArray(),
                protoPbftMessage.getSignature().toByteArray(),
                protoPbftMessage.getBlock().getSerializedSize() == 0 ? null :
                        new BlockImpl(protoPbftMessage.getBlock()));
    }

    public PbftMessage(JsonObject jsonObject) {
        this.type = jsonObject.get("type").getAsString();
        this.viewNumber = jsonObject.get("viewNumber").getAsLong();
        this.seqNumber = jsonObject.get("seqNumber").getAsLong();
        this.hash = Hex.decode(jsonObject.get("hash").getAsString());
        this.signature = Hex.decode(jsonObject.get("signature").getAsString());

        JsonElement resultJsonElement = jsonObject.get("result");
        if (resultJsonElement != null) {
            this.result = Hex.decode(resultJsonElement.getAsString());
        } else {
            this.result = null;
        }

        JsonElement blockJsonElement = jsonObject.get("block");
        if (blockJsonElement != null) {
            this.block = new BlockImpl(blockJsonElement.getAsJsonObject());
        } else {
            this.block = null;
        }
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public long getViewNumber() {
        return viewNumber;
    }

    @Override
    public long getSeqNumber() {
        return seqNumber;
    }

    @Override
    public byte[] getHash() {
        return hash;
    }

    @Override
    public String getHashHex() {
        return Hex.toHexString(hash);
    }

    @Override
    public byte[] getHashForSigning() {
        ByteArrayOutputStream bao = new ByteArrayOutputStream();

        try {
            bao.write(this.type.getBytes());
            bao.write(ByteUtil.longToBytes(this.viewNumber));
            bao.write(ByteUtil.longToBytes(this.seqNumber));
            bao.write(this.hash);
        } catch (IOException e) {
            throw new NotValidateException();
        }

        return HashUtil.sha3(bao.toByteArray());
    }

    @Override
    public byte[] getResult() {
        return result;
    }

    @Override
    public byte[] getSignature() {
        return signature;
    }

    @Override
    public String getSignatureHex() {
        if (signature == null) {
            return null;
        }
        return Hex.toHexString(signature);
    }

    @Override
    @JsonIgnore
    public Block getBlock() {
        return block;
    }

    @Override
    public byte[] toBinary() {
        return SerializationUtil.serializeJson(toJsonObject());
    }

    @Override
    public byte[] sign(Wallet wallet) {
        if (wallet == null) {
            throw new NotValidateException("wallet is null");
        }

        return wallet.sign(getHashForSigning(), true);
    }

    @Override
    public JsonObject toJsonObject() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", this.type);
        jsonObject.addProperty("viewNumber", this.viewNumber);
        jsonObject.addProperty("seqNumber", this.seqNumber);
        jsonObject.addProperty("hash", Hex.toHexString(this.hash));
        if (this.result != null) {
            jsonObject.addProperty("result", Hex.toHexString(this.result));
        }
        jsonObject.addProperty("signature", Hex.toHexString(this.signature));
        if (this.block != null) {
            jsonObject.add("block", this.block.toJsonObject());
        }
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
        if (pbftMessage == null) {
            return null;
        }

        PbftProto.PbftMessage.Builder protoPbftMessageBuilder = PbftProto.PbftMessage.newBuilder();
        protoPbftMessageBuilder.setType(pbftMessage.getType());
        protoPbftMessageBuilder.setViewNumber(pbftMessage.getViewNumber());
        protoPbftMessageBuilder.setSeqNumber(pbftMessage.getSeqNumber());
        if (pbftMessage.getHash() != null) {
            protoPbftMessageBuilder.setHash(ByteString.copyFrom(pbftMessage.getHash()));
        }
        if (pbftMessage.getResult() != null) {
            protoPbftMessageBuilder.setResult(ByteString.copyFrom(pbftMessage.getResult()));
        }
        if (pbftMessage.getSignature() != null) {
            protoPbftMessageBuilder.setSignature(ByteString.copyFrom(pbftMessage.getSignature()));
        }
        if (pbftMessage.getBlock() != null) {
            protoPbftMessageBuilder.setBlock(pbftMessage.getBlock().getProtoBlock());
        }
        return protoPbftMessageBuilder.build();
    }

    public static PbftProto.PbftMessageList toProtoList(Collection<PbftMessage> pbftMessageList) {
        if (pbftMessageList == null) {
            return null;
        }
        PbftProto.PbftMessageList.Builder protoPbftMessageListBuilder =
                PbftProto.PbftMessageList.newBuilder();
        for (PbftMessage pbftMessage : pbftMessageList) {
            protoPbftMessageListBuilder.addPbftMessageList(PbftMessage.toProto(pbftMessage));
        }
        return protoPbftMessageListBuilder.build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PbftMessage other = (PbftMessage) o;
        return Arrays.equals(toBinary(), other.toBinary());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(toBinary());
    }

    public void clear() {
        if (this.block != null) {
            this.block.clear();
        }
    }

    public PbftMessage clone() {
        return new PbftMessage(toJsonObject());
    }

}
