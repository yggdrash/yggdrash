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
    private byte[] chain;
    private List<String> activeNodeList;
    private EbftBlock lastConfirmedEbftBlock;
    private final List<EbftBlock> unConfirmedEbftBlockList = new ArrayList<>();
    private long timestamp;
    private byte[] signature;

    public EbftStatus(List<String> activeNodeList,
                      EbftBlock lastConfirmedEbftBlock,
                      List<EbftBlock> unConfirmedEbftBlockList,
                      long timestamp,
                      byte[] signature) {
        this.chain = lastConfirmedEbftBlock.getChain();
        this.activeNodeList = activeNodeList;
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

    public EbftStatus(List<String> activeNodeList,
                      EbftBlock lastConfirmedEbftBlock,
                      List<EbftBlock> unConfirmedEbftBlockList) {
        this (activeNodeList,
                lastConfirmedEbftBlock, unConfirmedEbftBlockList, TimeUtils.time(), null);
    }

    public EbftStatus(EbftProto.EbftStatus nodeStatus) {
        this.chain = nodeStatus.getChain().toByteArray();
        this.activeNodeList = nodeStatus.getActiveNodeList().getNodeListList();
        this.lastConfirmedEbftBlock = new EbftBlock(nodeStatus.getLastConfirmedEbftBlock());
        for (EbftProto.EbftBlock block :
                nodeStatus.getUnConfirmedEbftBlockList().getEbftBlockListList()) {
            this.unConfirmedEbftBlockList.add(new EbftBlock(block));
        }
        this.timestamp = nodeStatus.getTimestamp();
        this.signature = nodeStatus.getSignature().toByteArray();
    }

    public byte[] getChain() {
        return chain;
    }

    public List<String> getActiveNodeList() {
        return activeNodeList;
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
            for (String node : this.activeNodeList) {
                dataForSignning.write(node.getBytes());
            }

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
                .setChain(ByteString.copyFrom(ebftStatus.getChain()))
                .setActiveNodeList(EbftProto.NodeList.newBuilder()
                        .addAllNodeList(ebftStatus.getActiveNodeList()).build())
                .setLastConfirmedEbftBlock(
                        EbftBlock.toProto(ebftStatus.getLastConfirmedEbftBlock()))
                .setUnConfirmedEbftBlockList(
                        EbftBlock.toProtoList(ebftStatus.getUnConfirmedEbftBlockList()))
                .setTimestamp(ebftStatus.getTimestamp())
                .setSignature(ByteString.copyFrom(ebftStatus.getSignature()));
        return protoBlockStatus.build();
    }

}