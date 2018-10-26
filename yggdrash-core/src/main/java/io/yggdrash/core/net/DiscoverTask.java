package io.yggdrash.core.net;

import io.yggdrash.core.BranchId;
import io.yggdrash.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DiscoverTask implements Runnable {
    private static final Logger log = LoggerFactory.getLogger("DiscoverTask");

    private DiscoveryClient discoveryClient;
    private PeerGroup peerGroup;
    private final Peer owner;
    private final byte[] ownerId;

    DiscoverTask(PeerGroup peerGroup, DiscoveryClient discoveryClient) {
        this.peerGroup = peerGroup;
        this.owner = peerGroup.getOwner();
        this.ownerId = owner.getPeerId().getBytes();
        this.discoveryClient = discoveryClient;
    }

    @Override
    public void run() {
        discover(0, new ArrayList<>());
    }

    private synchronized void discover(int round, List<Peer> prevTried) {
        log.info("Start discover!");
        try {
            if (round == KademliaOptions.MAX_STEPS) {
                log.debug("Peer table contains [{}] peers", peerGroup.count(BranchId.stem()));
                log.debug("{}", String.format("(KademliaOptions.MAX_STEPS) Terminating discover"
                        + "after %d rounds.", round));
                log.trace("{}\n{}",
                        String.format("Peers discovered %d", peerGroup.count(BranchId.stem())),
                        peerGroup.getPeerUriList(BranchId.stem()));
                return;
            }

            Optional<PeerTable> peerTable
                    = Optional.ofNullable(peerGroup.getPeerTable(BranchId.stem()));
            List<Peer> closest = peerTable
                    .map(pt -> pt.getClosestPeers(ownerId)).orElse(new ArrayList<>());
            List<Peer> tried = new ArrayList<>();

            for (Peer p : closest) {
                if (!tried.contains(p) && !prevTried.contains(p)) {
                    try {
                        Optional<List<String>> list = Optional.ofNullable(
                                discoveryClient.findPeers(p.getHost(), p.getPort(), owner));
                        list.ifPresent(strings -> strings.forEach(
                                u -> peerGroup.addPeerByYnodeUri(BranchId.stem(), u)));

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
                log.debug("{}", String.format(
                        "(tried.isEmpty()) Terminating discover after %d rounds.", round));
                log.trace("{}\n{}",
                        String.format("Peers discovered %d", peerGroup.count(BranchId.stem())),
                        peerGroup.getPeerUriList(BranchId.stem()));
                return;
            }

            tried.addAll(prevTried);
            discover(round + 1, tried);
        } catch (Exception e) {
            log.info("{}", e);
        }
    }
}
