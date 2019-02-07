package io.yggdrash.validator.data;

import com.google.protobuf.ByteString;
import io.yggdrash.common.util.ByteUtil;
import io.yggdrash.common.util.TimeUtils;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.proto.EbftProto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NodeStatus {
    private byte[] chain;
    private List<String> activeNodeList;
    private EbftBlock lastConfirmedEbftBlock;
    private final List<EbftBlock> unConfirmedEbftBlockList = new ArrayList<>();
    private long timestamp;
    private byte[] signature;

    public NodeStatus(List<String> activeNodeList,
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

    public NodeStatus(List<String> activeNodeList,
                      EbftBlock lastConfirmedEbftBlock,
                      List<EbftBlock> unConfirmedEbftBlockList) {
        this (activeNodeList,
                lastConfirmedEbftBlock, unConfirmedEbftBlockList, TimeUtils.time(), null);
    }

    public NodeStatus(EbftProto.NodeStatus nodeStatus) {
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

    public static boolean verify(NodeStatus nodeStatus) {
        if (nodeStatus != null) {
            return Wallet.verify(
                    nodeStatus.getDataForSignning(), nodeStatus.getSignature(), false);
        }

        return false;
    }

    public static EbftProto.NodeStatus toProto(NodeStatus nodeStatus) {
        EbftProto.NodeStatus.Builder protoBlockStatus = EbftProto.NodeStatus.newBuilder()
                .setChain(ByteString.copyFrom(nodeStatus.getChain()))
                .setActiveNodeList(EbftProto.NodeList.newBuilder()
                        .addAllNodeList(nodeStatus.getActiveNodeList()).build())
                .setLastConfirmedEbftBlock(
                        EbftBlock.toProto(nodeStatus.getLastConfirmedEbftBlock()))
                .setUnConfirmedEbftBlockList(
                        EbftBlock.toProtoList(nodeStatus.getUnConfirmedEbftBlockList()))
                .setTimestamp(nodeStatus.getTimestamp())
                .setSignature(ByteString.copyFrom(nodeStatus.getSignature()));
        return protoBlockStatus.build();
    }

}