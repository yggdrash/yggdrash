package io.yggdrash.core.net;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class KademliaDiscovery implements Discovery {
    private static final Logger log = LoggerFactory.getLogger(KademliaDiscovery.class);

    private PeerTable peerTable;
    private PeerHandlerFactory factory;
    private Peer owner;

    public KademliaDiscovery(PeerTable peerTable) {
        this.peerTable = peerTable;
        this.owner = peerTable.getOwner();
    }

    @Override
    public PeerTable discover(PeerHandlerFactory factory) {
        this.factory = factory;

        List<Peer> prevTried = new ArrayList<>();
        for (Peer peer : peerTable.getBootstrappingSeedList()) {
            String ynodeUriWithoutPubKey = peerTable.getOwner().getYnodeUri()
                    .substring(peer.getYnodeUri().indexOf("@"));
            if (peer.getYnodeUri().contains(ynodeUriWithoutPubKey)) {
                continue;
            }
            prevTried.add(peer);
            PeerHandler peerHandler = null;
            try {
                log.info("Try connecting to SEED peer = {}", peer);
                peerHandler = factory.create(peer);
                List<Peer> peerList = peerHandler.findPeers(owner);
                peerList.forEach(peerTable::addPeer);
            } catch (Exception e) {
                log.error("Failed connecting to SEED peer = {}", peer);
            } finally {
                if (peerHandler != null) {
                    peerHandler.stop();
                }
            }
        }

        int peerCount = peerTable.count();
        log.info("Start discover! peerCount={}", peerCount - 1);
        findPeers(0, prevTried);
        return peerTable;
    }

    private synchronized void findPeers(int round, List<Peer> prevTried) {
        try {
            if (round == KademliaOptions.MAX_STEPS) {
                log.debug("{}", String.format("(KademliaOptions.MAX_STEPS) Terminating discover"
                        + "after %d rounds.", round));
                log.trace("{}\n{}",
                        String.format("Peers discovered %d", peerTable.count()),
                        peerTable.getPeerUriList());
                return;
            }

            List<Peer> closest = peerTable.getClosestPeers(KademliaOptions.BUCKET_SIZE);
            List<Peer> tried = new ArrayList<>();

            for (Peer peer : closest) {
                if (!tried.contains(peer) && !prevTried.contains(peer)) {
                    PeerHandler peerHandler = factory.create(peer);
                    try {
                        List<Peer> peerList = peerHandler.findPeers(owner);
                        peerList.forEach(peerTable::addPeer);
                        tried.add(peer);
                    } catch (Exception e) {
                        log.warn(e.getMessage());
                    } finally {
                        peerHandler.stop();
                    }
                }
                if (tried.size() == KademliaOptions.ALPHA) {
                    break;
                }
            }

            if (tried.isEmpty()) {
                log.debug("Terminating discover after {} rounds.", round);
                log.trace("{}\n{}",
                        String.format("Peers discovered %d", peerTable.count()),
                        peerTable.getPeerUriList());
                return;
            }

            tried.addAll(prevTried);
            findPeers(round + 1, tried);
        } catch (Exception e) {
            log.info("{}", e);
        }
    }
}
