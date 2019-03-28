package io.yggdrash.validator.data.pbft;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import io.yggdrash.common.crypto.HashUtil;
import io.yggdrash.common.utils.ByteUtil;
import io.yggdrash.common.utils.JsonUtil;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.proto.PbftProto;
import org.spongycastle.util.encoders.Hex;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PbftMessage {
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
                       byte[] hash,
                       byte[] result,
                       byte[] signature,
                       Block block) {
        this.type = type;
        this.viewNumber = viewNumber;
        this.seqNumber = seqNumber;
        this.hash = hash;
        this.result = result;
        this.signature = signature;
        this.block = block; // todo: whether check PrePrepare message
    }

    public PbftMessage(String type,
                       long viewNumber,
                       long seqNumber,
                       byte[] hash,
                       byte[] result,
                       Wallet wallet,
                       Block block) {
        this.type = type;
        this.viewNumber = viewNumber;
        this.seqNumber = seqNumber;
        this.hash = hash;
        this.result = result;
        this.signature = this.sign(wallet);
        this.block = block; // todo: whether check PrePrepare message
    }

    public PbftMessage(byte[] bytes) {
        this(JsonUtil.parseJsonObject(new String(bytes, StandardCharsets.UTF_8)));
    }

    public PbftMessage(PbftProto.PbftMessage protoPbftMessage) {
        this(protoPbftMessage.getType(),
                protoPbftMessage.getViewNumber(),
                protoPbftMessage.getSeqNumber(),
                protoPbftMessage.getHash().toByteArray(),
                protoPbftMessage.getResult().toByteArray().length == 0
                        ? null : protoPbftMessage.getResult().toByteArray(),
                protoPbftMessage.getSignature().toByteArray(),
                Block.toBlock(protoPbftMessage.getBlock()));
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
            this.block = new Block(blockJsonElement.getAsJsonObject());
        } else {
            this.block = null;
        }
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

    public String getHashHex() {
        return Hex.toHexString(hash);
    }

    public byte[] getResult() {
        return result;
    }

    public byte[] getSignature() {
        return signature;
    }

    public String getSignatureHex() {
        if (signature == null) {
            return null;
        }
        return Hex.toHexString(signature);
    }

    public Block getBlock() {
        return block;
    }

    public byte[] toBinary() {
        return this.toJsonObject().toString().getBytes(StandardCharsets.UTF_8);
    }

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

    public byte[] sign(Wallet wallet) {
        if (wallet == null) {
            throw new NotValidateException("wallet is null");
        }

        return wallet.sign(getHashForSigning(), true);
    }

    public static boolean verify(PbftMessage pbftMessage) {
        if (pbftMessage == null
                || pbftMessage.getSignature() == null
                || pbftMessage.getSignature().length == 0) {
            return false;
        }

        // todo: check validator

        if (!Wallet.verify(pbftMessage.getHashForSigning(), pbftMessage.getSignature(), true)) {
            return false;
        }

        if (pbftMessage.type.equals("PREPREPA")) {
            if (pbftMessage.getBlock() == null) {
                return false;
            }

            return Arrays.equals(pbftMessage.getHash(), pbftMessage.getBlock().getHash())
                    && pbftMessage.getBlock().verify();
        }

        return true;
    }

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
            protoPbftMessageBuilder.setBlock(Block.toProtoBlock(pbftMessage.getBlock()));
        }
        return protoPbftMessageBuilder.build();
    }

    public static PbftProto.PbftMessageList toProtoList(
            List<PbftMessage> pbftMessageList) {
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

    public boolean equals(PbftMessage newPbftMessage) {
        if (newPbftMessage == null) {
            return false;
        }
        return Arrays.equals(this.toBinary(), newPbftMessage.toBinary());
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
