package io.yggdrash.validator.data.ebft;

import com.google.protobuf.ByteString;
import io.yggdrash.common.util.ByteUtil;
import io.yggdrash.common.util.TimeUtils;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.proto.EbftProto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class EbftStatus {
    private EbftBlock lastConfirmedEbftBlock;
    private final List<EbftBlock> unConfirmedEbftBlockList = new ArrayList<>();
    private long timestamp;
    private byte[] signature;

    public EbftStatus(EbftBlock lastConfirmedEbftBlock,
                      List<EbftBlock> unConfirmedEbftBlockList,
                      long timestamp,
                      byte[] signature) {
        this.lastConfirmedEbftBlock = lastConfirmedEbftBlock;
        if (unConfirmedEbftBlockList != null) {
            this.unConfirmedEbftBlockList.addAll(unConfirmedEbftBlockList);
        }

        if (timestamp == 0L) {
            this.timestamp = TimeUtils.time();
        } else {
            this.timestamp = timestamp;
        }

        this.signature = signature;
    }

    public EbftStatus(EbftBlock lastConfirmedEbftBlock,
                      List<EbftBlock> unConfirmedEbftBlockList) {
        this(lastConfirmedEbftBlock, unConfirmedEbftBlockList, TimeUtils.time(), null);
    }

    public EbftStatus(EbftProto.EbftStatus nodeStatus) {
        this.lastConfirmedEbftBlock = new EbftBlock(nodeStatus.getLastConfirmedEbftBlock());
        for (EbftProto.EbftBlock block :
                nodeStatus.getUnConfirmedEbftBlockList().getEbftBlockListList()) {
            this.unConfirmedEbftBlockList.add(new EbftBlock(block));
        }
        this.timestamp = nodeStatus.getTimestamp();
        this.signature = nodeStatus.getSignature().toByteArray();
    }

    public EbftBlock getLastConfirmedEbftBlock() {
        return lastConfirmedEbftBlock;
    }

    public List<EbftBlock> getUnConfirmedEbftBlockList() {
        return unConfirmedEbftBlockList;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public byte[] getSignature() {
        return signature;
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }

    public byte[] getDataForSignning() {
        ByteArrayOutputStream dataForSignning = new ByteArrayOutputStream();

        try {
            for (EbftBlock ebftBlock : unConfirmedEbftBlockList) {
                dataForSignning.write(ebftBlock.getHash());
            }

            dataForSignning.write(lastConfirmedEbftBlock.getHash());
            dataForSignning.write(ByteUtil.longToBytes(timestamp));
        } catch (IOException e) {
            return null;
        }

        return dataForSignning.toByteArray();
    }

    public static boolean verify(EbftStatus ebftStatus) {
        if (ebftStatus != null) {
            return Wallet.verify(
                    ebftStatus.getDataForSignning(), ebftStatus.getSignature(), false);
        }

        return false;
    }

    public static EbftProto.EbftStatus toProto(EbftStatus ebftStatus) {
        EbftProto.EbftStatus.Builder protoBlockStatus = EbftProto.EbftStatus.newBuilder()
                .setLastConfirmedEbftBlock(
                        EbftBlock.toProto(ebftStatus.getLastConfirmedEbftBlock()))
                .setUnConfirmedEbftBlockList(
                        EbftBlock.toProtoList(ebftStatus.getUnConfirmedEbftBlockList()))
                .setTimestamp(ebftStatus.getTimestamp())
                .setSignature(ByteString.copyFrom(ebftStatus.getSignature()));
        return protoBlockStatus.build();
    }

}