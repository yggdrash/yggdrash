package io.yggdrash.core.net;

import io.yggdrash.common.util.Utils;
import io.yggdrash.core.BranchId;
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
        discover(0, new ArrayList<>());
    }

    public abstract PeerClientChannel getClient(Peer peer);

    private synchronized void discover(int round, List<Peer> prevTried) {
        log.info("Start discover!");
        log.info("Size of STEM PeerTable => {}",
                peerGroup.getPeerTable(branchId).getPeersCount() - 1);
        try {
            if (round == KademliaOptions.MAX_STEPS) {
                log.debug("Peer table contains [{}] peers", peerGroup.count(branchId));
                log.debug("{}", String.format("(KademliaOptions.MAX_STEPS) Terminating discover"
                        + "after %d rounds.", round));
                log.trace("{}\n{}",
                        String.format("Peers discovered %d", peerGroup.count(branchId)),
                        peerGroup.getPeerUriList(branchId));
                return;
            }

            Optional<PeerTable> peerTable
                    = Optional.ofNullable(peerGroup.getPeerTable(branchId));
            List<Peer> closest = peerTable
                    .map(pt -> pt.getClosestPeers(ownerId)).orElse(new ArrayList<>());
            List<Peer> tried = new ArrayList<>();

            for (Peer p : closest) {
                if (!tried.contains(p) && !prevTried.contains(p)) {
                    try {
                        Optional<List<NodeInfo>> list = Optional.ofNullable(
                                getClient(p).findPeers(branchId, owner));
                        list.ifPresent(nodeInfo -> nodeInfo.forEach(
                                n -> peerGroup.addPeerByYnodeUri(branchId, n.getUrl())));

                        tried.add(p);
                        Utils.sleep(50);
                    } catch (Exception e) {
                        //log.error("Unexpected Exception " + e, e);
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

                if (round == 0) {
                    // SeedPeer 로부터 빈 리스트([])를 받았을 때 SeedPeer 를 테이블과 채널에 추가해야한다.
                    Peer seedPeer = Peer.valueOf(peerGroup.getSeedPeerList().get(0));
                    peerGroup.addPeerByYnodeUri(branchId, seedPeer.getYnodeUri());
                    peerGroup.newPeerChannel(branchId, getClient(seedPeer));
                }
                return;
            }

            tried.addAll(prevTried);
            discover(round + 1, tried);
        } catch (Exception e) {
            log.info("{}", e);
        }
    }
}
