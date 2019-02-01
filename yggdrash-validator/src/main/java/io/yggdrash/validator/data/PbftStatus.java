package io.yggdrash.validator.data;

import com.google.protobuf.ByteString;
import io.yggdrash.common.crypto.HashUtil;
import io.yggdrash.common.util.ByteUtil;
import io.yggdrash.common.util.TimeUtils;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.proto.PbftProto;
import io.yggdrash.validator.data.pbft.PbftMessage;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.TreeMap;

public class PbftStatus {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(PbftStatus.class);

    private long index;
    private final Map<String, PbftMessage> pbftMessageMap = new TreeMap<>();
    private long timestamp;
    private byte[] signature;

    public PbftStatus(long index,
                      Map<String, PbftMessage> pbftMessageMap,
                      long timestamp,
                      byte[] signature) {
        this.index = index;
        this.pbftMessageMap.putAll(pbftMessageMap);
        if (timestamp == 0L) {
            this.timestamp = TimeUtils.time();
        } else {
            this.timestamp = timestamp;
        }

        this.signature = signature;
    }

    public PbftStatus(PbftProto.PbftStatus pbftStatus) {
        this.index = pbftStatus.getIndex();
        for (PbftProto.PbftMessage protoPbftMessage :
                pbftStatus.getPbftMessageList().getPbftMessageListList()) {
            String key = Hex.toHexString(protoPbftMessage.getSignature().toByteArray());
            if (this.pbftMessageMap.containsKey(key)) {
                // todo: update pbftmessge
            } else {
                this.pbftMessageMap.put(key, new PbftMessage(protoPbftMessage));
            }
        }
        this.timestamp = pbftStatus.getTimestamp();
        this.signature = pbftStatus.getSignature().toByteArray();
    }

    public long getIndex() {
        return index;
    }

    public Map<String, PbftMessage> getPbftMessageMap() {
        return pbftMessageMap;
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
            dataForSignning.write(ByteUtil.longToBytes(index));
            for (String key : this.pbftMessageMap.keySet()) {
                PbftMessage pbftMessage = this.pbftMessageMap.get(key);
                dataForSignning.write(pbftMessage.toBinary());
            }
            dataForSignning.write(ByteUtil.longToBytes(timestamp));
        } catch (Exception e) {
            log.debug(e.getMessage());
            return null;
        }

        return HashUtil.sha3(dataForSignning.toByteArray());
    }

    public static boolean verify(PbftStatus status) {
        if (status != null && status.getSignature() != null) {
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
        if (pbftStatus == null) {
            return null;
        }

        PbftProto.PbftMessageList.Builder protoPbftMessageListBuilder = PbftProto.PbftMessageList.newBuilder();
        for (String key : pbftStatus.getPbftMessageMap().keySet()) {
            PbftMessage pbftMessage = pbftStatus.getPbftMessageMap().get(key);
            protoPbftMessageListBuilder.addPbftMessageList(PbftMessage.toProto(pbftMessage));
        }

        PbftProto.PbftStatus.Builder protoPbftStatusBuilder = PbftProto.PbftStatus.newBuilder()
                .setIndex(pbftStatus.getIndex())
                .setPbftMessageList(protoPbftMessageListBuilder.build())
                .setTimestamp(pbftStatus.getTimestamp())
                .setSignature(ByteString.copyFrom(pbftStatus.getSignature()));
        return protoPbftStatusBuilder.build();
    }

}