package io.yggdrash.validator.data.pbft;

import com.google.protobuf.ByteString;
import io.yggdrash.common.crypto.HashUtil;
import io.yggdrash.common.util.ByteUtil;
import io.yggdrash.common.util.TimeUtils;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.proto.PbftProto;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.TreeMap;

public class PbftStatus {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(PbftStatus.class);

    private long index;
    private final Map<String, PbftMessage> unConfirmedPbftMessageMap = new TreeMap<>();
    private long timestamp;
    private byte[] signature;

    public PbftStatus(long index,
                      Map<String, PbftMessage> unConfirmedPbftMessageMap,
                      long timestamp,
                      byte[] signature) {
        this.index = index;
        this.unConfirmedPbftMessageMap.putAll(unConfirmedPbftMessageMap);
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
            if (!this.unConfirmedPbftMessageMap.containsKey(key)) {
                this.unConfirmedPbftMessageMap.put(key, new PbftMessage(protoPbftMessage));
            }
        }
        this.timestamp = pbftStatus.getTimestamp();
        this.signature = pbftStatus.getSignature().toByteArray();
    }

    public long getIndex() {
        return index;
    }

    public Map<String, PbftMessage> getUnConfirmedPbftMessageMap() {
        return unConfirmedPbftMessageMap;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public byte[] getSignature() {
        return signature;
    }

    public byte[] getHashForSigning() {
        ByteArrayOutputStream dataForSigning = new ByteArrayOutputStream();

        try {
            dataForSigning.write(ByteUtil.longToBytes(index));
            for (String key : this.unConfirmedPbftMessageMap.keySet()) {
                PbftMessage pbftMessage = this.unConfirmedPbftMessageMap.get(key);
                dataForSigning.write(pbftMessage.toBinary());
            }
            dataForSigning.write(ByteUtil.longToBytes(timestamp));
        } catch (Exception e) {
            log.debug(e.getMessage());
            return null;
        }

        return HashUtil.sha3(dataForSigning.toByteArray());
    }

    public static boolean verify(PbftStatus status) {
        if (status != null && status.getSignature() != null) {
            byte[] hashData = status.getHashForSigning();
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
        for (String key : pbftStatus.getUnConfirmedPbftMessageMap().keySet()) {
            PbftMessage pbftMessage = pbftStatus.getUnConfirmedPbftMessageMap().get(key);
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