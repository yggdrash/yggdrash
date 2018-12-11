package io.yggdrash.core.net;

import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.proto.NodeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class DiscoverTask implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(DiscoverTask.class);

    private final PeerGroup peerGroup;
    private final Peer owner;
    private final byte[] ownerId;
    private final BranchId branchId;

    public DiscoverTask(PeerGroup peerGroup, BranchId branchId) {
        this.peerGroup = peerGroup;
        this.owner = peerGroup.getOwner();
        this.ownerId = owner.getPeerId().getBytes();
        this.branchId = branchId;
    }

    @Override
    public void run() {
        int peerCount = peerGroup.count(branchId) - 1;
        log.info("Start discover! peerCount={}, branchId={}", peerCount, branchId);
        discover(0, new ArrayList<>());
    }

    public abstract PeerClientChannel getClient(Peer peer);

    private synchronized void discover(int round, List<Peer> prevTried) {
        PeerTable peerTable = peerGroup.getPeerTable(branchId);

        try {
            if (round == KademliaOptions.MAX_STEPS) {
                log.debug("{}", String.format("(KademliaOptions.MAX_STEPS) Terminating discover"
                        + "after %d rounds.", round));
                log.trace("{}\n{}",
                        String.format("Peers discovered %d", peerTable.getPeersCount() - 1),
                        peerGroup.getPeerUriList(branchId));
                return;
            }

            List<Peer> closest = peerTable.getClosestPeers(ownerId);
            List<Peer> tried = new ArrayList<>();

            for (Peer p : closest) {
                if (!tried.contains(p) && !prevTried.contains(p)) {
                    PeerClientChannel clientChannel = getClient(p);
                    try {
                        Optional<List<NodeInfo>> list = Optional.ofNullable(
                                clientChannel.findPeers(branchId, owner));
                        list.ifPresent(nodeInfo -> nodeInfo.forEach(
                                n -> peerGroup.addPeerByYnodeUri(branchId, n.getUrl())));

                        tried.add(p);
                    } catch (Exception e) {
                        log.warn(e.getMessage());
                    } finally {
                        clientChannel.stop();
                    }
                }
                if (tried.size() == KademliaOptions.ALPHA) {
                    break;
                }
            }

            if (tried.isEmpty()) {
                log.debug("Terminating discover after {} rounds.", round);
                log.trace("{}\n{}",
                        String.format("Peers discovered %d", peerGroup.count(branchId)),
                        peerGroup.getPeerUriList(branchId));
                return;
            }

            tried.addAll(prevTried);
            discover(round + 1, tried);
        } catch (Exception e) {
            log.info("{}", e);
        }
    }
}
