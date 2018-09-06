package io.yggdrash.core.net;

import io.yggdrash.core.BlockHusk;
import io.yggdrash.core.TransactionHusk;
import io.yggdrash.core.event.BranchEventListener;
import io.yggdrash.core.event.PeerEventListener;
import io.yggdrash.proto.Pong;
import io.yggdrash.proto.Proto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PeerChannelGroup implements BranchEventListener {

    private static final Logger log = LoggerFactory.getLogger(PeerChannelGroup.class);

    private final Map<String, PeerClientChannel> peerChannel = new ConcurrentHashMap<>();

    private final int maxPeers;

    private PeerEventListener listener;

    public PeerChannelGroup(int maxPeers) {
        this.maxPeers = maxPeers;
    }

    public void setListener(PeerEventListener listener) {
        this.listener = listener;
    }

    public void destroy(String ynodeUri) {
        peerChannel.values().forEach(client -> client.stop(ynodeUri));
    }

    public void healthCheck() {
        if (peerChannel.isEmpty()) {
            log.trace("Active peer is empty to health check peer");
            return;
        }
        List<PeerClientChannel> peerChannelList = new ArrayList<>(peerChannel.values());
        for (PeerClientChannel client : peerChannelList) {
            try {
                Pong pong = client.ping("Ping");
                if (pong.getPong().equals("Pong")) {
                    continue;
                }
            } catch (Exception e) {
                log.warn("Health check fail. peer=" + client.getPeer().getYnodeUri());
            }
            peerChannel.remove(client.getPeer().getYnodeUri());
            client.stop();
            listener.disconnected(client.getPeer());
        }
    }

    public void newTransaction(TransactionHusk tx) {
        if (peerChannel.isEmpty()) {
            log.warn("Active peer is empty to broadcast transaction");
        }
        Proto.Transaction[] txns = new Proto.Transaction[] {tx.getInstance()};

        for (PeerClientChannel client : peerChannel.values()) {
            client.broadcastTransaction(txns);
        }
    }

    @Override
    public void chainedBlock(BlockHusk block) {
        if (peerChannel.isEmpty()) {
            log.trace("Active peer is empty to broadcast block");
        }
        Proto.Block[] blocks
                = new Proto.Block[] {block.getInstance()};
        for (PeerClientChannel client : peerChannel.values()) {
            client.broadcastBlock(blocks);
        }
    }

    public void newPeerChannel(PeerClientChannel client) {
        Peer peer = client.getPeer();
        if (peerChannel.containsKey(peer.getYnodeUri())) {
            return;
        } else if (peerChannel.size() >= maxPeers) {
            log.info("Ignore to add active peer channel. count={}, peer={}", peerChannel.size(),
                    peer.getYnodeUri());
            return;
        }
        try {
            log.info("Connecting... peer {}:{}", peer.getHost(), peer.getPort());
            Pong pong = client.ping("Ping");
            // TODO validation peer
            if (pong.getPong().equals("Pong")) {
                peerChannel.put(peer.getYnodeUri(), client);
            }
        } catch (Exception e) {
            log.warn("Fail to add to the peer channel err=" + e.getMessage());
        }
    }

    public List<String> getActivePeerList() {
        return new ArrayList<>(peerChannel.keySet());
    }

    /**
     * Broadcast peer uri
     *
     * @param ynodeUri the peer uri to broadcast
     * @return the block list
     */
    public List<String> broadcastPeerConnect(String ynodeUri) {
        if (peerChannel.isEmpty()) {
            log.warn("Active peer is empty to broadcast peer");
            return Collections.emptyList();
        }
        List<String> peerList = new ArrayList<>();
        for (PeerClientChannel client : peerChannel.values()) {
            peerList.addAll(client.requestPeerList(ynodeUri, 0));
        }
        return peerList;
    }

    public void broadcastPeerDisconnect(String ynodeUri) {
        PeerClientChannel disconnectedPeer = peerChannel.remove(ynodeUri);
        if (disconnectedPeer != null) {
            disconnectedPeer.stop();
        }
        for (PeerClientChannel client : peerChannel.values()) {
            client.disconnectPeer(ynodeUri);
        }
    }

    /**
     * Sync block list.
     *
     * @param offset the offset
     * @return the block list
     */
    public List<BlockHusk> syncBlock(long offset) {
        if (peerChannel.isEmpty()) {
            log.warn("Active peer is empty to sync block");
            return Collections.emptyList();
        }
        // TODO sync peer selection policy
        String key = (String) peerChannel.keySet().toArray()[0];
        PeerClientChannel client = peerChannel.get(key);
        List<Proto.Block> blockList = client.syncBlock(offset);
        log.debug("Synchronize block received=" + blockList.size());
        List<BlockHusk> syncList = new ArrayList<>(blockList.size());
        for (Proto.Block block : blockList) {
            syncList.add(new BlockHusk(block));
        }
        return syncList;
    }

    /**
     * Sync transaction list.
     *
     * @return the transaction list
     */
    public List<TransactionHusk> syncTransaction() {
        if (peerChannel.isEmpty()) {
            log.warn("Active peer is empty to sync transaction");
            return Collections.emptyList();
        }
        // TODO sync peer selection policy
        String key = (String) peerChannel.keySet().toArray()[0];
        PeerClientChannel client = peerChannel.get(key);
        List<Proto.Transaction> txList = client.syncTransaction();
        log.debug("Synchronize transaction received=" + txList.size());
        List<TransactionHusk> syncList = new ArrayList<>(txList.size());
        for (Proto.Transaction tx : txList) {
            syncList.add(new TransactionHusk(tx));
        }
        return syncList;
    }

}
