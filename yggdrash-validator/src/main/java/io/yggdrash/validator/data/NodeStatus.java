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
    private BlockCon lastConfirmedBlockCon;
    private final List<BlockCon> unConfirmedBlockConList = new ArrayList<>();
    private long timestamp;
    private byte[] signature;

    public NodeStatus(List<String> activeNodeList,
                      BlockCon lastConfirmedBlockCon,
                      List<BlockCon> unConfirmedBlockConList,
                      long timestamp,
                      byte[] signature) {
        this.chain = lastConfirmedBlockCon.getChain();
        this.activeNodeList = activeNodeList;
        this.lastConfirmedBlockCon = lastConfirmedBlockCon;
        if (unConfirmedBlockConList != null) {
            this.unConfirmedBlockConList.addAll(unConfirmedBlockConList);
        }

        if (timestamp == 0L) {
            this.timestamp = TimeUtils.time();
        } else {
            this.timestamp = timestamp;
        }

        this.signature = signature;
    }

    public NodeStatus(List<String> activeNodeList,
                      BlockCon lastConfirmedBlockCon,
                      List<BlockCon> unConfirmedBlockConList) {
        this (activeNodeList,
                lastConfirmedBlockCon, unConfirmedBlockConList, TimeUtils.time(), null);
    }

    public NodeStatus(EbftProto.NodeStatus nodeStatus) {
        this.chain = nodeStatus.getChain().toByteArray();
        this.activeNodeList = nodeStatus.getActiveNodeList().getNodeListList();
        this.lastConfirmedBlockCon = new BlockCon(nodeStatus.getLastConfirmedBlockCon());
        for (EbftProto.BlockCon blockCon :
                nodeStatus.getUnConfirmedBlockConList().getBlockConListList()) {
            this.unConfirmedBlockConList.add(new BlockCon(blockCon));
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

    public BlockCon getLastConfirmedBlockCon() {
        return lastConfirmedBlockCon;
    }

    public List<BlockCon> getUnConfirmedBlockConList() {
        return unConfirmedBlockConList;
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

            for (BlockCon blockCon : unConfirmedBlockConList) {
                dataForSignning.write(blockCon.getHash());
            }

            dataForSignning.write(lastConfirmedBlockCon.getHash());
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
                .setLastConfirmedBlockCon(
                        BlockCon.toProto(nodeStatus.getLastConfirmedBlockCon()))
                .setUnConfirmedBlockConList(
                        BlockCon.toProtoList(nodeStatus.getUnConfirmedBlockConList()))
                .setTimestamp(nodeStatus.getTimestamp())
                .setSignature(ByteString.copyFrom(nodeStatus.getSignature()));
        return protoBlockStatus.build();
    }

}