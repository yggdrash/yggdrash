package io.yggdrash.validator.data;

import com.google.protobuf.ByteString;
import io.yggdrash.common.crypto.HashUtil;
import io.yggdrash.common.util.ByteUtil;
import io.yggdrash.common.util.TimeUtils;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.proto.PbftProto;

import java.io.ByteArrayOutputStream;

public class PbftStatus {
    private PbftBlock lastConfirmedBlock;
    private PbftBlock unConfirmedBlock;
    private long timestamp;
    private byte[] signature;

    public PbftStatus(PbftBlock lastConfirmedBlock,
                      PbftBlock unConfirmedBlock,
                      long timestamp,
                      byte[] signature) {
        this.lastConfirmedBlock = lastConfirmedBlock;
        this.unConfirmedBlock = unConfirmedBlock;

        if (timestamp == 0L) {
            this.timestamp = TimeUtils.time();
        } else {
            this.timestamp = timestamp;
        }

        this.signature = signature;
    }

    public PbftStatus(PbftProto.PbftStatus pbftStatus) {
        this.lastConfirmedBlock = new PbftBlock(pbftStatus.getLastConfirmedBlock());
        this.unConfirmedBlock = new PbftBlock(pbftStatus.getUnConfirmedBlock());
        this.timestamp = pbftStatus.getTimestamp();
        this.signature = pbftStatus.getSignature().toByteArray();
    }

    public PbftBlock getLastConfirmedBlock() {
        return lastConfirmedBlock;
    }

    public PbftBlock getUnConfirmedBlock() {
        return unConfirmedBlock;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public byte[] getSignature() {
        return signature;
    }

    public byte[] getHashForSignning() {
        ByteArrayOutputStream dataForSignning = new ByteArrayOutputStream();

        try {
            if (lastConfirmedBlock != null && lastConfirmedBlock.getBlock() != null) {
                dataForSignning.write(lastConfirmedBlock.getHash());
            }
            if (unConfirmedBlock != null && unConfirmedBlock.getBlock() != null) {
                dataForSignning.write(unConfirmedBlock.getHash());
            }
            dataForSignning.write(ByteUtil.longToBytes(timestamp));
        } catch (Exception e) {
            return null;
        }

        return HashUtil.sha3(dataForSignning.toByteArray());
    }

    public static boolean verify(PbftStatus status) {
        if (status != null) {
            byte[] hashData = status.getHashForSignning();
            byte[] signature = status.getSignature();
            if (hashData == null || signature == null) {
                return false;
            }
            return Wallet.verify(hashData, signature, true);
        }

        return false;
    }

    public static PbftProto.PbftStatus toProto(PbftStatus pbftStatus) {
        PbftProto.PbftStatus.Builder protoPbftStatusBuilder = PbftProto.PbftStatus.newBuilder();

        PbftBlock lastConfirmedBlock = pbftStatus.getLastConfirmedBlock();
        if (lastConfirmedBlock != null) {
            protoPbftStatusBuilder.setLastConfirmedBlock(PbftBlock.toProto(lastConfirmedBlock));
        }

        PbftBlock unConfirmedBlock = pbftStatus.getUnConfirmedBlock();
        if (unConfirmedBlock != null) {
            protoPbftStatusBuilder.setUnConfirmedBlock(PbftBlock.toProto(unConfirmedBlock));
        }

        protoPbftStatusBuilder
                .setTimestamp(pbftStatus.getTimestamp())
                .setSignature(ByteString.copyFrom(pbftStatus.getSignature()));
        return protoPbftStatusBuilder.build();
    }

}