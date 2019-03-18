package io.yggdrash.validator.data.ebft;

import com.google.protobuf.ByteString;
import io.yggdrash.common.crypto.HashUtil;
import io.yggdrash.common.util.TimeUtils;
import io.yggdrash.common.utils.ByteUtil;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.proto.EbftProto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class EbftStatus {
    private long index;
    private final List<EbftBlock> unConfirmedEbftBlockList = new ArrayList<>();
    private long timestamp;
    private byte[] signature;

    public EbftStatus(long index,
                      List<EbftBlock> unConfirmedEbftBlockList,
                      long timestamp,
                      byte[] signature) {
        this.index = index;
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

    public EbftStatus(long index,
                      List<EbftBlock> unConfirmedEbftBlockList) {
        this(index, unConfirmedEbftBlockList, TimeUtils.time(), null);
    }

    public EbftStatus(EbftProto.EbftStatus nodeStatus) {
        this.index = nodeStatus.getIndex();
        for (EbftProto.EbftBlock block :
                nodeStatus.getUnConfirmedEbftBlockList().getEbftBlockListList()) {
            this.unConfirmedEbftBlockList.add(new EbftBlock(block));
        }
        this.timestamp = nodeStatus.getTimestamp();
        this.signature = nodeStatus.getSignature().toByteArray();
    }

    public long getIndex() {
        return index;
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

    public byte[] getHashForSigning() {
        ByteArrayOutputStream dataForSigning = new ByteArrayOutputStream();

        try {
            dataForSigning.write(ByteUtil.longToBytes(index));
            for (EbftBlock ebftBlock : unConfirmedEbftBlockList) {
                dataForSigning.write(ebftBlock.getHash());
            }
            dataForSigning.write(ByteUtil.longToBytes(timestamp));
        } catch (IOException e) {
            return null;
        }

        return HashUtil.sha3(dataForSigning.toByteArray());
    }

    public static boolean verify(EbftStatus ebftStatus) {
        if (ebftStatus != null) {
            return Wallet.verify(
                    ebftStatus.getHashForSigning(), ebftStatus.getSignature(), true);
        }
        return false;
    }

    public static EbftProto.EbftStatus toProto(EbftStatus ebftStatus) {
        EbftProto.EbftStatus.Builder protoBlockStatus = EbftProto.EbftStatus.newBuilder()
                .setIndex(ebftStatus.getIndex())
                .setUnConfirmedEbftBlockList(
                        EbftBlock.toProtoList(ebftStatus.getUnConfirmedEbftBlockList()))
                .setTimestamp(ebftStatus.getTimestamp())
                .setSignature(ByteString.copyFrom(ebftStatus.getSignature()));
        return protoBlockStatus.build();
    }

}